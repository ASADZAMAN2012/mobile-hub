/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.ui.checkout.AppointmentSearchFragmentDirections
import java.time.LocalDate

interface AppointmentSearchDestination {
    fun goToAddAppointment(fragment: Fragment?, appointmentListDate: LocalDate?)
}

class AppointmentSearchDestinationImpl(
    private val navCommons: NavCommons
) : AppointmentSearchDestination {
    override fun goToAddAppointment(fragment: Fragment?, appointmentListDate: LocalDate?) {
        val destination =
            AppointmentSearchFragmentDirections.actionAppointmentSearchFragmentToAddAppointmentFragment(
                appointmentListDate = appointmentListDate
            )
        navCommons.goToFragment(fragment, destination)
    }
}
