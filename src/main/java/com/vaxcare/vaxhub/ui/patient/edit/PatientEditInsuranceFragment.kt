/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.view.View
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.isInsurancePhoneCaptureDisabled
import com.vaxcare.vaxhub.core.extension.safeLet
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentEditPatientInsuranceBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.model.UpdatePatientData
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.ui.navigation.PatientDestination
import com.vaxcare.vaxhub.ui.navigation.PatientEditDestination
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PatientEditInsuranceFragment : BaseEditInsuranceFragment() {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    @MobilePicasso
    override lateinit var picasso: Picasso

    @Inject
    override lateinit var baseDestination: PatientEditDestination

    @Inject
    lateinit var destination: PatientDestination

    private val args: PatientEditInsuranceFragmentArgs by navArgs()

    override val frontInsuranceFragmentId: Int = R.id.patientEditFrontInsuranceFragment
    override val backInsuranceFragmentId: Int = R.id.patientEditBackInsuranceFragment

    override fun onNoInsuranceCardClicked() {
        val updatePatientData = UpdatePatientData(
            appointmentId = args.appointmentId,
            payer = args.payer,
            frontInsurancePath = null,
            backInsurancePath = null,
            updatePatient = args.updatePatient,
            retriedPhoto = retriedPhoto
        )

        if (isAbleToNavigateToPhoneCaptureFlow()) {
            destination.goToPatientNoCard(
                fragment = this@PatientEditInsuranceFragment,
                flow = NoInsuranceCardFlow.EDIT_PATIENT,
                appointmentId = args.appointmentId,
                patientId = args.patientId,
                data = buildUpdatePatientData(null, null),
                currentPhone = args.updatePatient?.phoneNumber
            )
        } else {
            destination.goToPatientUpdate(
                this@PatientEditInsuranceFragment,
                updatePatientData
            )
        }
    }

    private fun isAbleToNavigateToPhoneCaptureFlow(): Boolean {
        val featureFlags = locationViewModel.getLocation().value?.activeFeatureFlags ?: emptyList()

        return !featureFlags.isInsurancePhoneCaptureDisabled()
    }

    override fun onNavigateNext() {
        destination.goToPatientUpdate(
            this@PatientEditInsuranceFragment,
            buildUpdatePatientData(frontCardUrl, backCardUrl)
        )
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_edit_patient_insurance,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentEditPatientInsuranceBinding =
        FragmentEditPatientInsuranceBinding.bind(container)

    override fun buildUpdatePatientData(frontCardUrl: String?, backCardUrl: String?) =
        safeLet(frontCardUrl, backCardUrl) { frontCard, backCard ->
            UpdatePatientData(
                appointmentId = args.appointmentId,
                payer = args.payer,
                frontInsurancePath = File(
                    requireContext().cacheDir.absolutePath,
                    File(frontCard).name
                ).absolutePath,
                backInsurancePath = File(
                    requireContext().cacheDir.absolutePath,
                    File(backCard).name
                ).absolutePath,
                updatePatient = args.updatePatient,
                retriedPhoto = retriedPhoto
            )
        } ?: UpdatePatientData(
            appointmentId = args.appointmentId,
            payer = args.payer,
            updatePatient = args.updatePatient,
            retriedPhoto = retriedPhoto
        )
}
