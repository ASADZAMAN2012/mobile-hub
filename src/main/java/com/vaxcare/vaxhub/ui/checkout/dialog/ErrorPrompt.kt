/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogErrorBinding
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ErrorPrompt : BaseDialog<DialogErrorBinding>() {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    companion object {
        const val RESULT = "RESULT"
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
            globalDestinations.goBack(
                this@ErrorPrompt,
                mapOf(RESULT to ErrorDialogButton.PRIMARY_BUTTON)
            )
        }

        val params = binding?.buttonContinue?.layoutParams as MarginLayoutParams
        params.bottomMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            50F,
            context?.resources?.displayMetrics
        ).toInt()
        binding?.buttonDelete?.hide()
    }
}
