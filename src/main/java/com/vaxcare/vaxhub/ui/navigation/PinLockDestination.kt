/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.ui.checkout.AppointmentListFragment
import com.vaxcare.vaxhub.ui.login.PinLockFragmentDirections
import java.time.LocalDate

interface PinLockDestination {
    fun goBackToSplash(fragment: Fragment?)

    fun goToAppointmentList(fragment: Fragment?, lookupDate: LocalDate? = null)
}

class PinLockDestinationImpl(private val navCommons: NavCommons) : PinLockDestination {
    override fun goBackToSplash(fragment: Fragment?) = navCommons.goBack(fragment)

    override fun goToAppointmentList(fragment: Fragment?, lookupDate: LocalDate?) {
        val direction = PinLockFragmentDirections.actionPinLockFragmentToAppointmentListFragment()
        val passDate = lookupDate ?: LocalDate.now()
        val data = mapOf<String, Any?>(AppointmentListFragment.APPOINTMENT_DATE to passDate)
        navCommons.goToFragment(fragment, direction, data)
    }
}
