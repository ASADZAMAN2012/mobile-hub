/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.NavController

class BackNavigationHelper(private val navController: NavController) {
    /**
     * Resolve fragment handleBack (if defined) or navigate up
     *
     * @param fragment
     */
    fun handleBackPressed(fragment: Fragment?) =
        when (fragment) {
            is BackNavigationHandler -> !fragment.handleBack() && navController.navigateUp()
            else -> navController.navigateUp()
        }
}
