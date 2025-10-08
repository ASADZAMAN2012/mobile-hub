/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogNotInCovidAssistBinding
import com.vaxcare.vaxhub.ui.checkout.dialog.NotInCOVIDAssistDialog.CheckCOVIDBehavior.CREATE_PRODUCT
import com.vaxcare.vaxhub.ui.checkout.dialog.NotInCOVIDAssistDialog.CheckCOVIDBehavior.SCAN_OR_SEARCH_PRODUCT
import com.vaxcare.vaxhub.ui.navigation.LotDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotInCOVIDAssistDialog : BaseDialog<DialogNotInCovidAssistBinding>() {
    private val args: NotInCOVIDAssistDialogArgs by navArgs()

    @Inject
    lateinit var destination: LotDestination

    enum class CheckCOVIDBehavior {
        SCAN_OR_SEARCH_PRODUCT,
        CREATE_PRODUCT;

        companion object {
            private val map = values().associateBy(CheckCOVIDBehavior::ordinal)

            fun fromInt(type: Int) = map[type] ?: SCAN_OR_SEARCH_PRODUCT
        }
    }

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogNotInCovidAssistBinding =
        DialogNotInCovidAssistBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        when (CheckCOVIDBehavior.fromInt(args.checkCovidAction)) {
            SCAN_OR_SEARCH_PRODUCT -> {
                binding?.covidAssistNotEnrolledPurpleBack?.hide()
                binding?.covidAssistNotEnrolledGrayCancel?.hide()
                binding?.covidAssistNotEnrolledPurpleCancel?.show()
                binding?.covidAssistNotEnrolledPurpleCancel?.setOnClickListener {
                    dismiss()
                }
            }
            CREATE_PRODUCT -> {
                binding?.covidAssistNotEnrolledPurpleBack?.show()
                binding?.covidAssistNotEnrolledGrayCancel?.show()
                binding?.covidAssistNotEnrolledPurpleCancel?.hide()
                binding?.covidAssistNotEnrolledPurpleBack?.setOnClickListener {
                    dismiss()
                }
                binding?.covidAssistNotEnrolledGrayCancel?.setOnClickListener {
                    destination.goBackToCheckoutVaccine(this@NotInCOVIDAssistDialog)
                    dismiss()
                }
            }
        }
    }
}
