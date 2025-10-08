/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.isAlphaNumeric
import com.vaxcare.vaxhub.core.extension.isFullSsnAndNotNull
import com.vaxcare.vaxhub.core.extension.isMbiFormatAndNotNull
import com.vaxcare.vaxhub.core.extension.isPartialSsn
import com.vaxcare.vaxhub.core.extension.registerBroadcastReceiver
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.trimDashes
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.view.RoundedTextInputView
import com.vaxcare.vaxhub.core.view.SocialSecurityNumberView
import com.vaxcare.vaxhub.databinding.FragmentMedDCheckBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.checkout.isError
import com.vaxcare.vaxhub.model.checkout.isNotCovered
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import com.vaxcare.vaxhub.model.enums.CheckCopayAntigen
import com.vaxcare.vaxhub.model.metric.MedDCopayCheckResultNextClick
import com.vaxcare.vaxhub.model.metric.MedDMissingDataProvidedNextClick
import com.vaxcare.vaxhub.ui.checkout.adapter.MedDCheckAdapter
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.MedDCheckViewModel
import com.vaxcare.vaxhub.viewmodel.MedDCheckViewModel.MedDCheckState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MedDCheckFragment : BaseFragment<FragmentMedDCheckBinding>() {
    companion object {
        const val MEDD_COPAY_CHECK_FRAGMENT_RESULT_KEY = "medDCopayReviewDialogFragmentResultKey"
        const val MEDD_COPAY_CHECK_FRAGMENT_COPAY_KEY = "medDCopayReviewDialogFragmentCopayKey"
        private const val MBI_CHARACTER_LENGTH = 13
        private const val SSN_CHARACTER_LENGTH = 11

        const val COPAY_REVIEWED = "copay_reviewed"
        const val MEDD_CHECK_INFO = "medd_info"
        const val MATCH_COPAY_ANTIGEN = "match_copay_antigen"
        const val SSN_REGEX_PATERN = "^$|(\\d{3}-?\\d{2}-?(\\d{4}$))"
    }

    private val screenTitle = "MedDCopayCheck"

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations
    private val medDCheckViewModel: MedDCheckViewModel by viewModels()
    private val args: MedDCheckFragmentArgs by navArgs()

    private lateinit var medDCheckAdapter: MedDCheckAdapter
    private var resultsReceived: Boolean = false
    private var hasRunCheck = false
    private var checkCopayAntigen: CheckCopayAntigen = CheckCopayAntigen.DEFAULT
    private var appointment: Appointment? = null
    private var associatedCopay: ProductCopayInfo? = null
    private var isScreenIdle = false
    private var medDCheckEligible = false

    /**
     * MBI length is 13 alpha-numeric characters: XXXX-XXX-XXXX
     */
    private val mbiFilterLength by lazy { InputFilter.LengthFilter(MBI_CHARACTER_LENGTH) }

    /**
     * SSN length is 11 numeric characters: XXX-XX-XXXX
     */
    private val ssnFilterLength by lazy { InputFilter.LengthFilter(SSN_CHARACTER_LENGTH) }

    /**
     * This forces the input to be alpha-numeric characters only
     */
    private val inputFilterChars by lazy {
        InputFilter { source, _, _, _, _, _ -> if (source.isAlphaNumeric()) "" else null }
    }

    private var partDReceiverRegistered = false
    private var aceReceiverRegistered = false

    private val appointmentChangedEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val (incomingAppointmentId, changeReasonOrdinal) =
                intent?.extras?.getInt(Receivers.ACE_APPOINTMENT_ID) to
                    intent?.extras?.getInt(Receivers.ACE_CHANGE_REASON)
            val changeType =
                AppointmentChangeType.fromInt(intent?.extras?.getInt(Receivers.ACE_CHANGE_TYPE, -1))
            if (incomingAppointmentId == args.appointmentId) {
                when (val changeReason = AppointmentChangeReason.fromInt(changeReasonOrdinal)) {
                    AppointmentChangeReason.MedDCompleted,
                    AppointmentChangeReason.MedDError -> {
                        medDCheckViewModel.reportAceReceived(
                            changeReason,
                            changeType,
                            args.appointmentId
                        )

                        aceReceiverRegistered = false
                        unregister()
                    }

                    else -> Unit
                }
            }
        }
    }

    private val partDEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val incomingAppointmentId = intent?.extras?.getInt(Receivers.PART_D_PATIENT_VISIT_ID)
            if (incomingAppointmentId == args.appointmentId) {
                medDCheckViewModel.updatePartDResponseFromEvent(incomingAppointmentId, intent)
                partDReceiverRegistered = false
                unregister()
            }
        }
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_med_d_check,
        hasMenu = false,
        hasToolbar = false
    )

    override fun handleBack(): Boolean {
        if (isScreenIdle) {
            setFragmentResultAndGoBack()
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        registerReceivers()
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        showLoading()
        medDCheckViewModel.reportState(args.appointmentId, MedDCheckState.Loading)

        if (args.isMedDCheckStartedAlready) {
            medDCheckViewModel.checkForMedDCheckFinished()
        } else {
            medDCheckViewModel.runMedDCheckIfAvailable(
                appointmentId = args.appointmentId,
                isCheckMbi = args.isCheckMbi
            )
        }

        lifecycleScope.launch(Dispatchers.Main) {
            medDCheckViewModel.medDCheckUIState.collect { state ->
                medDCheckViewModel.reportState(args.appointmentId, state as? MedDCheckState)
                if (!resultsReceived) {
                    when (state) {
                        MedDCheckState.Loading -> showLoading()
                        MedDCheckState.TextChangeTimeout -> showLoadingText(R.string.taking_a_few_more_seconds)
                        MedDCheckState.MedDCheckFailed,
                        MedDCheckState.TimeoutResponse -> getLatestStateAndDisplayResults()

                        is MedDCheckState.MedDCheckAutoRunning -> appointment = state.appointment
                        is MedDCheckState.MissingCheckField -> setupUi(state.appointment)
                        is MedDCheckState.CopayResponse -> displayResults(state.medDInfo.parseCopays())
                    }
                }
            }
        }

        binding?.apply {
            topBar.onCloseAction = { handleBack() }
            fabNext.setOnSingleClickListener {
                when {
                    args.isCheckMbi -> runMedDCheckWithMbiOrGoBack()
                    else -> runMedDCheckOrGoBack()
                }
            }
        }
    }

    private fun MedDInfo?.parseCopays() =
        this?.let {
            medDCheckEligible = eligible
            associatedCopay = copays
                .filter { it.isCovered() }
                .firstOrNull { it.antigen.value == args.copayAntigen }
            checkCopayAntigen = if (associatedCopay != null) {
                CheckCopayAntigen.COPAY_MATCHED
            } else {
                CheckCopayAntigen.COPAY_UN_MATCH
            }
            this
        }

    private fun setupUi(fetchedAppointment: Appointment?) {
        appointment = fetchedAppointment
        hideLoading()
        binding?.apply {
            ssn.apply {
                isGone = args.isCheckMbi
                startChangedBehavior()
            }

            inputMbiOrSsn.imageViewClearButton.setOnSingleClickListener {
                inputMbiOrSsn.inputField.setText("")
                inputMbiOrSsn.inputField.openKeyboard()
            }

            inputMbiOrSsn.apply {
                isVisible = args.isCheckMbi
                startChangedBehavior()
            }
            mbiSsnText.isVisible = args.isCheckMbi
        }
    }

    private fun runMedDCheckWithMbiOrGoBack() {
        if (!hasRunCheck && binding?.inputMbiOrSsn.isMbiSsnInputValid()) {
            binding?.inputMbiOrSsn?.inputValue?.let {
                analytics.saveMetric(
                    MedDMissingDataProvidedNextClick(
                        args.appointmentId
                    )
                )
                hasRunCheck = true
                medDCheckViewModel.doMedDCheck(args.appointmentId, it.trimDashes())
            }
        } else {
            analytics.saveMetric(
                MedDCopayCheckResultNextClick(
                    isMedDEligible = medDCheckEligible,
                    patientVisitId = args.appointmentId
                )
            )
            setFragmentResultAndGoBack()
        }
    }

    private fun runMedDCheckOrGoBack() {
        if (!hasRunCheck && binding?.ssn?.isValid() == true) {
            binding?.ssn?.ssn?.let {
                analytics.saveMetric(
                    MedDMissingDataProvidedNextClick(
                        args.appointmentId
                    )
                )
                hasRunCheck = true
                medDCheckViewModel.doMedDCheck(args.appointmentId, it)
            }
        } else {
            analytics.saveMetric(
                MedDCopayCheckResultNextClick(
                    isMedDEligible = medDCheckEligible,
                    patientVisitId = args.appointmentId
                )
            )
            setFragmentResultAndGoBack()
        }
    }

    private fun RoundedTextInputView?.isMbiSsnInputValid() =
        this?.let {
            inputValue.isFullSsnAndNotNull() || inputValue.isMbiFormatAndNotNull()
        } ?: false

    private fun SocialSecurityNumberView.startChangedBehavior() =
        apply {
            onSsnChanged = {
                if (isValid()) hideKeyboard()
                binding?.fabNext?.isEnabled = isValid()
            }
            addTextChangedListener()
        }

    private fun RoundedTextInputView.startChangedBehavior() =
        apply {
            inputField.apply {
                setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                setOnSingleClickListener {
                    val editText = (it as EditText)
                    editText.setSelection(editText.text.length)
                    openKeyboard()
                }

                setOnTouchListener { v, _ ->
                    v.performClick()
                    true
                }

                filters = arrayOf(InputFilter.AllCaps(), inputFilterChars, mbiFilterLength)
                addTextChangedListener(textChangedListener)
                doAfterTextChanged { editable ->
                    editable?.toString().let { inputText ->
                        val textIsValid =
                            inputText.isFullSsnAndNotNull() || inputText.isMbiFormatAndNotNull()
                        binding?.inputMbiOrSsn?.imageViewClearButton?.isGone =
                            inputText.isNullOrEmpty()
                        binding?.fabNext?.isEnabled = textIsValid
                        if (textIsValid) {
                            hideKeyboard()
                        }
                    }
                }
                openKeyboard()
            }
        }

    private fun onSsnOrMbiTextChanged(
        text: CharSequence?,
        inputField: EditText?,
        count: Int
    ) {
        with(text.toString()) {
            inputField?.filters = arrayOf(
                InputFilter.AllCaps(),
                inputFilterChars,
                if (this.trimDashes().isPartialSsn()) {
                    ssnFilterLength
                } else {
                    mbiFilterLength
                }
            )
            when {
                lastOrNull() == '-' && count == 0 ->
                    inputField?.setAndSelectEndOfText(this.trimLastChar())

                count == 0 -> Unit
                shouldAppendHyphen(this) -> inputField?.setAndSelectEndOfText("$this-")
                else -> Unit
            }
        }
    }

    private fun EditText.openKeyboard() {
        requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(
            this,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    private fun shouldAppendHyphen(rawText: String): Boolean =
        shouldAppendHyphenSsn(rawText) || shouldAppendHyphenMbi(rawText)

    private fun shouldAppendHyphenSsn(rawSsnText: String) =
        rawSsnText.lastOrNull() != '-' &&
            rawSsnText.trimDashes().isPartialSsn() &&
            (rawSsnText.length == 3 || rawSsnText.length == 6)

    private fun shouldAppendHyphenMbi(rawMbiText: String) =
        rawMbiText.lastOrNull() != '-' &&
            !rawMbiText.trimDashes().isPartialSsn() &&
            (rawMbiText.length == 4 || rawMbiText.length == 8)

    private fun String.trimLastChar() = substring(0, (length - 1).coerceAtLeast(0))

    private fun EditText.setAndSelectEndOfText(value: String) {
        removeTextChangedListener(textChangedListener)
        setText(value)
        setSelection(value.length)
        addTextChangedListener(textChangedListener)
    }

    /**
     * This textChangedListener is an instance so we can remove and add when dynamically adding
     * hyphens to avoid recursion
     */
    private val textChangedListener = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(
            text: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            onSsnOrMbiTextChanged(text, binding?.inputMbiOrSsn?.inputField, count)
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun showLoading() {
        isScreenIdle = false
        binding?.root?.hide()
        startLoading()
        showLoadingText(R.string.med_d_check_running_copay_check)
    }

    private fun hideLoading() {
        isScreenIdle = true
        endLoading()
        binding?.root?.show()
    }

    private fun getLatestStateAndDisplayResults() {
        medDCheckViewModel.getLatestMedDInfoAndForceUpdate()
    }

    private fun displayResults(results: MedDInfo?) {
        binding?.apply {
            resultsReceived = true
            topBar.toolbarIconActive(false)
            headerText.hide()
            ssn.hide()
            mbiSsnText.hide()
            inputMbiOrSsn.hide()
            inputMbiOrSsn.imageViewClearButton.hide()
            resultsErrorText.hide()
            resultsHeaderText.show()

            when {
                results == null || results.isError() -> displayErrorResults(
                    medDInfo = results,
                    displayedMessage = resources.getString(R.string.med_d_check_error)
                )

                results.isNotCovered() -> displayErrorResults(
                    medDInfo = results,
                    displayedMessage = resources.getString(R.string.med_d_check_this_patient_not_eligible)
                )

                else -> {
                    medDCheckViewModel.saveMedDCheckRunMetric(
                        appointmentId = args.appointmentId,
                        medDInfo = results,
                        resultsUnavailable = false,
                        medDCheckStartedAt = args.medDCheckStartedAt,
                        checkOutContext = args.checkoutContext
                    )
                    medDCheckAdapter = MedDCheckAdapter(results.copays.toMutableList())
                    rvMedDResults.apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = medDCheckAdapter
                        isVisible = true
                    }
                }
            }

            fabNext.isEnabled = true
        }
        hideLoading()
    }

    private fun FragmentMedDCheckBinding.displayErrorResults(medDInfo: MedDInfo?, displayedMessage: String) {
        medDCheckViewModel.saveMedDCheckRunMetric(
            appointmentId = args.appointmentId,
            medDInfo = medDInfo,
            resultsUnavailable = medDInfo == null,
            displayedMessage = displayedMessage,
            medDCheckStartedAt = args.medDCheckStartedAt,
            checkOutContext = args.checkoutContext
        )
        resultsErrorText.text = getErrorStyledSpannableString(displayedMessage)
        resultsErrorText.show()

        fabNext.isEnabled = true
        hideLoading()
    }

    private fun getErrorStyledSpannableString(string: String): SpannableString {
        val spannableString = SpannableString(string)
        spannableString.setSpan(
            // what =
            StyleSpan(Typeface.ITALIC),
            // start =
            0,
            // end =
            string.length,
            // flags =
            0
        )
        return spannableString
    }

    private fun setFragmentResultAndGoBack() {
        medDCheckViewModel.updateAppointmentDetails(
            appointmentId = args.appointmentId,
            medDInfo = MedDInfo(
                eligible = medDCheckEligible,
                copays = listOfNotNull(associatedCopay)
            )
        )

        setFragmentResult(
            MEDD_COPAY_CHECK_FRAGMENT_RESULT_KEY,
            bundleOf(
                COPAY_REVIEWED to true,
                MATCH_COPAY_ANTIGEN to checkCopayAntigen.ordinal,
                MEDD_COPAY_CHECK_FRAGMENT_COPAY_KEY to associatedCopay
            )
        )

        globalDestinations.goBack(
            this@MedDCheckFragment,
            mapOf(
                COPAY_REVIEWED to true,
//                MEDD_CHECK_INFO to medDInfo
            )
        )
    }

    override fun onStop() {
        if (aceReceiverRegistered) {
            appointmentChangedEventReceiver.unregister()
        }
        if (partDReceiverRegistered) {
            partDEventReceiver.unregister()
        }
        super.onStop()
    }

    private fun registerReceivers() {
        appointmentChangedEventReceiver.register(Receivers.ACE_ACTION)
        partDEventReceiver.register(Receivers.PART_D_ACTION)
        aceReceiverRegistered = true
        partDReceiverRegistered = true
    }

    private fun BroadcastReceiver.register(filter: String) =
        context?.also { ctx ->
            ctx.registerBroadcastReceiver(
                receiver = this,
                intentFilter = IntentFilter(filter)
            )
        }

    private fun BroadcastReceiver.unregister() =
        context?.also { ctx ->
            ctx.unregisterReceiver(this)
        }

    override fun bindFragment(container: View): FragmentMedDCheckBinding = FragmentMedDCheckBinding.bind(container)
}
