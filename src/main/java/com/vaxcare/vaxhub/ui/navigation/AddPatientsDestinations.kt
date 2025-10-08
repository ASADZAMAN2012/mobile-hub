/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.NavDirections
import com.vaxcare.vaxhub.ui.patient.AddNewPatientFragmentDirections
import com.vaxcare.vaxhub.ui.patient.AddNewPayerInfoFragmentDirections
import com.vaxcare.vaxhub.ui.patient.SelectPayerFragmentDirections

interface AddPatientsDestination {
    fun navigateToAddNewPatient(fragment: Fragment?)

    fun displayExitDialog(fragment: Fragment?)

    fun navigateToSelectPayer(fragment: Fragment?)

    fun navigateToAddPayerInfo(fragment: Fragment?)

    fun navigateToConfirmPatientInfoFromSelectPayer(fragment: Fragment?)

    fun navigateToConfirmPatientInfoFromAddNewPayerInfo(fragment: Fragment?)
}

class AddPatientsDestinationImpl(private val navCommons: NavCommons) : AddPatientsDestination {
    override fun navigateToAddNewPatient(fragment: Fragment?) {
        val action = NavDirections.actionGlobalAddPatient()
        navCommons.goToFragment(fragment, action)
    }

    override fun displayExitDialog(fragment: Fragment?) {
        val action = AddNewPatientFragmentDirections.actionAddNewPatientFragmentToAddNewPatientExitDialog()
        navCommons.goToFragment(fragment, action)
    }

    override fun navigateToSelectPayer(fragment: Fragment?) {
        val action = AddNewPatientFragmentDirections.actionAddNewPatientFragmentToSelectPayerFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun navigateToAddPayerInfo(fragment: Fragment?) {
        val action = SelectPayerFragmentDirections.actionSelectPayerFragmentToAddNewPayerInfoFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun navigateToConfirmPatientInfoFromSelectPayer(fragment: Fragment?) {
        val action = SelectPayerFragmentDirections.actionSelectPayerFragmentToConfirmPatientInfoFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun navigateToConfirmPatientInfoFromAddNewPayerInfo(fragment: Fragment?) {
        val action = AddNewPayerInfoFragmentDirections.actionAddNewPayerInfoFragmentToConfirmPatientInfoFragment()
        navCommons.goToFragment(fragment, action)
    }
}
