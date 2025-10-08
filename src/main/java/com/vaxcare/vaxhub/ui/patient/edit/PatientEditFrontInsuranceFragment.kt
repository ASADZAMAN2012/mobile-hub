/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.model.AppointmentMediaType
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PatientEditFrontInsuranceFragment : PatientBaseEditInsuranceFragment() {
    private val screenTitle = "CaptureInsuranceCard"

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var scannerManager: ScannerManager

    @Inject @MobilePicasso
    override lateinit var picasso: Picasso

    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    @Inject
    override lateinit var destination: CaptureFlowDestination

    override val appointmentMediaType: AppointmentMediaType
        get() = AppointmentMediaType.INSURANCE_CARD_FRONT

    override val title: String
        get() = resources.getString(R.string.patient_edit_title)

    override val subTitle: String
        get() = resources.getString(R.string.patient_edit_insurance_title)

    override val captureTitle: String
        get() = resources.getString(R.string.patient_edit_front_card_subtitle)

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        logScreenNavigation(screenTitle)
    }

    override fun onActionNext(result: PatientEditInsuranceResult) {
        globalDestinations.goBack(
            this@PatientEditFrontInsuranceFragment,
            mapOf(
                BaseEditInsuranceFragment.FRONT_INSURANCE to
                    result
            )
        )
    }
}
