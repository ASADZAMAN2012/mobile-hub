/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

enum class EditCheckoutStatus(val summaryDisplay: String) {
    ACTIVE_CHECKOUT("Summary"),
    PAST_CHECKOUT("Updated Summary"),
    VIEW_CHECKOUT("Past Checkout Summary");

    companion object {
        fun fromInt(value: Int) = values()[value]
    }

    fun isEditable() = this != VIEW_CHECKOUT

    fun isCheckedOut() = this != ACTIVE_CHECKOUT
}
