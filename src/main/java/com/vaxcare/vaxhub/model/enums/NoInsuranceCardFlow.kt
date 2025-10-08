/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

enum class NoInsuranceCardFlow {
    EDIT_PATIENT,
    CREATE_PATIENT,
    ABORT,
    CHECKOUT_PAYMENT,
    COPAY_PAYMENT,
    CHECKOUT_PATIENT;

    companion object {
        fun fromOrdinal(value: Int): NoInsuranceCardFlow? = values().firstOrNull { it.ordinal == value }
    }
}
