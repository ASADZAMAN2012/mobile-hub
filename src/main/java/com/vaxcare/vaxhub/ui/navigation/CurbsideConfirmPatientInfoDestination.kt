/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.ui.patient.CurbsideConfirmPatientInfoFragmentDirections

interface CurbsideConfirmPatientInfoDestination {
    fun goToCurbsideCreateAppointment(
        fragment: Fragment,
        patientId: Int = -1,
        providerId: Int = -1
    )
}

class CurbsideConfirmPatientInfoDestinationImpl(
    private val navCommons: NavCommons
) : CurbsideConfirmPatientInfoDestination {
    override fun goToCurbsideCreateAppointment(
        fragment: Fragment,
        patientId: Int,
        providerId: Int
    ) {
        val destination =
            CurbsideConfirmPatientInfoFragmentDirections
                .actionCurbsideConfirmPatientInfoToCurbsideCreateAppointmentFragment(
                    patientId = patientId,
                    providerId = providerId
                )
        navCommons.goToFragment(fragment, destination)
    }
}
