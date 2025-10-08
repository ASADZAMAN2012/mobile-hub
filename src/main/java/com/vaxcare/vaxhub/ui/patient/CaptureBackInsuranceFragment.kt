/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.View
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
class CaptureBackInsuranceFragment : BaseCaptureFragment() {
    @Inject @MobilePicasso override lateinit var picasso: Picasso

    @Inject override lateinit var scannerManager: ScannerManager

    @Inject override lateinit var destination: CaptureFlowDestination

    @Inject override lateinit var globalDestinations: GlobalDestinations

    @Inject @MHAnalyticReport override lateinit var analytics: AnalyticReport

    override val appointmentMediaType: AppointmentMediaType
        get() = AppointmentMediaType.INSURANCE_CARD_BACK

    override val title: String
        get() = resources.getString(R.string.patient_add_title)

    override val subTitle: String
        get() = resources.getString(R.string.patient_add_capture_insurance_card)

    override val skipTitle: String
        get() = ""

    override val captureTitle: SpannableStringBuilder
        get() {
            val value = resources.getString(R.string.patient_add_capture_back_insurance_card)
            val spannable = SpannableStringBuilder(value)
            val semiBold = FontSpan(resources.getFont(R.font.graphik_semi_bold))
            val highLightLabel = "back"
            spannable.setSpan(
                semiBold,
                value.indexOf(highLightLabel),
                value.indexOf(highLightLabel) + highLightLabel.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            return spannable
        }

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        binding?.captureNotCatch?.visibility = View.GONE
    }

    override fun onActionNext(url: String) {
        destination.goToCaptureViewerBackInsurance(this@CaptureBackInsuranceFragment, url)
    }

    override fun skip() {
        // No skip on capture insurance back screen
    }
}
