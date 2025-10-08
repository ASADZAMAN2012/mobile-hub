/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditBackToNurseFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditCollectPhoneFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditHandToPatientFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditPhoneCollectedFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditPinLockFragmentDirections

interface PhoneCollectDestination {
    fun goToPhoneCollection(fragment: Fragment?, data: PatientCollectData)

    /**
     * This is the Ok screen
     *
     * @param fragment the current fragment
     * @param data the data to be passed
     */
    fun goToPhoneNumberProvided(fragment: Fragment?, data: PatientCollectData)

    fun goToHandDeviceToNurse(fragment: Fragment?, data: PatientCollectData)

    fun goToRePinPrompt(fragment: Fragment?, data: PatientCollectData)

    fun goBackToPatientNoCard(fragment: Fragment?, data: Map<String, Any>? = null)

    fun goToUpdatePatientFragment(fragment: Fragment?, data: PatientCollectData)
}

class PhoneCollectDestinationImpl(private val navCommons: NavCommons) : PhoneCollectDestination {
    override fun goToPhoneCollection(fragment: Fragment?, data: PatientCollectData) {
        val directions =
            PatientEditHandToPatientFragmentDirections
                .actionPatientNoCardFragmentToPatientCollectPhoneFragment(data)

        navCommons.goToFragment(fragment, directions)
    }

    override fun goToPhoneNumberProvided(fragment: Fragment?, data: PatientCollectData) {
        val directions = PatientEditCollectPhoneFragmentDirections.actionToPhoneCollected(data)
        navCommons.goToFragment(fragment, directions)
    }

    override fun goToHandDeviceToNurse(fragment: Fragment?, data: PatientCollectData) {
        val directions = PatientEditPhoneCollectedFragmentDirections.actionBackToNurse(data)
        navCommons.goToFragment(fragment, directions)
    }

    override fun goToRePinPrompt(fragment: Fragment?, data: PatientCollectData) {
        val directions = PatientEditBackToNurseFragmentDirections.actionToPinin(data)
        navCommons.goToFragment(fragment, directions)
    }

    override fun goBackToPatientNoCard(fragment: Fragment?, data: Map<String, Any>?) {
        navCommons.goBackPopTo(
            fragment,
            R.id.patientHandToPatientFragment,
            backData = data,
            inclusive = true
        )
    }

    override fun goToUpdatePatientFragment(fragment: Fragment?, data: PatientCollectData) {
        val directions =
            PatientEditPinLockFragmentDirections.actionPatientEditPinLockToUpdatePatientFragment(
                collectPhoneData = data,
                data = data.mergeUpdatePatientData()
            )

        navCommons.goToFragment(fragment, directions)
    }
}
