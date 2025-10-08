/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CaptureViewerFrontDriverLicenseFragment : BaseCaptureViewerFragment() {
    @Inject @MobilePicasso override lateinit var picasso: Picasso

    @Inject override lateinit var destination: CaptureFlowDestination

    @Inject override lateinit var globalDestinations: GlobalDestinations

    @Inject @MHAnalyticReport override lateinit var analytics: AnalyticReport

    override val title: String
        get() = resources.getString(R.string.patient_add_title)

    override val subTitle: String
        get() = resources.getString(R.string.patient_add_capture_driver_license)

    override fun actionNext(path: String) {
        appointmentViewModel.appointmentCreation.driverLicenseFrontPath = path

        checkIfParentPatient {
            destination.goToCurbsideSelectPayerFromViewer(this@CaptureViewerFrontDriverLicenseFragment)
        }
    }

    override fun actionCancel() {
        globalDestinations.goBack(this@CaptureViewerFrontDriverLicenseFragment)
    }
}
