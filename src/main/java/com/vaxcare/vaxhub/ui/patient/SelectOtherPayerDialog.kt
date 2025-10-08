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
import com.vaxcare.vaxhub.databinding.DialogSelectOtherPayerBinding
import com.vaxcare.vaxhub.model.enums.DialogAction
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SelectOtherPayerDialog : BaseDialog<DialogSelectOtherPayerBinding>() {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogSelectOtherPayerBinding =
        DialogSelectOtherPayerBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonOk?.setOnClickListener {
            globalDestinations.goBack(
                this@SelectOtherPayerDialog,
                mapOf(DIALOG_RESULT to DialogAction.POSITIVE)
            )
        }
    }
}
