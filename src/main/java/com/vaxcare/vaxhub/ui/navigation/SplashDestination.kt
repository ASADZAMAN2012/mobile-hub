/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.vaxhub.ui.login.PinLockAction
import com.vaxcare.vaxhub.ui.login.SplashFragmentDirections
import java.time.LocalDate

interface SplashDestination {
    fun goToPinLock(
        fragment: Fragment?,
        pinLockAction: PinLockAction,
        selectedDate: LocalDate? = null
    )

    fun goToSSOLogin(fragment: Fragment?, selectedDate: LocalDate? = null)

    fun goToOutOfDate(
        fragment: Fragment?,
        severity: UpdateSeverity,
        partnerName: String,
        clinicName: String
    )
}

class SplashDestinationImpl(private val navCommons: NavCommons) : SplashDestination {
    override fun goToPinLock(
        fragment: Fragment?,
        pinLockAction: PinLockAction,
        selectedDate: LocalDate?
    ) {
        val direction =
            SplashFragmentDirections.actionSplashFragmentToPinLockFragment(
                pinLockAction = pinLockAction,
                appointmentListDate = selectedDate
            )

        navCommons.goToFragment(fragment, direction)
    }

    override fun goToSSOLogin(fragment: Fragment?, selectedDate: LocalDate?) {
        val direction = SplashFragmentDirections.actionSplashFragmentToLoginFragment(selectedDate)
        navCommons.goToFragment(fragment, direction)
    }

    override fun goToOutOfDate(
        fragment: Fragment?,
        severity: UpdateSeverity,
        partnerName: String,
        clinicName: String
    ) {
        val direction = SplashFragmentDirections.actionSplashFragmentToOutOfDateFragment(
            severity = severity,
            partnerName = partnerName,
            clinicName = clinicName
        )

        navCommons.goToFragment(fragment, direction)
    }
}
