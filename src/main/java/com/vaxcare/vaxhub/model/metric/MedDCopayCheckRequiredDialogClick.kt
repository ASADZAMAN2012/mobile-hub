/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.ui.checkout.dialog.MedDReviewCopayDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.MedDReviewCopayDialog.Option.REMOVE_DOSE
import com.vaxcare.vaxhub.ui.checkout.dialog.MedDReviewCopayDialog.Option.RUN_COPAY_CHECK

class MedDCopayCheckRequiredDialogClick(
    private val optionSelected: MedDReviewCopayDialog.Option,
    patientVisitId: Int
) : CheckoutMedDMetric(patientVisitId, "CopayRequiredDialog.Click") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put(
                "optionSelected",
                when (optionSelected) {
                    RUN_COPAY_CHECK -> "Run Copay Check"
                    REMOVE_DOSE -> "Remove Dose"
                }
            )
        }
    }
}
