/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogProcessingAppointmentBinding
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProcessingAppointmentDialog : BaseDialog<DialogProcessingAppointmentBinding>() {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    companion object {
        const val REFRESH = "Refresh"
    }

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogProcessingAppointmentBinding =
        DialogProcessingAppointmentBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonRefresh?.setOnClickListener {
            globalDestinations.goBack(this@ProcessingAppointmentDialog, mapOf(REFRESH to true))
        }
    }
}
