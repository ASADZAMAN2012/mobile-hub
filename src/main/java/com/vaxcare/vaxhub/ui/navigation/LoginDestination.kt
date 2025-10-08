/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.NavDirections
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.login.LoginOptions
import com.vaxcare.vaxhub.ui.checkout.AppointmentListFragment
import com.vaxcare.vaxhub.ui.login.sso.LoginFragmentDirections
import com.vaxcare.vaxhub.ui.login.sso.LoginPinFragmentDirections
import java.time.LocalDate

interface LoginDestination {
    fun goBackToSplash(fragment: Fragment?)

    fun goToPinLogin(fragment: Fragment?, username: String)

    fun goToAppointmentList(fragment: Fragment?, lookupDate: LocalDate? = null)

    fun goToResetPinConfirmFromPinFragment(
        fragment: Fragment?,
        username: String,
        isSuccessful: Boolean
    )
}

class LoginDestinationImpl(private val navCommons: NavCommons) : LoginDestination {
    override fun goBackToSplash(fragment: Fragment?) {
        navCommons.goBackPopTo(fragment, R.id.splashFragment)
    }

    override fun goToPinLogin(fragment: Fragment?, username: String) {
        val direction = NavDirections.actionGlobalLoginPinFragment(LoginOptions(username))
        navCommons.goToFragment(fragment, direction)
    }

    override fun goToAppointmentList(fragment: Fragment?, lookupDate: LocalDate?) {
        val direction = LoginFragmentDirections.actionLoginFragmentToAppointmentListFragment()
        val passDate = lookupDate ?: LocalDate.now()
        val data = mapOf<String, Any?>(AppointmentListFragment.APPOINTMENT_DATE to passDate)
        navCommons.goToFragment(fragment, direction, data)
    }

    override fun goToResetPinConfirmFromPinFragment(
        fragment: Fragment?,
        username: String,
        isSuccessful: Boolean
    ) {
        val direction = LoginPinFragmentDirections
            .actionLoginPinFragmentToResetPinConfirmFragment(username, isSuccessful)
        navCommons.goToFragment(fragment, direction)
    }
}
