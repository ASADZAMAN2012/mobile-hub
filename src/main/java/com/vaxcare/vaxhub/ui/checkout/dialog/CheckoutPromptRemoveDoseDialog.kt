/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutPromptRemoveDoseDialog : BaseOneTouchCheckoutDialog() {
    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    override val header: String
        get() = getString(R.string.checkout_remove_dose_title)

    override val body: String
        get() = getString(R.string.checkout_remove_dose_message)

    override val resultKey: String
        get() = CHECKOUT_REMOVE_DOSE_DIALOG_RESULT_KEY

    override fun viewChanges() {
        binding?.buttonContinue?.setText(R.string.patient_checkout_remove_dose)
        binding?.buttonDelete?.setText(R.string.scanned_dose_issue_keep_dose)
        binding?.dialogBodyPayer?.hide()
        binding?.dialogSubBody?.hide()
        binding?.buttonNeutral?.hide()
    }

    companion object {
        const val CHECKOUT_REMOVE_DOSE_DIALOG_RESULT_KEY = "checkoutRemoveDoseResultKey"
    }
}
