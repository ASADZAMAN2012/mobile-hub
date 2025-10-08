/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutPromptMedDExclusionDialog : BaseOneTouchCheckoutDialog() {
    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    override val header: String
        get() = getString(R.string.patient_checkout_prompt_med_d_exclusion_title)

    override val body: String
        get() = getString(R.string.patient_checkout_prompt_med_d_exclusion_body)

    override val resultKey: String
        get() = MEDD_EXCLUSION_DIALOG_RESULT_KEY

    override fun viewChanges() {
        binding?.dialogSubBody?.hide()
    }

    companion object {
        const val MEDD_EXCLUSION_DIALOG_RESULT_KEY = "medDPromptDialogExclusionResultKey"
    }
}
