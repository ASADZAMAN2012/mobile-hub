/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

sealed class PaymentModals(
    val display: String,
    val summaryDisplay: String
) {
    data class PaymentDebitOrCredit(
        val cardOnFile: Boolean = false,
        val isCopay: Boolean = false
    ) : PaymentModals("Debit or Credit", "")

    object PaymentCashOrCheck : PaymentModals("Cash or Check", ": Cash or Check")

    object PaymentByPhone : PaymentModals("Pay By Phone", ": Pay By Phone")
}
