/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment

interface PatientEditDestination {
    fun goToDestination(fragment: Fragment, destId: Int)
}

class PatientEditDestinationImpl(private val navCommons: NavCommons) : PatientEditDestination {
    override fun goToDestination(fragment: Fragment, destId: Int) {
        navCommons.goToFragment(fragment, destId)
    }
}
