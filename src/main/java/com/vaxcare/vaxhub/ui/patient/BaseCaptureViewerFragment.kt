/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.View
import com.squareup.picasso.Picasso
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentBaseCaptureViewerBinding
import java.io.File

abstract class BaseCaptureViewerFragment : BaseCaptureFlowFragment<FragmentBaseCaptureViewerBinding>() {
    abstract val title: String
    abstract val subTitle: String

    abstract fun actionNext(path: String)

    abstract fun actionCancel()

    val url: String by lazy {
        requireNotNull(arguments?.getString(CAPTURE_PHOTO_URL))
    }
    abstract val picasso: Picasso

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_base_capture_viewer,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentBaseCaptureViewerBinding =
        FragmentBaseCaptureViewerBinding.bind(
            container
        )

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.topBar?.setTitle(title)
        binding?.topBar?.setSubTitle(subTitle)

        picasso.load(url).into(binding?.photoViewer)

        binding?.buttonUsePhoto?.setOnClickListener {
            actionNext(File(requireContext().cacheDir.absolutePath, File(url).name).absolutePath)
        }

        binding?.buttonRetakePhoto?.setOnClickListener {
            try {
                File(url).delete()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
            actionCancel()
        }
    }

    companion object {
        const val CAPTURE_PHOTO_URL = "capture_photo_url"
    }
}
