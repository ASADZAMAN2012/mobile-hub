/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.ui.checkout.AddAppointmentFragmentDirections

interface AddAppointmentDestination {
    fun goToCurbsideConfirmPatient(fragment: Fragment?, patientId: Int)
}

class AddAppointmentDestinationImpl(
    private val navCommons: NavCommons
) : AddAppointmentDestination {
    override fun goToCurbsideConfirmPatient(fragment: Fragment?, patientId: Int) {
        val destination =
            AddAppointmentFragmentDirections.actionAddAppointmentFragmentToCurbsideConfirmPatientInfoFragment(
                patientId = patientId
            )
        navCommons.goToFragment(fragment, destination)
    }
}
