/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogBackCheckoutBinding
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BackCheckoutDialog : BaseDialog<DialogBackCheckoutBinding>() {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogBackCheckoutBinding =
        DialogBackCheckoutBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonContinue?.setOnClickListener {
            globalDestinations.goBack(this@BackCheckoutDialog, mapOf(BACK_CHECKOUT_RESULT to false))
        }

        binding?.buttonDelete?.setOnClickListener {
            globalDestinations.goBack(this@BackCheckoutDialog, mapOf(BACK_CHECKOUT_RESULT to true))
        }
    }

    companion object {
        const val BACK_CHECKOUT_RESULT = "back_checkout_result"
    }
}
