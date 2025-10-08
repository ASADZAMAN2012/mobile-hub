/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import java.time.LocalDate

interface CreateAppointmentDestination {
    fun goBackToAddAppointment(fragment: Fragment, date: LocalDate?)
}

class CreateAppointmentDestinationImpl(
    private val navCommons: NavCommons
) : CreateAppointmentDestination {
    override fun goBackToAddAppointment(fragment: Fragment, date: LocalDate?) {
        val data = mapOf<String, Any?>(
            "appointmentListDate" to date,
            "isOffline" to true
        )
        val popped = navCommons.goBackPopTo(fragment, R.id.addAppointmentFragment, data)
        if (!popped) {
            navCommons.goBack(fragment)
        }
    }
}
