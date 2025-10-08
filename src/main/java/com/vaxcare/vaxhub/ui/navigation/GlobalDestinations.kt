/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.NavDirections
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.ui.appointment.AppointmentCheckoutMetaData
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.enums.AppointmentNavigateStatus
import com.vaxcare.vaxhub.model.login.LoginOptions
import com.vaxcare.vaxhub.ui.checkout.AppointmentListFragment
import com.vaxcare.vaxhub.ui.checkout.extensions.toAppointmentNavigateStatus
import com.vaxcare.vaxhub.ui.patient.CurbsideConfirmPatientInfoFragmentArgs
import com.vaxcare.vaxhub.ui.patient.edit.CheckoutCollectDobFragmentArgs
import java.time.LocalDate

interface GlobalDestinations {
    fun goToErrorDialog(
        fragment: Fragment?,
        @StringRes title: Int,
        @StringRes body: Int,
        @StringRes primaryBtn: Int,
        @StringRes secondaryBtn: Int
    )

    fun goToErrorPrompt(
        fragment: Fragment?,
        @StringRes title: Int,
        @StringRes body: Int,
        @StringRes primaryBtn: Int,
    )

    fun goToCurbsideAddPatient(
        fragment: Fragment?,
        appointmentId: Int = -1,
        addPatientSource: Int = -1
    )

    fun goToCheckout(
        fragment: Fragment?,
        appointmentId: Int = -1,
        curbsideNewPatient: Boolean = false,
        isForceRiskFree: Boolean = false,
        isLocallyCreated: Boolean = false,
        updateData: PatientCollectData? = null
    )

    fun goToCheckoutSummary(fragment: Fragment?, appointmentId: Int)

    fun toLotLookup(
        fragment: Fragment?,
        productId: Int = 0,
        appointmentId: Int? = null,
        relativeDoS: String? = null
    )

    fun startCheckoutWithCreatePatient(
        fragment: Fragment?,
        appointment: Appointment,
        analytics: AnalyticReport,
        data: PatientCollectData?,
        isNewPatient: Boolean,
    )

    fun startCheckoutWithCollectPhone(
        fragment: Fragment?,
        appointment: Appointment,
        data: PatientCollectData?
    )

    fun startCheckout(
        fragment: Fragment?,
        appointment: Appointment,
        analytics: AnalyticReport,
        isForceRiskFree: Boolean = false,
        isLocallyCreated: Boolean = false,
        overrideNavigateToCheckout: (() -> Unit?)? = null
    )

    fun goBack(fragment: Fragment?, backData: Map<String, Any?>? = null)

    fun goBackToSplash(fragment: Fragment?)

    fun goBackToCheckout(fragment: Fragment?)

    fun goBackToAppointmentList(fragment: Fragment?, localDate: LocalDate? = null)

    fun goToCurbsidePatientSearch(fragment: Fragment?)

    fun goToAddAppointmentOrCreatePatient(fragment: Fragment?)

    fun goToCreatePatient(
        fragment: Fragment,
        patientId: Int = -1,
        providerId: Int = -1,
        patientCollectData: PatientCollectData? = null
    )

    fun goToEnhancedPinIn(
        fragment: Fragment?,
        username: String,
        titleResId: Int = 0,
        correlation: Int = -1,
        disableReset: Boolean = false
    )

    fun goToEnhancedPassword(
        fragment: Fragment?,
        username: String,
        titleResId: Int = 0,
        correlation: Int = -1,
        disableReset: Boolean = false
    )

    fun goToTroubleConnectingDialog(fragment: Fragment, bodyStringRes: Int)

    fun goToKeepDeviceInHands(fragment: Fragment, isAddPatientFlow: Boolean = false)

    fun goToCurbsideConfirmPatientInfo(fragment: Fragment?, patientId: Int)

    fun goToPatientInfoConfirmationDialog(
        fragment: Fragment?,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        address: String
    )

    fun goToDoBCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        isForceRiskFree: Boolean = false,
        isLocallyCreated: Boolean = false,
    )
}

