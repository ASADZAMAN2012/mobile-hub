/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.squareup.picasso.Picasso
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentEditPatientInsuranceBinding
import com.vaxcare.vaxhub.model.UpdatePatientData
import com.vaxcare.vaxhub.ui.navigation.PatientEditDestination
import com.vaxcare.vaxhub.ui.patient.edit.PatientBaseEditInsuranceFragment.PatientEditInsuranceResult
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import timber.log.Timber
import java.io.File

abstract class BaseEditInsuranceFragment : BaseFragment<FragmentEditPatientInsuranceBinding>() {
    companion object {
        const val BACK_INSURANCE = "back_insurance"
        const val FRONT_INSURANCE = "front_insurance"
        const val NO_CARD_FLOW = "checkout_collect_no_insurance_card"
    }

    protected abstract val baseDestination: PatientEditDestination
    protected abstract val picasso: Picasso

    protected var frontCardUrl: String? = null
    protected var backCardUrl: String? = null
    protected var retriedPhoto: Boolean = false
    protected val appointmentViewModel: AppointmentViewModel by activityViewModels()

    protected abstract val frontInsuranceFragmentId: Int
    protected abstract val backInsuranceFragmentId: Int

    protected abstract fun onNoInsuranceCardClicked()

    protected abstract fun onNavigateNext()

    protected open fun buildUpdatePatientData(frontCardUrl: String?, backCardUrl: String?): UpdatePatientData =
        UpdatePatientData(0)

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_edit_patient_insurance,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentEditPatientInsuranceBinding =
        FragmentEditPatientInsuranceBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.topBar?.setTitle(resources.getString(R.string.patient_edit_insurance_title_current))
        binding?.topBar?.onCloseAction = {
            clearCachedFiles()
            activity?.onBackPressed()
        }

        binding?.patientEditFrontCardCaptureView?.setOnSingleClickListener {
            baseDestination.goToDestination(this, frontInsuranceFragmentId)
        }
        binding?.patientEditFrontCardView?.setOnSingleClickListener {
            baseDestination.goToDestination(this, frontInsuranceFragmentId)
        }
        binding?.patientEditBackCardCaptureView?.setOnSingleClickListener {
            baseDestination.goToDestination(this, backInsuranceFragmentId)
        }
        binding?.patientEditBackCardView?.setOnSingleClickListener {
            baseDestination.goToDestination(this, backInsuranceFragmentId)
        }

        getResultLiveData<PatientEditInsuranceResult>(FRONT_INSURANCE)
            ?.observe(viewLifecycleOwner) { result ->
                frontCardUrl?.let {
                    if (it != result.url) {
                        deleteFile(it)
                    }
                }
                frontCardUrl = result?.url
                retriedPhoto = retriedPhoto || result?.retry ?: false
                toggleUI()
                if (result != null) {
                    binding?.patientEditFrontCardCaptureView?.hide()
                    binding?.patientEditFrontCardView?.show()
                    picasso.load(result.url)
                        .rotate(-90f)
                        .into(binding?.patientEditFrontCard)
                } else {
                    binding?.patientEditFrontCardCaptureView?.show()
                    binding?.patientEditFrontCardView?.hide()
                }
            }

        getResultLiveData<PatientEditInsuranceResult>(BACK_INSURANCE)
            ?.observe(viewLifecycleOwner) { result ->
                backCardUrl?.let {
                    if (it != result.url) {
                        deleteFile(it)
                    }
                }
                backCardUrl = result?.url
                retriedPhoto = retriedPhoto || result?.retry ?: false
                toggleUI()
                if (result != null) {
                    binding?.patientEditBackCardCaptureView?.hide()
                    binding?.patientEditBackCardView?.show()
                    picasso.load(result.url)
                        .rotate(-90f)
                        .into(binding?.patientEditBackCard)
                } else {
                    binding?.patientEditBackCardCaptureView?.show()
                    binding?.patientEditBackCardView?.hide()
                }
            }

        binding?.patientEditNoInsuranceCard?.paint?.flags = Paint.UNDERLINE_TEXT_FLAG
        binding?.patientEditNoInsuranceCard?.paint?.isAntiAlias = true
        binding?.patientEditNoInsuranceCard?.setOnSingleClickListener(noInsuranceListener)

        binding?.fabNext?.setOnSingleClickListener {
            val frontCard = this.frontCardUrl
            val backCard = this.backCardUrl
            if (frontCard == null || backCard == null) {
                return@setOnSingleClickListener
            }

            onNavigateNext()
        }
    }

    /**
     * Delete the existing files - this should only be called when navigating back or tapping on
     * "NoInsuranceCard"
     */
    private fun clearCachedFiles() {
        frontCardUrl?.let { deleteFile(it) }
        backCardUrl?.let { deleteFile(it) }
    }

    private fun deleteFile(filePath: String) {
        val file = File(requireContext().cacheDir.absolutePath, File(filePath).name)
        if (file.delete()) {
            Timber.i("Deleted file: ${file.name}")
        } else {
            Timber.w("Unable to delete file: ${file.name}")
        }
    }

    private val noInsuranceListener: (View) -> Unit = {
        clearCachedFiles()
        onNoInsuranceCardClicked()
    }

    private fun toggleUI() {
        if (frontCardUrl != null && backCardUrl != null) {
            binding?.fabNext?.show()
        } else {
            binding?.fabNext?.hide()
        }

        when {
            frontCardUrl != null && backCardUrl != null -> {
                binding?.tapToCaptureLabel?.hide()
                binding?.patientEditNoInsuranceCard?.apply {
                    setOnSingleClickListener { }
                    typeface = Typeface.create(resources.getFont(R.font.graphik_regular), Typeface.ITALIC)
                    // paintFlags is a bitflag, removing it is taking the current flags and
                    // *and-ing* it with the inverse of the flag we want to remove
                    paintFlags = paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                    text = getString(R.string.patient_edit_tap_to_retake)
                }
            }
            else -> {
                binding?.tapToCaptureLabel?.show()
                binding?.patientEditNoInsuranceCard?.apply {
                    typeface = Typeface.create(resources.getFont(R.font.graphik_semi_bold), Typeface.BOLD)
                    paint?.flags = Paint.UNDERLINE_TEXT_FLAG
                    paint?.isAntiAlias = true
                    setOnSingleClickListener(noInsuranceListener)
                }
            }
        }
    }
}
