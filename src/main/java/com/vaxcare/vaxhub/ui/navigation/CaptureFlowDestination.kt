/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.ui.patient.CaptureBackInsuranceFragmentDirections
import com.vaxcare.vaxhub.ui.patient.CaptureFrontDriverLicenseFragmentDirections
import com.vaxcare.vaxhub.ui.patient.CaptureFrontInsuranceFragmentDirections
import com.vaxcare.vaxhub.ui.patient.CaptureViewerFrontDriverLicenseFragmentDirections
import com.vaxcare.vaxhub.ui.patient.CaptureViewerFrontInsuranceFragmentDirections
import com.vaxcare.vaxhub.ui.patient.CurbsideSelectPayerFragmentDirections

interface CaptureFlowDestination {
    fun goToSameInsuranceDialog(fragment: Fragment?)

    fun goToNoInsuranceFragment(
        fragment: Fragment?,
        flow: NoInsuranceCardFlow,
        appointmentId: Int = -1,
        patientId: Int = -1,
        currentPhone: String? = null
    )

    fun goToSelectOtherPayer(fragment: Fragment?)

    fun goToCaptureFrontInsurance(fragment: Fragment?)

    fun goToCaptureViewerFrontInsurance(fragment: Fragment?, url: String)

    fun goToCurbsideConfirmPatientInfo(fragment: Fragment)

    fun goToCaptureViewerBackInsurance(fragment: Fragment?, url: String)

    fun goToCurbsideSelectPayer(fragment: Fragment?)

    fun goToCurbsideSelectPayerFromViewer(fragment: Fragment?)

    fun goToCaptureBackInsurance(fragment: Fragment?)

    fun goToCaptureViewerFrontDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        addPatientSource: Int,
        url: String
    )
}

class CaptureFlowDestinationImpl(private val navCommons: NavCommons) : CaptureFlowDestination {
    override fun goToSameInsuranceDialog(fragment: Fragment?) {
        navCommons.goToFragment(fragment, R.id.sameInsuranceDialog)
    }

    override fun goToNoInsuranceFragment(
        fragment: Fragment?,
        flow: NoInsuranceCardFlow,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    ) {
        val directions =
            CaptureFrontInsuranceFragmentDirections.actionCaptureFrontInsuranceFragmentToPatientNoCardFragment(
                flow = flow,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = currentPhone
            )
        navCommons.goToFragment(fragment, directions)
    }

    override fun goToCaptureViewerFrontInsurance(fragment: Fragment?, url: String) {
        val action = CaptureFrontInsuranceFragmentDirections
            .actionCaptureDriverLicenseFragmentToCaptureViewerFrontInsuranceFragment(
                capturePhotoUrl = url
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCurbsideConfirmPatientInfo(fragment: Fragment) {
        navCommons.goToFragment(fragment, R.id.curbsideConfirmPatientInfoFragment)
    }

    override fun goToCaptureViewerBackInsurance(fragment: Fragment?, url: String) {
        val action = CaptureBackInsuranceFragmentDirections
            .actionCaptureBackInsuranceFragmentToCaptureViewerBackInsuranceFragment(
                capturePhotoUrl = url
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCurbsideSelectPayer(fragment: Fragment?) {
        val action = CaptureFrontDriverLicenseFragmentDirections
            .actionCaptureDriverLicenseFragmentToCurbsideSelectPayerFragment()

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCurbsideSelectPayerFromViewer(fragment: Fragment?) {
        val action = CaptureViewerFrontDriverLicenseFragmentDirections
            .actionCaptureViewerDriverLicenseFragmentToCurbsideSelectPayerFragment()

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCaptureBackInsurance(fragment: Fragment?) {
        val action = CaptureViewerFrontInsuranceFragmentDirections
            .actionCaptureViewerFrontInsuranceFragmentToCaptureBackInsuranceFragment()

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCaptureViewerFrontDriverLicense(
        fragment: Fragment?,
        appointmentId: Int,
        addPatientSource: Int,
        url: String
    ) {
        val action =
            CaptureFrontDriverLicenseFragmentDirections.actionGoToCaptureViewerFrontDriverLicenseFragment(
                appointmentId = appointmentId,
                addPatientSource = addPatientSource,
                capturePhotoUrl = url
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToSelectOtherPayer(fragment: Fragment?) {
        val action = CurbsideSelectPayerFragmentDirections
            .actionCurbsideSelectPayerFragmentToSelectOtherPayerDialog()

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCaptureFrontInsurance(fragment: Fragment?) {
        val action = CurbsideSelectPayerFragmentDirections
            .actionCurbsideSelectPayerFragmentToCaptureFrontInsuranceFragment()

        navCommons.goToFragment(fragment, action)
    }
}
