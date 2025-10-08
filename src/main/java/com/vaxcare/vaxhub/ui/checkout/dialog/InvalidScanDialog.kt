/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogInvalidScanBinding
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InvalidScanDialog : BaseDialog<DialogInvalidScanBinding>() {
    companion object {
        private const val CDATA_PREFIX = "<![CDATA["
        private const val CDATA_SUFFIX = "]]>"
    }

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    val args: InvalidScanDialogArgs by navArgs()

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogInvalidScanBinding =
        DialogInvalidScanBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        when (args.messageType) {
            InvalidScanMessageType.HTML -> {
                val htmlText = HtmlCompat
                    .fromHtml(args.displayMessage.cleanCData(), HtmlCompat.FROM_HTML_MODE_COMPACT)
                if (htmlText.isNotEmpty()) {
                    binding?.body?.text = htmlText
                }
            }
            InvalidScanMessageType.TEXT -> {
                binding?.body?.text = args.displayMessage
            }
        }

        binding?.title?.text = args.title
        binding?.buttonContinue?.setOnClickListener {
            globalDestinations.goBack(this@InvalidScanDialog)
        }
    }

    private fun String.cleanCData() = replace(CDATA_PREFIX, "").replace(CDATA_SUFFIX, "")
}

enum class InvalidScanMessageType {
    HTML,
    TEXT
}
