/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import android.app.Activity
import com.vaxcare.vaxhub.R

interface MainDestination {
    fun goToAdminLogin(activity: Activity?)

    fun lockScreen(activity: Activity?)
}

class MainDestinationImpl(private val navCommons: NavCommons) : MainDestination {
    override fun goToAdminLogin(activity: Activity?) = navCommons.goToFragment(activity, R.id.adminLoginFragment)

    override fun lockScreen(activity: Activity?) {
        navCommons.goBackPopTo(activity = activity, destinationId = R.id.splashFragment)
    }
}
