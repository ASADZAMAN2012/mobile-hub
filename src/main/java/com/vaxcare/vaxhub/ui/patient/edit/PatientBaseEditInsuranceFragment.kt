/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.camera.view.PreviewView
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.model.checkout.InsuranceCardCaptureMetric
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.view.VaxToolbar
import com.vaxcare.vaxhub.databinding.FragmentBaseEditInsuranceBinding
import com.vaxcare.vaxhub.model.AppointmentMediaType
import com.vaxcare.vaxhub.ui.patient.BaseCameraFragment
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.File

abstract class PatientBaseEditInsuranceFragment :
    BaseCameraFragment<FragmentBaseEditInsuranceBinding>() {
    private var url: String? = null
    private var retryPhoto: Boolean = false

    abstract val picasso: Picasso
    abstract val appointmentMediaType: AppointmentMediaType
    abstract val title: String
    abstract val subTitle: String
    abstract val captureTitle: String

    abstract fun onActionNext(result: PatientEditInsuranceResult)

    override val verifyPhoto: Boolean
        get() = this.url == null && appointmentMediaType == AppointmentMediaType.INSURANCE_CARD_FRONT

    override val captureView: View?
        get() = binding?.viewCameraCapture

    override val previewView: PreviewView?
        get() = binding?.previewView

    override val captureButton: View?
        get() = binding?.cameraCaptureButton

    override val topBar: VaxToolbar?
        get() = binding?.topBar

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_base_edit_insurance,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentBaseEditInsuranceBinding =
        FragmentBaseEditInsuranceBinding.bind(container)

    override fun onStartTakePhoto() {
        super.onStartTakePhoto()
        binding?.viewCameraPreview?.hide()
        binding?.cameraCaptureButton?.hide()
    }

    override fun onTakePhoto(url: String, verify: Boolean) {
        super.onTakePhoto(url, verify)

        if (verify) {
            this.url = url
            // we cannot rotate the image here - strange condition where the process hangs
            picasso.load(url).into(binding?.viewCameraPreview)
            binding?.captureHoldCard?.hide()
            binding?.viewCameraPreview?.show()
            binding?.photoCheckProgressBar?.show()
        } else {
            onActionNext(PatientEditInsuranceResult(url, retryPhoto))
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
            onActionNext(PatientEditInsuranceResult(url, retryPhoto))
        } else {
            deleteCurrentUrl()
            retryPhoto = true
            binding?.cancelCaptureButton?.setOnSingleClickListener { activity?.onBackPressed() }
            binding?.viewCameraPreview?.hide()
            binding?.photoCheckProgressBar?.hide()
            binding?.cameraCaptureButton?.show()
            binding?.captureTitle?.apply {
                text = getString(R.string.checking_the_photo_failed_insurance)
                setTypeface(typeface, Typeface.BOLD)
            }
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
        binding?.cancelCaptureButton?.setOnSingleClickListener { activity?.onBackPressed() }
    }

    override fun onDestroyView() {
        this.url = null
        super.onDestroyView()
    }

    private fun deleteCurrentUrl() {
        this.url?.let {
            try {
                val deleted = File(requireContext().cacheDir.absolutePath, File(it).name).delete()
                Timber.i("Previous photo deleted: $deleted")
            } catch (e: NullPointerException) {
                Timber.e(e, "An error occurred when trying to delete the previous photo")
            }
        }
    }

    /**
     * Used to return back the result of this interaction
     *
     * @property url the url of the picture
     * @property retry if the user requires to retry/retake the picture or not. default = false
     */
    @Parcelize
    data class PatientEditInsuranceResult(val url: String, val retry: Boolean = false) : Parcelable
}
