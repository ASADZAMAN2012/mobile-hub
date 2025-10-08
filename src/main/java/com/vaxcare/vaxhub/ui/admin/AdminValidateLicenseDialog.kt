/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.admin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.makeShortToast
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseScannerDialog
import com.vaxcare.vaxhub.databinding.DialogValidateScannerLicenseBinding
import com.vaxcare.vaxhub.service.ScannerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdminValidateLicenseDialog : BaseScannerDialog<DialogValidateScannerLicenseBinding>() {
    @Inject
    override lateinit var scannerManager: ScannerManager

    override val baseDialogProperties: DialogProperties = DialogProperties(
        scannerViewport = R.id.scanner_viewport,
        scannerPreview = R.id.scanner_preview,
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogValidateScannerLicenseBinding =
        DialogValidateScannerLicenseBinding.inflate(inflater, container, true)

    override fun onScannerLicenseValid() {
        context?.makeShortToast(R.string.validate_scanner_success)
    }

    override fun onScannerLicenseInvalid() {
        context?.makeShortToast(R.string.validate_scanner_failure)
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        binding?.buttonOk?.setOnClickListener {
            dialog?.dismiss()
            val imm =
                it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        }
    }
}