class GlobalDestinationsImpl(private val navCommons: NavCommons) : GlobalDestinations {
    override fun goToErrorDialog(
        fragment: Fragment?,
        @StringRes title: Int,
        @StringRes body: Int,
        @StringRes primaryBtn: Int,
        @StringRes secondaryBtn: Int
    ) {
        val action = NavDirections.actionGlobalErrorDialog(
            title = title,
            body = body,
            primaryButton = primaryBtn,
            secondaryButton = secondaryBtn
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToErrorPrompt(
        fragment: Fragment?,
        @StringRes title: Int,
        @StringRes body: Int,
        @StringRes primaryBtn: Int
    ) {
        val action = NavDirections.actionGlobalErrorPrompt(
            title = title,
            body = body,
            primaryButton = primaryBtn
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCurbsideAddPatient(
        fragment: Fragment?,
        appointmentId: Int,
        addPatientSource: Int
    ) {
        val action = NavDirections.actionGlobalCurbsideAddPatient(
            appointmentId = appointmentId,
            addPatientSource = addPatientSource
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToCheckout(
        fragment: Fragment?,
        appointmentId: Int,
        curbsideNewPatient: Boolean,
        isForceRiskFree: Boolean,
        isLocallyCreated: Boolean,
        updateData: PatientCollectData?
    ) {
        val action = NavDirections.actionGlobalCheckout(
            appointmentCheckoutMetaData = AppointmentCheckoutMetaData(
                appointmentId = appointmentId,
                isForceRiskFree = isForceRiskFree,
                isLocallyCreated = isLocallyCreated,
                curbsideNewPatient = curbsideNewPatient,
                updateData = updateData
            )
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCheckoutSummary(fragment: Fragment?, appointmentId: Int) {
        val action = NavDirections.actionGlobalCheckoutSummaryFragment(
            appointmentId = appointmentId
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun toLotLookup(
        fragment: Fragment?,
        productId: Int,
        appointmentId: Int?,
        relativeDoS: String?
    ) {
        val action = NavDirections.actionGlobalLotLookup(
            productId = productId,
            appointmentId = appointmentId?.toString(),
            relativeDoS = relativeDoS
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun goBack(fragment: Fragment?, backData: Map<String, Any?>?) {
        navCommons.goBack(fragment, backData)
    }

    override fun goBackToSplash(fragment: Fragment?) {
        navCommons.goBackPopTo(fragment, R.id.splashFragment)
    }

    override fun goBackToCheckout(fragment: Fragment?) {
        navCommons.goBackPopTo(fragment, R.id.checkoutPatientFragment)
    }

    override fun goBackToAppointmentList(fragment: Fragment?, localDate: LocalDate?) {
        val data = mapOf<String, Any?>(
            AppointmentListFragment.APPOINTMENT_DATE to localDate
        )

        navCommons.goBackPopTo(fragment, R.id.appointmentListFragment, data)
    }

    override fun goToCurbsidePatientSearch(fragment: Fragment?) {
        val action = NavDirections.actionGlobalCurbsidePatientSearchFragment()

        navCommons.goToFragment(fragment, action)
    }

    override fun goToAddAppointmentOrCreatePatient(fragment: Fragment?) {
        val action = NavDirections.actionGlobalAddAppointmentOrCreatePatientFragment()

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCreatePatient(
        fragment: Fragment,
        patientId: Int,
        providerId: Int,
        patientCollectData: PatientCollectData?
    ) {
        val action = NavDirections.actionGlobalCreatePatientFragment(
            patientId = patientId,
            providerId = providerId,
            collectPhoneData = patientCollectData
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun startCheckoutWithCreatePatient(
        fragment: Fragment?,
        appointment: Appointment,
        analytics: AnalyticReport,
        data: PatientCollectData?,
        isNewPatient: Boolean,
    ) {
        goBackToAppointmentList(fragment, appointment.appointmentTime.toLocalDate())

        startCheckout(
            fragment = fragment,
            appointment = appointment,
            analytics = analytics,
            isLocallyCreated = false
        ) {
            goToCheckout(
                fragment = fragment,
                appointmentId = appointment.id,
                curbsideNewPatient = isNewPatient
            )
        }
    }

    override fun startCheckoutWithCollectPhone(
        fragment: Fragment?,
        appointment: Appointment,
        data: PatientCollectData?
    ) {
        goBackToAppointmentList(fragment, appointment.appointmentTime.toLocalDate())

        goToCheckout(fragment, appointmentId = appointment.id)
    }

    override fun startCheckout(
        fragment: Fragment?,
        appointment: Appointment,
        analytics: AnalyticReport,
        isForceRiskFree: Boolean,
        isLocallyCreated: Boolean,
        overrideNavigateToCheckout: (() -> Unit?)?
    ) {
        when (appointment.toAppointmentNavigateStatus()) {
            AppointmentNavigateStatus.AppointmentNavigateToDob -> {
                navCommons.goToFragment(
                    fragment,
                    R.id.checkoutCollectDobFragment,
                    CheckoutCollectDobFragmentArgs(
                        appointmentId = appointment.id,
                        patientId = appointment.patient.id,
                        isForceRiskFree = isForceRiskFree,
                        isLocallyCreated = isLocallyCreated
                    ).toBundle()
                )
            }

            AppointmentNavigateStatus.AppointmentNavigateToCheckout -> {
                overrideNavigateToCheckout?.invoke() ?: run {
                    goToCheckout(
                        fragment = fragment,
                        appointmentId = appointment.id,
                        isForceRiskFree = isForceRiskFree,
                        isLocallyCreated = isLocallyCreated
                    )
                }
            }
        }
    }

    override fun goToDoBCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        isForceRiskFree: Boolean,
        isLocallyCreated: Boolean,
    ) {
        navCommons.goToFragment(
            fragment,
            R.id.checkoutCollectDobFragment,
            CheckoutCollectDobFragmentArgs(
                appointmentId = appointmentId,
                patientId = patientId,
                isForceRiskFree = isForceRiskFree,
                isLocallyCreated = isLocallyCreated
            ).toBundle()
        )
    }

    override fun goToEnhancedPinIn(
        fragment: Fragment?,
        username: String,
        titleResId: Int,
        correlation: Int,
        disableReset: Boolean
    ) {
        val action = NavDirections.actionGlobalLoginPinFragment(
            options = LoginOptions(
                username = username,
                titleResourceId = titleResId,
                loginCorrelation = correlation,
                disableReset = disableReset
            )
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToTroubleConnectingDialog(
        fragment: Fragment,
        @StringRes bodyStringRes: Int
    ) {
        val action = NavDirections.actionGlobalTroubleConnectingDialog(bodyStringRes)

        navCommons.goToFragment(fragment, action)
    }

    override fun goToKeepDeviceInHands(fragment: Fragment, isAddPatientFlow: Boolean) {
        val action = NavDirections.actionGlobalKeepDeviceInHandsDialog(
            isAddPatientFlow = isAddPatientFlow
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToEnhancedPassword(
        fragment: Fragment?,
        username: String,
        titleResId: Int,
        correlation: Int,
        disableReset: Boolean
    ) {
        val action = NavDirections.actionGlobalLoginPasswordFragment(
            options = LoginOptions(
                username = username,
                titleResourceId = titleResId,
                loginCorrelation = correlation,
                disableReset = disableReset
            )
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToCurbsideConfirmPatientInfo(fragment: Fragment?, patientId: Int) {
        navCommons.goToFragment(
            fragment,
            R.id.curbsideConfirmPatientInfoFragment,
            CurbsideConfirmPatientInfoFragmentArgs(patientId = patientId).toBundle()
        )
    }

    override fun goToPatientInfoConfirmationDialog(
        fragment: Fragment?,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        address: String
    ) {
        val action = NavDirections.actionGlobalPatientInfoConfirmationDialog(
            firstName,
            lastName,
            dateOfBirth,
            address
        )
        navCommons.goToFragment(fragment, action)
    }
}
