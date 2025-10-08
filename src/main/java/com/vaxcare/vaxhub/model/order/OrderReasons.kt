/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.order

import android.os.Parcelable
import com.vaxcare.vaxhub.R
import kotlinx.parcelize.Parcelize

interface OrderReasons : Parcelable {
    val value: Int
    val displayValue: Int
}

@Parcelize
enum class OrderedDoseNotAdministeredReasonEnum(override val value: Int, override val displayValue: Int) :
    OrderReasons {
    POSTPONED(4, R.string.orders_ordered_dose_reason_postponed),
    PATIENT_REFUSED(1, R.string.orders_ordered_dose_reason_patient_refused),
    OUT_OF_STOCK(2, R.string.orders_ordered_dose_reason_out_of_stock),
    OTHER(3, R.string.orders_ordered_dose_reason_other);

    companion object {
        fun fromValue(value: Int?): OrderedDoseNotAdministeredReasonEnum? =
            when (value) {
                1 -> PATIENT_REFUSED
                2 -> OUT_OF_STOCK
                3 -> OTHER
                4 -> POSTPONED
                else -> null
            }
    }
}

@Parcelize
enum class UnorderedDoseReasonModelReasonEnum(override val value: Int, override val displayValue: Int) :
    OrderReasons {
    ORDERED_NOT_APPEARING(1, R.string.orders_unordered_dose_reason_order_not_appearing),
    PHYSICIAN_UNABLE(2, R.string.orders_unordered_dose_reason_physician_unable),
    PRODUCT_MISMATCH(3, R.string.orders_unordered_dose_reason_product_mismatch),
    OTHER(4, R.string.orders_unordered_dose_reason_other);

    companion object {
        fun fromValue(value: Int?): UnorderedDoseReasonModelReasonEnum? =
            when (value) {
                1 -> ORDERED_NOT_APPEARING
                2 -> PHYSICIAN_UNABLE
                3 -> PRODUCT_MISMATCH
                4 -> OTHER
                else -> null
            }
    }
}
