/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.core.extension.isFullSsnAndNotNull
import com.vaxcare.vaxhub.core.extension.isMaskedSsnFormatted
import com.vaxcare.vaxhub.core.extension.isMbiFormatAndNotNull
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus.ACTIVE_CHECKOUT
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus.PAST_CHECKOUT
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus.VIEW_CHECKOUT

class CheckoutAppointmentOpenedMetric(
    patientVisitId: Int,
    private val patientId: Int,
    private val stock: InventorySource,
    private val isCheckedOut: Boolean,
    private val paymentMethod: String,
    private val ssn: String?,
    private val mbi: String?,
    private val medDCta: String?,
    private val riskAssessmentId: Int?,
    private val vaccineCta: String?,
    private val larcCta: String?,
    private val editCheckoutStatus: EditCheckoutStatus,
    private val patientOrderCountTotal: Int,
    private val patientOrderCountOutstanding: Int,
) : CheckoutMetric(patientVisitId, "AppointmentOpened") {
    companion object {
        private const val PATIENT_ID = "patientId"
        private const val STOCK = "stock"
        private const val CHECKED_OUT = "isCheckedOut"
        private const val PAYMENT_METHOD = "paymentMethod"
        private const val IS_VALID_SSN_PRESENT = "isValidSSNPresent"
        private const val IS_VALID_MBI_PRESENT = "isValidMBIPresent"
        private const val MED_D_CTA = "medDCta"
        private const val RISK_ASSESSMENT_ID = "riskAssessmentId"
        private const val VACCINE_CTA = "vaccineCta"
        private const val LARC_CTA = "larcCta"
        private const val CHECKOUT_STATUS = "checkoutStatus"
        private const val PATIENT_ORDER_COUNT_TOTAL = "patientOrderCountTotal"
        private const val PATIENT_ORDER_COUNT_OUTSTANDING = "patientOrderCountOutstanding"
    }

    override fun toMap(): MutableMap<String, String> {
        val isValidSsn = ssn.isFullSsnAndNotNull() || ssn.isMaskedSsnFormatted()
        val isValidMbi = mbi.isMbiFormatAndNotNull()
        val checkoutStatus: String = when (editCheckoutStatus) {
            ACTIVE_CHECKOUT -> "Pre Shot"
            PAST_CHECKOUT, VIEW_CHECKOUT -> "Post Shot"
        }

        return super.toMap().apply {
            put(PATIENT_ID, patientId.toString())
            put(STOCK, stock.displayName)
            put(CHECKED_OUT, isCheckedOut.toString())
            put(PAYMENT_METHOD, paymentMethod)
            put(IS_VALID_SSN_PRESENT, isValidSsn.toString())
            put(IS_VALID_MBI_PRESENT, isValidMbi.toString())
            put(MED_D_CTA, medDCta.toString())
            put(RISK_ASSESSMENT_ID, riskAssessmentId.toString())
            put(VACCINE_CTA, vaccineCta.toString())
            put(LARC_CTA, larcCta.toString())
            put(CHECKOUT_STATUS, checkoutStatus)
            put(PATIENT_ORDER_COUNT_TOTAL, patientOrderCountTotal.toString())
            put(PATIENT_ORDER_COUNT_OUTSTANDING, patientOrderCountOutstanding.toString())
        }
    }
}
