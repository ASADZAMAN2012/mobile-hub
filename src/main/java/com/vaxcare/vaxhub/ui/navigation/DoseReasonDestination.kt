/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.model.patient.InvalidInfoWrapper
import com.vaxcare.vaxhub.ui.checkout.DoseReasonFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditHandToPatientFragmentArgs

interface DoseReasonDestination {
    fun popBackToCheckout(fragment: Fragment?): Boolean

    fun toCollectDemo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper)

    fun toNewPayerScreen(fragment: Fragment?, infoWrapper: InvalidInfoWrapper)

    fun toSelectPayerScreen(fragment: Fragment?, infoWrapper: InvalidInfoWrapper)

    fun toScanInsuranceScreen(fragment: Fragment?)

    fun toInsurancePhoneCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    )
}

class DoseReasonDestinationImpl(private val navCommons: NavCommons) : DoseReasonDestination {
    override fun popBackToCheckout(fragment: Fragment?): Boolean {
        return navCommons.goBackPopTo(fragment, R.id.checkoutPatientFragment)
    }

    override fun toCollectDemo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper) {
        val action = DoseReasonFragmentDirections.actionDoseReasonToCollectDemoInfoFragment(
            infoWrapper = infoWrapper
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun toNewPayerScreen(fragment: Fragment?, infoWrapper: InvalidInfoWrapper) {
        val action = DoseReasonFragmentDirections.actionDoseReasonToCollectPayerInfoFragment(
            infoWrapper = infoWrapper
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun toSelectPayerScreen(fragment: Fragment?, infoWrapper: InvalidInfoWrapper) {
        val action =
            DoseReasonFragmentDirections.actionDoseReasonToCollectPayerFragment(infoWrapper)
        navCommons.goToFragment(fragment, action)
    }

    override fun toScanInsuranceScreen(fragment: Fragment?) {
        val action = DoseReasonFragmentDirections.actionDoseReasonToCollectInsuranceFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun toInsurancePhoneCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    ) {
        navCommons.goToFragment(
            fragment,
            R.id.patientHandToPatientFragment,
            PatientEditHandToPatientFragmentArgs(
                flow = NoInsuranceCardFlow.CHECKOUT_PATIENT,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = currentPhone
            ).toBundle()
        )
    }
}
