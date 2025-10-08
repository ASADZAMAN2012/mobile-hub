/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.intToEnum
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseRestrictedFragment
import com.vaxcare.vaxhub.databinding.FragmentCheckoutCompleteBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.service.UserSessionService
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutCompleteViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutCompleteFragment : BaseRestrictedFragment<FragmentCheckoutCompleteBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var localStorage: LocalStorage

    @Inject
    override lateinit var sessionService: UserSessionService

    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CheckoutSummaryDestination

    private val screenTitle = "CheckoutComplete"
    private val args: CheckoutCompleteFragmentArgs by navArgs()
    private val checkoutCompleteViewModel: CheckoutCompleteViewModel by viewModels()
    private var isIntegrationTypeBi: Boolean = false

    private val appointmentViewModel: AppointmentViewModel by activityViewModels()
    private val selectedAppointment by lazy { appointmentViewModel.currentCheckout.selectedAppointment }

    private val count by lazy { args.shotCount }
    private val isMultiplePaymentMode by lazy { args.isMultiplePayment }

    override val fragmentProperties = FragmentProperties(
        resource = R.layout.fragment_checkout_complete,
        hasMenu = false,
        hasToolbar = false
    )

    override fun handleBack(): Boolean = true

    override fun canShowConnection(): Boolean = false

    private enum class LoginActionCorrelation {
        DEFAULT,
        GO_BACK,
        CHECKOUT_ANOTHER
    }

    override fun bindFragment(container: View): FragmentCheckoutCompleteBinding =
        FragmentCheckoutCompleteBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        checkoutCompleteViewModel.loadFeatures()
        binding?.checkoutRightClippingBg?.clipToOutline = true
        checkoutCompleteViewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CheckoutCompleteViewModel.CheckoutCompleteState.FeaturesLoaded -> {
                    isIntegrationTypeBi = state.isIntegrationTypeBi
                    selectedAppointment?.let {
                        setAppointmentInfo(it)
                    }
                }
            }
        }
        initEvents()
    }

    private fun initEvents() {
        binding?.checkoutEdit?.setOnSingleClickListener {
            checkSessionNavigation(
                titleResId = R.string.loginPinFragment_enterPin,
                correlation = LoginActionCorrelation.GO_BACK.ordinal
            )
        }

        binding?.checkOutAnother?.setOnSingleClickListener {
            checkSessionNavigation(
                titleResId = R.string.loginPinFragment_enterPin,
                correlation = LoginActionCorrelation.CHECKOUT_ANOTHER.ordinal
            )
        }

        binding?.checkoutLogOut?.setOnSingleClickListener {
            appointmentViewModel.clearCurrentCheckout()
            globalDestinations.goBackToSplash(this@CheckoutCompleteFragment)
        }
    }

    private fun handleActionForEditCheckout() {
        appointmentViewModel.currentCheckout.isEditCheckoutFromComplete = true
    }

    @SuppressLint("InflateParams")
    private fun setAppointmentInfo(appointment: Appointment) {
        // note this is technically incorrect as the patient will be checked out,
        // this would actually be the state prior to checkout
        // e.g. checkedOut =true: previously checked out, =false: just checked out
        if (appointment.checkedOut) {
            binding?.checkoutCompleteTitle?.text =
                getString(R.string.patient_checkout_complete_updated)
        } else {
            binding?.checkoutCompleteTitle?.text =
                getString(R.string.patient_checkout_complete)
        }

        val shouldShowEHRDialogEnabled = appointmentViewModel.currentCheckout.isLocallyCreated &&
            !appointmentViewModel.currentCheckout.isEditCheckoutFromComplete &&
            isIntegrationTypeBi

        binding?.ehrContainer?.isVisible = shouldShowEHRDialogEnabled

        Timber.d("isMultiplePaymentMode = $isMultiplePaymentMode")
        if (isMultiplePaymentMode) {
            binding?.checkoutEdit?.hide()
        } else {
            binding?.checkoutEdit?.show()
        }

        val dobString = appointment.patient.getDobString()
        binding?.patientShotAdministered?.text =
            resources.getQuantityString(R.plurals.shot_administered, count, count)

        val dob = if (dobString != null) {
            LocalDate.parse(dobString, DateTimeFormatter.ofPattern("M/dd/yyyy"))
        } else {
            null
        }

        val patient = appointment.patient
        val paymentInformation = patient.paymentInformation
        val patientIsChild =
            dob != null && ChronoUnit.YEARS.between(dob, LocalDateTime.now()).toInt() < 26
        val childAppointmentId = appointmentViewModel.getChildAppointmentId()

        checkoutCompleteViewModel.loadPotentialFamilyAppointments(appointment)

        checkoutCompleteViewModel.familyAppointments.observe(viewLifecycleOwner) { familyMembers ->
            binding?.checkoutCompletePatientName?.text = requireContext().formatString(
                R.string.patient_name_display,
                appointment.patient.firstName.captureName(),
                appointment.patient.lastName.captureName()
            )
            if (!appointment.patient.originatorPatientId.isNullOrEmpty()) {
                binding?.patientId?.text = appointment.patient.originatorPatientId
            } else {
                binding?.patientId?.text = appointment.patient.id.toString()
            }
            binding?.patientDob?.text = dobString ?: ""
        }
    }

    override fun onLoginSuccess(data: Int) {
        super.onLoginSuccess(data)
        val loginCorrelation = intToEnum(data, LoginActionCorrelation.DEFAULT)
        reportLogin(true, loginCorrelation)
        when (loginCorrelation) {
            LoginActionCorrelation.GO_BACK -> {
                appointmentViewModel.clearCurrentCheckout()
                handleActionForEditCheckout()
                globalDestinations.goBackToCheckout(this@CheckoutCompleteFragment)
            }

            LoginActionCorrelation.CHECKOUT_ANOTHER -> {
                appointmentViewModel.clearCurrentCheckout()
                globalDestinations.goBackToAppointmentList(
                    this@CheckoutCompleteFragment,
                    selectedAppointment?.appointmentTime?.toLocalDate()
                )
            }

            else -> Timber.e("Error: LoginCorrelation for CheckoutComplete unknown")
        }
    }

    override fun onLoginAbort(data: Int) {
        super.onLoginAbort(data)
        reportLogin(false, intToEnum(data, LoginActionCorrelation.DEFAULT))
    }

    override fun onLoginFailure(data: Int) {
        reportLogin(false, intToEnum(data, LoginActionCorrelation.DEFAULT))
        appointmentViewModel.clearCurrentCheckout()
        super.onLoginFailure(data)
    }

    private fun reportLogin(success: Boolean, loginCorrelation: LoginActionCorrelation) {
        val attemptedNavigation = when (loginCorrelation) {
            LoginActionCorrelation.GO_BACK -> "EditCurrentCheckout"
            LoginActionCorrelation.CHECKOUT_ANOTHER -> "AppointmentList"
            else -> "Unknown"
        }

        checkoutCompleteViewModel.reportLogin(success, attemptedNavigation, selectedAppointment?.id)
    }
}
