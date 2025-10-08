/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentPatientEditCollectPhoneBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.ui.navigation.PhoneCollectDestination
import com.vaxcare.vaxhub.viewmodel.PatientEditCollectPhoneViewModel
import com.vaxcare.vaxhub.viewmodel.PatientEditCollectPhoneViewModel.CollectPhoneState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PatientEditCollectPhoneFragment : BaseFragment<FragmentPatientEditCollectPhoneBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: PhoneCollectDestination

    private val screenTitle = "PhoneNumberCapture"
    private val args: PatientEditCollectPhoneFragmentArgs by navArgs()
    private val viewModel: PatientEditCollectPhoneViewModel by viewModels()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_patient_edit_collect_phone,
        hasToolbar = false
    )

    override fun handleBack(): Boolean = true

    override fun bindFragment(container: View): FragmentPatientEditCollectPhoneBinding =
        FragmentPatientEditCollectPhoneBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        binding?.topBar?.onCloseAction = {
            // This is intended to allow pinning in while navigating to the previous flow
            args.data.flow = NoInsuranceCardFlow.ABORT
            continueNavigation()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CollectPhoneState.InitialState -> {
                    args.data.isSessionLocked = state.isSessionLocked
                    toggleInvalidPhone(true)
                    toggleContinueBtn(false)
                }

                is CollectPhoneState.PhoneNumberFetched -> {
                    args.data.isSessionLocked = state.isSessionLocked
                    prePopulateNumber(
                        state.area,
                        state.prefix,
                        state.line
                    )
                }

                is CollectPhoneState.PhoneNumberProvided -> toggleViews(true)

                is CollectPhoneState.PhoneNumberNotValid -> toggleViews(false)
            }
        }

        setupUI()
        viewModel.setPhoneHint(args.data.currentPhone, args.data.appointmentId)
    }

    private fun setupUI() {
        binding?.continueBtn?.setOnSingleClickListener {
            val extractedPhone = extractEnteredPhoneNumber()
            with(args.data) {
                phoneContactAgreement = true
                phoneNumberUpdated = args.data.currentPhone != extractedPhone
                currentPhone = extractedPhone
            }

            continueNavigation()
        }

        binding?.noAgreeBtn?.setOnSingleClickListener {
            with(args.data) {
                phoneContactAgreement = false
                phoneNumberUpdated = false
                currentPhone = null
            }

            continueNavigation()
        }

        binding?.patientAddPhoneStart?.doOnTextChanged { text, _, _, _ ->
            text?.let {
                viewModel.validateAndUpdatePhone(
                    it.toString(),
                    binding?.patientAddPhoneMid?.text.toString(),
                    binding?.patientAddPhoneEnd?.text.toString()
                )

                if (it.length == 3) {
                    binding?.patientAddPhoneMid?.apply {
                        requestFocus()
                        post { setSelection(this.text.length) }
                    }
                }
            }
        }

        binding?.patientAddPhoneMid?.doOnTextChanged { text, start, previous, ct ->
            text?.let {
                viewModel.validateAndUpdatePhone(
                    binding?.patientAddPhoneStart?.text.toString(),
                    it.toString(),
                    binding?.patientAddPhoneEnd?.text.toString()
                )

                if (it.length == 3) {
                    binding?.patientAddPhoneEnd?.apply {
                        requestFocus()
                        post { setSelection(this.text.length) }
                    }
                }
            }

            if (start == 0 && previous == 1 && ct == 0) {
                binding?.patientAddPhoneStart?.apply {
                    requestFocus()
                    post { setSelection(this.text.length) }
                }
            }
        }

        binding?.patientAddPhoneEnd?.doOnTextChanged { text, start, previous, ct ->
            text?.let {
                viewModel.validateAndUpdatePhone(
                    binding?.patientAddPhoneStart?.text.toString(),
                    binding?.patientAddPhoneMid?.text.toString(),
                    it.toString()
                )

                if (it.length == 4) {
                    hideKeyboard()
                }
            }

            if (start == 0 && previous == 1 && ct == 0) {
                binding?.patientAddPhoneMid?.apply {
                    requestFocus()
                    post { setSelection(this.text.length) }
                }
            }
        }
    }

    private fun continueNavigation() {
        toggleContinueBtn(false)

        // Do navigation
        destination.goToPhoneNumberProvided(
            fragment = this@PatientEditCollectPhoneFragment,
            data = args.data
        )
    }

    private fun toggleViews(enabled: Boolean) {
        toggleContinueBtn(enabled)
        toggleInvalidPhone(enabled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        viewModel.state.removeObservers(this)
    }

    private fun prePopulateNumber(
        area: String,
        prefix: String,
        line: String
    ) {
        binding?.patientAddPhoneStart?.setText(area)
        binding?.patientAddPhoneMid?.setText(prefix)
        binding?.patientAddPhoneEnd?.setText(line)
        toggleContinueBtn(true)
        toggleInvalidPhone(false)
        // Phone number already on record and pre-filled
        args.data.phoneNumberPrefilled = true
    }

    private fun toggleInvalidPhone(valid: Boolean) =
        if (valid) {
            binding?.patientAddPhoneInvalid?.invisible()
            binding?.layoutCollectPhone?.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.bg_rounded_corner_white,
                null
            )
        } else {
            binding?.patientAddPhoneInvalid?.show()
            binding?.layoutCollectPhone?.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.bg_rounded_corner_lightest_yellow,
                null
            )
        }

    private fun toggleContinueBtn(enabled: Boolean) {
        binding?.continueBtn?.isEnabled = enabled
    }

    private fun extractEnteredPhoneNumber(): String =
        "${binding?.patientAddPhoneStart?.text}${binding?.patientAddPhoneMid?.text}${binding?.patientAddPhoneEnd?.text}"
}
