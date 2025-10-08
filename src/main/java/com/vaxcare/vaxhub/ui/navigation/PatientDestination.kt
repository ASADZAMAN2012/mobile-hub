/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.UpdatePatient
import com.vaxcare.vaxhub.model.UpdatePatientData
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.ui.patient.CurbsideAddNewPatientFragmentDirections
import com.vaxcare.vaxhub.ui.patient.CurbsideConfirmPatientInfoFragmentArgs
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditDriverLicenseFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditInsuranceFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditPayerFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientScanDriverLicenseFragmentDirections

interface PatientDestination {
    fun goToKeepDevice(fragment: Fragment?)

    fun goToCaptureFrontDriverLicence(
        fragment: Fragment?,
        appointmentId: Int,
        addPatientSource: Int
    )

    fun goToCurbsideConfirmPatientInfo(fragment: Fragment?, patientId: Int)

    fun goToPatientEditPayerFromEditDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        updatePatient: UpdatePatient?
    )

    fun goToEditFrontInsurance(fragment: Fragment?)

    fun goToEditBackInsurance(fragment: Fragment?)

    fun goToPatientUpdate(fragment: Fragment?, updatePatientData: UpdatePatientData)

    fun goToPatientNoCard(
        fragment: Fragment?,
        flow: NoInsuranceCardFlow,
        appointmentId: Int,
        patientId: Int,
        data: UpdatePatientData,
        currentPhone: String?
    )

    fun goToEditDriverLicenseFromScanDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        driverLicenseInfo: DriverLicense?
    )

    fun goToPatientEditPayerFromScanDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        updatePatient: UpdatePatient?
    )

    fun goToEditInsurance(
        fragment: Fragment,
        appointmentId: Int,
        patientId: Int,
        updatePatient: UpdatePatient?,
        payer: Payer?
    )

    fun goToPatientUpdateFromEditPayer(fragment: Fragment?, updatePatientData: UpdatePatientData)
}

class PatientDestinationImpl(private val navCommons: NavCommons) : PatientDestination {
    override fun goToKeepDevice(fragment: Fragment?) {
        val directions =
            CurbsideAddNewPatientFragmentDirections.actionAddNewPatientFragmentToKeepDeviceDialog()

        navCommons.goToFragment(fragment, directions)
    }

    override fun goToCaptureFrontDriverLicence(
        fragment: Fragment?,
        appointmentId: Int,
        addPatientSource: Int
    ) {
        val directions =
            CurbsideAddNewPatientFragmentDirections.actionAddNewPatientFragmentToCaptureFrontDriverLicenseFragment(
                appointmentId = appointmentId,
                addPatientSource = addPatientSource
            )

        navCommons.goToFragment(fragment, directions)
    }

    override fun goToCurbsideConfirmPatientInfo(fragment: Fragment?, patientId: Int) {
        // navigation by destination id since Patient Search is open and override in AddNewAppointmentFragment
        navCommons.goToFragment(
            fragment,
            R.id.curbsideConfirmPatientInfoFragment,
            CurbsideConfirmPatientInfoFragmentArgs(patientId = patientId).toBundle()
        )
    }

    override fun goToPatientEditPayerFromEditDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        updatePatient: UpdatePatient?
    ) {
        val action = PatientEditDriverLicenseFragmentDirections.actionGoToPatientEditPayerFragment(
            appointmentId = appointmentId,
            patientId = patientId,
            updatePatient = updatePatient
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToEditFrontInsurance(fragment: Fragment?) {
        val action =
            PatientEditInsuranceFragmentDirections
                .actionPatientEditInsuranceFragmentToPatientEditFrontInsuranceFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun goToEditBackInsurance(fragment: Fragment?) {
        val action =
            PatientEditInsuranceFragmentDirections
                .actionPatientEditInsuranceFragmentToPatientEditBackInsuranceFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun goToPatientUpdate(fragment: Fragment?, updatePatientData: UpdatePatientData) {
        val action =
            PatientEditInsuranceFragmentDirections.actionPatientEditInsuranceFragmentToPatientUpdateFragment(
                data = updatePatientData
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToPatientNoCard(
        fragment: Fragment?,
        flow: NoInsuranceCardFlow,
        appointmentId: Int,
        patientId: Int,
        data: UpdatePatientData,
        currentPhone: String?
    ) {
        val action =
            PatientEditInsuranceFragmentDirections.actionPatientEditInsuranceFragmentToPatientNoCardFragment(
                flow = flow,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = currentPhone
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToEditDriverLicenseFromScanDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        driverLicenseInfo: DriverLicense?
    ) {
        val action =
            PatientScanDriverLicenseFragmentDirections.actionGoToPatientEditDriverLicenseFragment(
                appointmentId = appointmentId,
                driverLicense = driverLicenseInfo
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToPatientEditPayerFromScanDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        updatePatient: UpdatePatient?
    ) {
        val action =
            PatientScanDriverLicenseFragmentDirections.actionScanDriverLicenseToPatientEditPayer(
                appointmentId = appointmentId,
                patientId = patientId,
                updatePatient = updatePatient
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToEditInsurance(
        fragment: Fragment,
        appointmentId: Int,
        patientId: Int,
        updatePatient: UpdatePatient?,
        payer: Payer?
    ) {
        val action =
            PatientEditPayerFragmentDirections.actionPatientEditPayerFragmentToPatientEditInsuranceFragment(
                appointmentId = appointmentId,
                payer = payer,
                updatePatient = updatePatient,
                patientId = patientId
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToPatientUpdateFromEditPayer(fragment: Fragment?, updatePatientData: UpdatePatientData) {
        val action =
            PatientEditPayerFragmentDirections.actionPatientEditPayerFragmentToPatientUpdateFragment(
                data = updatePatientData
            )
        navCommons.goToFragment(fragment, action)
    }
}
