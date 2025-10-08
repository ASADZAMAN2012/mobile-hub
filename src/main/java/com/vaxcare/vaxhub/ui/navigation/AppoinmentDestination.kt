/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.ui.checkout.AppointmentListFragmentDirections
import java.time.LocalDate

interface AppointmentDestination {
    fun goToTemporaryClinicDialog(fragment: Fragment?, header: String)

    fun goToProcessingAppointmentDialog(fragment: Fragment?)

    fun goToAppointmentSearch(fragment: Fragment?, appointmentListDate: LocalDate?)
}

class AppointmentDestinationImpl(private val navCommons: NavCommons) : AppointmentDestination {
    override fun goToTemporaryClinicDialog(fragment: Fragment?, header: String) {
        val destination =
            AppointmentListFragmentDirections.actionAppointmentListFragmentToTemporaryClinicDialog(
                header = header
            )

        navCommons.goToFragment(fragment, destination)
    }

    override fun goToProcessingAppointmentDialog(fragment: Fragment?) {
        val destination =
            AppointmentListFragmentDirections.actionAppointmentListFragmentToProcessingAppointmentDialog()

        navCommons.goToFragment(fragment, destination)
    }

    override fun goToAppointmentSearch(fragment: Fragment?, appointmentListDate: LocalDate?) {
        val destination =
            AppointmentListFragmentDirections.actionAppointmentListFragmentToAppointmentSearchFragment(
                appointmentListDate = appointmentListDate
            )
        navCommons.goToFragment(fragment, destination)
    }
}
