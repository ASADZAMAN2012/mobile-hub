/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog.Options.CONFIRM
import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog.Options.GO_BACK

class ConfirmEnteredLotNumberDialogClickMetric(
    private val optionSelected: ConfirmEnteredLotNumberDialog.Options,
    private val enteredLotNumber: String,
    patientVisitId: Int?
) : CheckoutMetric(
        patientVisitId = patientVisitId,
        checkoutEventName = "ConfirmEnteredLotNumberDialog.Click"
    ) {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("selection", optionSelected.getMetricValue())
            put("enteredLotNumber", enteredLotNumber)
        }
    }

    private fun ConfirmEnteredLotNumberDialog.Options.getMetricValue(): String =
        when (this) {
            CONFIRM -> "Confirm"
            GO_BACK -> "Go Back"
        }
}
