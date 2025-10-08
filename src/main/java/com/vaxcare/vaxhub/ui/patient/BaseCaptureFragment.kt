/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.camera.view.PreviewView
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.model.checkout.InsuranceCardCaptureMetric
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.view.VaxToolbar
import com.vaxcare.vaxhub.databinding.FragmentBaseCaptureBinding
import com.vaxcare.vaxhub.model.AppointmentMediaType
import java.io.File

abstract class BaseCaptureFragment : BaseCameraFragment<FragmentBaseCaptureBinding>() {
    private var url: String? = null

    abstract val picasso: Picasso
    abstract val appointmentMediaType: AppointmentMediaType
    abstract val title: String
    abstract val subTitle: String
    abstract val skipTitle: String
    abstract val captureTitle: SpannableStringBuilder

    abstract fun onActionNext(url: String)

    abstract fun skip()

    override val verifyPhoto: Boolean
        get() = this.url == null && appointmentMediaType == AppointmentMediaType.INSURANCE_CARD_FRONT

    override val captureView: View
        get() = requireNotNull(view).findViewById(R.id.view_camera_capture)

    override val previewView: PreviewView
        get() = requireNotNull(view).findViewById(R.id.view_finder)

    override val captureButton: View
        get() = requireNotNull(view).findViewById(R.id.camera_capture_button)

    override val topBar: VaxToolbar
        get() = requireNotNull(view).findViewById(R.id.top_bar)

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_base_capture,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentBaseCaptureBinding = FragmentBaseCaptureBinding.bind(container)

    override fun onStartTakePhoto() {
        super.onStartTakePhoto()
        this.url?.let {
            try {
                File(it).delete()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
        binding?.viewCameraPreview?.hide()
        binding?.cameraCaptureButton?.hide()
    }

    override fun onTakePhoto(url: String, verify: Boolean) {
        super.onTakePhoto(url, verify)

        if (verify) {
            this.url = url
            picasso.load(url).into(binding?.viewCameraPreview)
            binding?.viewCameraPreview?.show()
            binding?.photoCheckLabel?.show()
            binding?.photoCheckProgressBar?.show()
            binding?.photoCheckLabel?.text = getString(R.string.checking_the_photo)
        } else {
            onActionNext(url)
        }
    }

    override fun onEndVerifyPhoto(
        url: String,
        success: Boolean,
        captureText: String?
    ) {
        super.onEndVerifyPhoto(url, success, captureText)

        analytics.saveMetric(
            InsuranceCardCaptureMetric(
                appointmentViewModel.currentCheckout.selectedAppointment?.id,
                success,
                captureText?.length ?: 0
            )
        )

        if (success) {
            onActionNext(url)
        } else {
            binding?.viewCameraPreview?.hide()
            binding?.photoCheckLabel?.show()
            binding?.photoCheckProgressBar?.hide()
            binding?.cameraCaptureButton?.show()
            binding?.photoCheckLabel?.text = getString(R.string.checking_the_photo_failed)
        }
    }

    override fun onTakePhotoFailed() {
        super.onTakePhotoFailed()

        binding?.viewCameraPreview?.hide()
        binding?.cameraCaptureButton?.show()
        binding?.photoCheckProgressBar?.hide()
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)

        binding?.topBar?.setTitle(title)
        binding?.topBar?.setSubTitle(subTitle)
        binding?.captureTitle?.text = captureTitle
        binding?.captureNotCatch?.text = skipTitle

        binding?.captureNotCatch?.setOnClickListener {
            skip()
        }
    }

    override fun onDestroyView() {
        this.url = null
        super.onDestroyView()
    }
}
