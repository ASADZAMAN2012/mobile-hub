/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.span.FontSpan
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.model.AppointmentMediaType
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CaptureFrontDriverLicenseFragment : BaseCaptureFragment() {
    @Inject @MobilePicasso override lateinit var picasso: Picasso

    @Inject override lateinit var scannerManager: ScannerManager

    @Inject override lateinit var destination: CaptureFlowDestination

    @Inject override lateinit var globalDestinations: GlobalDestinations

    @Inject @MHAnalyticReport override lateinit var analytics: AnalyticReport

    private val args: CaptureFrontDriverLicenseFragmentArgs by navArgs()

    override val appointmentMediaType: AppointmentMediaType
        get() = AppointmentMediaType.DRIVERS_LICENSE_FRONT

    override val verifyPhoto: Boolean
        get() = false

    override val title: String
        get() = resources.getString(R.string.patient_add_title)

    override val subTitle: String
        get() = resources.getString(R.string.patient_add_capture_driver_license)

    override val skipTitle: String
        get() = resources.getString(R.string.patient_add_capture_no_license)

    override val captureTitle: SpannableStringBuilder
        get() {
            val value = resources.getString(R.string.patient_add_capture_front_driver_license)
            val spannable = SpannableStringBuilder(value)
            val semiBold = FontSpan(resources.getFont(R.font.graphik_semi_bold))
            val highLightLabel = "front"
            spannable.setSpan(
                semiBold,
                value.indexOf(highLightLabel),
                value.indexOf(highLightLabel) + highLightLabel.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            return spannable
        }

    override fun onActionNext(url: String) {
        destination.goToCaptureViewerFrontDriverLicense(
            fragment = this@CaptureFrontDriverLicenseFragment,
            appointmentId = args.appointmentId,
            addPatientSource = args.addPatientSource,
            url = url
        )
    }

    override fun skip() {
        checkIfParentPatient {
            destination.goToCurbsideSelectPayer(this@CaptureFrontDriverLicenseFragment)
        }
    }
}
