/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogSameInsuranceBinding
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SameInsuranceDialog : BaseDialog<DialogSameInsuranceBinding>() {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(dialogSize = DialogSize.LEGACY_WRAP)

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogSameInsuranceBinding =
        DialogSameInsuranceBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonOk?.setOnClickListener {
            confirmAndDismiss(true)
        }
        binding?.buttonCancel?.setOnClickListener {
            confirmAndDismiss(false)
        }
    }

    private fun confirmAndDismiss(confirm: Boolean) {
        globalDestinations.goBack(this@SameInsuranceDialog, mapOf(DIALOG_RESULT to confirm))
    }
}
