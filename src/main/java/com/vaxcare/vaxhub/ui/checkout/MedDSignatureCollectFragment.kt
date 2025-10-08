/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.os.Bundle
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentMedDSignatureCollectBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.domain.signature.SaveSignatureUseCase
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.MedDCheckoutDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MedDSignatureCollectFragment : BaseFragment<FragmentMedDSignatureCollectBinding>() {
    companion object {
        const val SIGNATURE_URI_KEY = "signature_uri_key"
    }

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: MedDCheckoutDestination

    @Inject
    lateinit var saveSignature: SaveSignatureUseCase

    @Inject
    lateinit var fileStorage: FileStorage

    @Inject
    lateinit var globalDestination: GlobalDestinations

    private val screenTitle = "MedDConsentSignature"
    private val args: MedDSignatureCollectFragmentArgs by navArgs()

    private val paymentInformation by lazy { args.paymentInformation }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_med_d_signature_collect,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentMedDSignatureCollectBinding =
        FragmentMedDSignatureCollectBinding.bind(container)

    override fun handleBack(): Boolean {
        val result = args.signatureUri != null && binding?.medDSignatureSubmit?.isEnabled == true
        if (result) {
            saveSignatureAndGetFileUri {
                globalDestination.goBack(this, mapOf(SIGNATURE_URI_KEY to it))
            }
        }

        return result
    }

    /**
     * Rotate to landscape
     * `requestedOrientation` Will cause an animation and the recreate of the activity
     */
    private fun requestRotationLayout() {
        val width = requireActivity().window.decorView.width
        val height = requireActivity().window.decorView.height
        view?.rotation = 270f
        view?.translationX = ((width - height) / 2).toFloat()
        view?.translationY = ((height - width) / 2).toFloat()
        val layoutParams = view?.layoutParams
        layoutParams?.height = width
        layoutParams?.width = height
        view?.layoutParams = layoutParams
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        requestRotationLayout()
        binding?.apply {
            medDSignatureView.lineColor =
                ContextCompat.getColor(requireContext(), R.color.coral_light)

            medDSignatureSubmit.isEnabled = false
            args.signatureUri?.let { fileUri ->
                fileStorage.popFileContents(fileUri, requireContext().contentResolver)
                    ?.let { base64Signature ->
                        medDSignatureView.applyReadOnlySignature(
                            Base64.decode(
                                base64Signature,
                                Base64.DEFAULT
                            )
                        )
                        medDSignatureSubmit.isEnabled = true
                    }
            }
            medDSignatureView.touchCallback = {
                it?.let {
                    when (it.action) {
                        MotionEvent.ACTION_UP -> {
                            medDSignatureSubmit.isEnabled = true
                            medDClearSignature.show()
                        }
                    }
                }
            }

            medDClearSignature.setOnClickListener {
                medDSignatureView.resetCanvas()
                medDSignatureSubmit.isEnabled = false
                medDClearSignature.hide()
            }

            medDSignatureSubmit.setOnSingleClickListener {
                saveSignatureAndGetFileUri { fileUri ->
                    destination.goToSignatureSubmit(
                        this@MedDSignatureCollectFragment,
                        paymentInformation = paymentInformation,
                        fileUri = fileUri
                    )
                }
            }
        }
    }

    private fun saveSignatureAndGetFileUri(onFileUriResolved: (String) -> Unit) {
        binding?.medDSignatureView?.getSignatureBytes(
            window = requireActivity().window,
            trimImage = true,
            vertical = false
        ) { bytes ->
            val fileUri = saveSignature(
                Base64.encodeToString(
                    bytes,
                    Base64.DEFAULT
                )
            )

            onFileUriResolved(fileUri)
        }
    }
}
