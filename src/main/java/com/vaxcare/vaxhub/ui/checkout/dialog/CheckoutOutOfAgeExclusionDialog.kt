/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutOutOfAgeExclusionDialog : BaseOneTouchCheckoutDialog() {
    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    override val header: String
        get() = getString(R.string.patient_checkout_out_of_age_exclusion_title)

    override val body: String
        get() = getString(R.string.patient_checkout_out_of_age_exclusion_body)

    override val resultKey: String
        get() = OUT_OF_AGE_EXCLUSION_DIALOG_RESULT_KEY

    companion object {
        const val OUT_OF_AGE_EXCLUSION_DIALOG_RESULT_KEY = "ooaIndicationDialogExclusionResultKey"
    }
}
