/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.ui.extension.underlined
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.AndroidBug5497Workaround
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.constant.RegexPatterns
import com.vaxcare.vaxhub.core.extension.afterTextChanged
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentCollectPaymentBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.ui.navigation.MedDCheckoutDestination
import com.vaxcare.vaxhub.util.PhoneUtils
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class CollectPaymentFragment : BaseFragment<FragmentCollectPaymentBinding>() {
    private val screenTitle = "CollectPaymentInformation"
    private val args: CollectPaymentFragmentArgs by navArgs()

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: MedDCheckoutDestination

    private val commonLeftBg = R.drawable.bg_rounded_corner_left
    private val errorLeftBg = R.drawable.bg_rounded_corner_left_red

    private val commonRightBg = R.drawable.bg_rounded_corner_right
    private val errorRightBg = R.drawable.bg_rounded_corner_right_red
    private var viewAssist: AndroidBug5497Workaround? = null
    private val emailPattern = Pattern.compile(RegexPatterns.EMAIL_RFC_5322)

    private val commonColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.primary_black)
    }
    private val errorColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.error_red)
    }
    private var selectedMonth: String? = null
    private var selectedYear: String? = null
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()
    private val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment!!.id
    }
    private val patientId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment!!.patient.id
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_collect_payment,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentCollectPaymentBinding =
        FragmentCollectPaymentBinding.bind(container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appointmentViewModel.clearAllPhoneDeltas(COLLECT_PHONE_DATA_FRAGMENT_TAG)
        appointmentViewModel.currentCheckout.phoneContactReasons.removeAll(
            setOf(
                PhoneContactReasons.SELF_PAY,
                PhoneContactReasons.COPAY
            )
        )
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewAssist = AndroidBug5497Workaround.assistActivity(requireActivity())
        logScreenNavigation(screenTitle)
        binding?.medDCopayCardNumber?.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    toggleCardNumberStatus(binding?.medDCopayCardNumber?.cardNumber == null)
                }
            }
        addStateHandlerCallback()
        binding?.medDCopayCardNumber?.completionCallback = {
            binding?.medDCopayCardNumber?.clearFocus()
        }

        binding?.medDCopayCardNumber?.errorCallback = { showError ->
            toggleCardNumberStatus(showError)
        }

        binding?.medDCopayCardNumber?.afterTextChanged {
            checkInvalid()
        }

        binding?.medDCopayExpirationMonth?.setOnClickListener {
            val month = binding?.medDCopayExpirationMonth?.text.toString()
            val monthList = (1..12).map { it.toString() }
            val selectedIndex = monthList.indexOf(month)
            val monthSimplyArray = resources.getStringArray(R.array.array_month)
            val bottomDialog = BottomDialog.newInstance(
                requireContext().getString(R.string.select_expiration_month_placeholder)
                    .uppercase(),
                monthList.map { monthSimplyArray[it.toInt() - 1] },
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                selectedMonth = monthList[index]
                binding?.medDCopayExpirationMonth?.text = selectedMonth
                if (selectedYear == null) {
                    binding?.medDCopayExpirationYear?.performClick()
                }
                toggleExpirationStatus()
                checkInvalid()
            }
            bottomDialog.show(
                childFragmentManager,
                "monthBottomDialog"
            )
        }

        binding?.medDCopayExpirationYear?.setOnClickListener {
            val year = binding?.medDCopayExpirationYear?.text.toString()
            val currentYear = LocalDate.now().year
            val yearList = (currentYear..(currentYear + 20)).map { it.toString() }
            val selectedIndex = yearList.indexOf(year)
            val bottomDialog = BottomDialog.newInstance(
                requireContext().getString(R.string.select_expiration_year_placeholder)
                    .toUpperCase(Locale.getDefault()),
                yearList,
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                selectedYear = yearList[index]
                binding?.medDCopayExpirationYear?.text = selectedYear
                toggleExpirationStatus()
                checkInvalid()
            }
            bottomDialog.show(
                childFragmentManager,
                "yearBottomDialog"
            )
        }

        val rawPhone = args.currentPhone?.replace("-", "")
        PhoneUtils.disassemblePhone(rawPhone)?.let { phoneParts ->
            binding?.medDCopayAddPhoneStart?.setText(phoneParts[0])
            binding?.medDCopayAddPhoneMid?.setText(phoneParts[1])
            binding?.medDCopayAddPhoneEnd?.setText(phoneParts[2])
        }

        binding?.medDCopayAddPhoneStart?.doAfterTextChanged {
            if (binding?.medDCopayAddPhoneStart?.text?.length == 3) {
                binding?.medDCopayAddPhoneMid?.requestFocus()
            }
            checkInvalid()
        }

        binding?.medDCopayAddPhoneMid?.doAfterTextChanged {
            if (binding?.medDCopayAddPhoneMid?.text?.length == 3) {
                binding?.medDCopayAddPhoneEnd?.requestFocus()
            } else if (binding?.medDCopayAddPhoneMid?.text.isNullOrEmpty()) {
                binding?.medDCopayAddPhoneStart?.requestFocus()
            }
            checkInvalid()
        }

        binding?.medDCopayAddPhoneEnd?.doAfterTextChanged {
            if (binding?.medDCopayAddPhoneEnd?.text?.length == 4) {
                binding?.medDCopayEmail?.requestFocus()
            } else if (binding?.medDCopayAddPhoneEnd?.text.isNullOrEmpty()) {
                if (binding?.medDCopayAddPhoneMid?.text.isNullOrEmpty()) {
                    binding?.medDCopayAddPhoneStart?.requestFocus()
                } else {
                    binding?.medDCopayAddPhoneMid?.requestFocus()
                }
            }
            checkInvalid()
        }

        binding?.medDCopayCardName?.afterTextChanged {
            checkInvalid()
        }

        binding?.medDCopayEmail?.afterTextChanged {
            checkInvalid()
        }

        binding?.medDCopaySave?.setOnClickListener {
            savePaymentInformation(appointmentId)
        }

        binding?.medDCopayExpirationMonth?.text = selectedMonth
        binding?.medDCopayExpirationYear?.text = selectedYear

        NoInsuranceCardFlow.fromOrdinal(args.ordinalFlow)?.let { flow ->
            binding?.medDNoCard?.apply {
                val textId = if (args.enablePhoneCollection) {
                    R.string.patient_add_capture_no_card
                } else {
                    R.string.med_d_copay_no_charge
                }
                setText(textId)
                show()
                underlined()
                setOnSingleClickListener {
                    if (args.enablePhoneCollection) {
                        startPhoneCollectionFlow(flow)
                    } else {
                        destination.goToPaymentSummaryFromCollectPayment(
                            fragment = this@CollectPaymentFragment,
                            appointmentId = appointmentId,
                            paymentInfo = null
                        )
                    }
                }
            }
        }
    }

    private fun startPhoneCollectionFlow(flow: NoInsuranceCardFlow) {
        val (phoneOptIn, phoneOptOut) =
            appointmentViewModel.deltaPhoneOptInPresent() to appointmentViewModel.deltaPhoneOptOutPresent()

        if (!phoneOptIn && !phoneOptOut) {
            destination.goToPatientNoCard(
                fragment = this,
                flow = flow,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = args.currentPhone ?: appointmentViewModel.currentCheckout
                    .selectedAppointment?.patient?.phoneNumber
            )
        } else {
            val paymentInformation = if (phoneOptOut) {
                null
            } else {
                appointmentViewModel.deltaFields
                    .firstOrNull { field -> field is DemographicField.Phone }?.currentValue?.let { phone ->
                        PaymentInformationRequestBody("", "", "", "", phone)
                    }
            }

            if (phoneOptIn) {
                val hasCopays =
                    appointmentViewModel.currentCheckout.stagedProducts.any { it.hasCopay() }
                val hasSelfPay =
                    appointmentViewModel.currentCheckout.stagedProducts.any { it.paymentMode == PaymentMode.SelfPay }

                val phoneReasons = listOfNotNull(
                    if (hasCopays) {
                        PhoneContactReasons.COPAY
                    } else {
                        null
                    },
                    if (hasSelfPay) {
                        PhoneContactReasons.SELF_PAY
                    } else {
                        null
                    }
                )

                appointmentViewModel.currentCheckout.phoneContactReasons
                    .addAll(phoneReasons.toSet())
            }

            destination.goToNurseDisclaimer(
                fragment = this,
                appointmentId = appointmentId,
                paymentInfo = paymentInformation,
                preAccepted = phoneOptIn
            )
        }
    }

    private fun toggleCardNumberStatus(showError: Boolean) {
        binding?.medDCopayInvalidInfo?.isInvisible = showError.not()
        binding?.medDCopayBgNumber?.isInvisible = showError.not()
    }

    private fun toggleExpirationStatus() {
        try {
            if (selectedMonth != null && selectedYear != null) {
                if (isValidExpiration()) {
                    binding?.medDCopayExpirationTitle?.setTextColor(commonColor)
                    binding?.medDCopayExpirationMonth?.setBackgroundResource(commonLeftBg)
                    binding?.medDCopayExpirationYear?.setBackgroundResource(commonRightBg)
                } else {
                    binding?.medDCopayExpirationTitle?.setTextColor(errorColor)
                    binding?.medDCopayExpirationMonth?.setBackgroundResource(errorLeftBg)
                    binding?.medDCopayExpirationYear?.setBackgroundResource(errorRightBg)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isValidExpiration(): Boolean {
        try {
            if (selectedMonth != null && selectedYear != null) {
                val expiration = YearMonth.of(selectedYear!!.toInt(), selectedMonth!!.toInt())
                return expiration.equals(YearMonth.now()) || expiration.isAfter(YearMonth.now())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun checkInvalid() {
        val emailText = binding?.medDCopayEmail?.text?.trim() ?: ""
        binding?.medDCopaySave?.isEnabled = binding?.medDCopayCardNumber?.cardNumber != null &&
            isValidExpiration() &&
            binding?.medDCopayCardName?.text?.isNotBlank() == true &&
            binding?.medDCopayAddPhoneStart?.text?.trim()?.length == 3 &&
            binding?.medDCopayAddPhoneMid?.text?.trim()?.length == 3 &&
            binding?.medDCopayAddPhoneEnd?.text?.trim()?.length == 4 &&
            emailPattern.matcher(emailText).matches()
    }

    private fun savePaymentInformation(appointmentId: Int) {
        val cardNumber = requireNotNull(binding?.medDCopayCardNumber?.cardNumber)
        val paymentInformation = PaymentInformationRequestBody(
            expirationDate = "${binding?.medDCopayExpirationMonth?.text}/${binding?.medDCopayExpirationYear?.text}",
            email = binding?.medDCopayEmail?.text?.trim().toString(),
            cardholderName = binding?.medDCopayCardName?.text?.trim().toString(),
            cardNumber = cardNumber,
            phoneNumber = "${binding?.medDCopayAddPhoneStart?.text}" +
                "${binding?.medDCopayAddPhoneMid?.text}" +
                "${binding?.medDCopayAddPhoneEnd?.text}",
        )
        destination.goToPaymentSummaryFromCollectPayment(
            fragment = this,
            appointmentId = appointmentId,
            paymentInfo = paymentInformation
        )
    }

    private fun addStateHandlerCallback() {
        getResultLiveData<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)?.observe(
            viewLifecycleOwner
        ) { data ->
            val hasCopays =
                appointmentViewModel.currentCheckout.stagedProducts.any { it.hasCopay() }
            val hasSelfPay =
                appointmentViewModel.currentCheckout.stagedProducts.any {
                    it.appointmentPaymentMethod == PaymentMethod.SelfPay ||
                        it.paymentMode == PaymentMode.SelfPay
                }

            val phoneReasons = listOfNotNull(
                if (hasCopays) {
                    PhoneContactReasons.COPAY
                } else {
                    null
                },
                if (hasSelfPay) {
                    PhoneContactReasons.SELF_PAY
                } else {
                    null
                }
            )

            val agreed = appointmentViewModel.addPhoneCollectField(
                tagSet = COLLECT_PHONE_DATA_FRAGMENT_TAG,
                data = data,
                reasons = phoneReasons.toTypedArray()
            )

            val paymentInformation = if (agreed) {
                data.currentPhone?.let {
                    PaymentInformationRequestBody("", "", "", "", it)
                }
            } else {
                null
            }

            removeResult<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)
            destination.goToPaymentSummaryFromCollectPayment(
                fragment = this,
                appointmentId = appointmentId,
                paymentInfo = paymentInformation
            )
        }
    }

    override fun onDestroyView() {
        hideKeyboard()
        viewAssist?.stopAssistingActivity()
        super.onDestroyView()
    }
}
