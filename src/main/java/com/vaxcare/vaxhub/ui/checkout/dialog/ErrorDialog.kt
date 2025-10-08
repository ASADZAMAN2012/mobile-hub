/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogErrorBinding
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ErrorDialog : BaseDialog<DialogErrorBinding>() {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    companion object {
        const val RESULT = "RESULT"
        const val SECONDARY_KEY = "$RESULT-secondary"
    }

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    val args: ErrorDialogArgs by navArgs()

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogErrorBinding =
        DialogErrorBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.title?.setText(args.title)
        binding?.body?.setText(args.body)

        binding?.buttonContinue?.setText(args.primaryButton)
        binding?.buttonDelete?.setText(args.secondaryButton)

        binding?.buttonContinue?.setOnClickListener {
            setDialogResultAndGoBack(ErrorDialogButton.PRIMARY_BUTTON)
        }

        binding?.buttonDelete?.setOnClickListener {
            setDialogResultAndGoBack(ErrorDialogButton.SECONDARY_BUTTON)
        }
    }

    private fun setDialogResultAndGoBack(result: ErrorDialogButton) {
        setFragmentResult(
            RESULT,
            bundleOf(
                RESULT to result.ordinal,
                SECONDARY_KEY to args.optionalArg
            )
        )
        globalDestinations.goBack(
            this@ErrorDialog,
            mapOf(RESULT to result)
        )
    }
}

enum class ErrorDialogButton {
    PRIMARY_BUTTON,
    SECONDARY_BUTTON
}
