/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.ui.admin.AdminDetailsFragmentDirections
import com.vaxcare.vaxhub.ui.admin.AdminLoginFragmentDirections

interface AdminDestination {
    fun goBackToSplash(fragment: Fragment?)

    fun goFromDetailsToEnterID(fragment: Fragment?, typeName: String)

    fun goFromDetailsToSetup(fragment: Fragment?)

    fun goToValidateLicense(fragment: Fragment?)

    fun goToAdminDetail(fragment: Fragment?)

    fun goBack(fragment: Fragment?, backData: Map<String, Any>? = null)
}

class AdminDestinationImpl(private val navCommons: NavCommons) : AdminDestination {
    override fun goBackToSplash(fragment: Fragment?) {
        navCommons.goBackPopTo(fragment, R.id.splashFragment)
    }

    override fun goFromDetailsToEnterID(fragment: Fragment?, typeName: String) {
        val direction =
            AdminDetailsFragmentDirections.actionAdminDetailsFragmentToEnterIDFragment(
                type = typeName
            )

        navCommons.goToFragment(fragment, direction)
    }

    override fun goFromDetailsToSetup(fragment: Fragment?) {
        val direction =
            AdminDetailsFragmentDirections.actionAdminDetailsFragmentToAdminSetupOverlay()

        navCommons.goToFragment(fragment, direction)
    }

    override fun goToValidateLicense(fragment: Fragment?) {
        val direction =
            AdminLoginFragmentDirections.actionAdminLoginFragmentToAdminValidateLicenseDialog()

        navCommons.goToFragment(fragment, direction)
    }

    override fun goToAdminDetail(fragment: Fragment?) {
        val direction =
            AdminLoginFragmentDirections.actionAdminLoginFragmentToAdminDetailsFragment()

        navCommons.goToFragment(fragment, direction)
    }

    override fun goBack(fragment: Fragment?, backData: Map<String, Any>?) {
        navCommons.goBack(fragment, backData)
    }
}
