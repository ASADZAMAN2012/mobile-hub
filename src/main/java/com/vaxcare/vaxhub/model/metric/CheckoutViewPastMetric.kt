/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import java.time.LocalDate

data class CheckoutViewPastMetric(
    val visitId: Int,
    val checkContext: String,
    var checkoutRelativeTime: String,
    var checkoutResult: String
) : CheckoutMetric(visitId, "ViewPast") {
    enum class CheckContext(val displayName: String) {
        COMPLETE_SCREEN("CompleteScreen"),
        PATIENT_SCHEDULE("PatientSchedule")
    }

    enum class RelativeTime(val displayName: String) {
        SAME_DOS("SameDOS"),
        PAST_DOS("PastDOS"),
        FUTURE_DOS("FutureDOS");

        companion object {
            fun checkoutRelativeTime(date: LocalDate) =
                when {
                    date.isEqual(LocalDate.now()) -> SAME_DOS
                    date.isBefore(LocalDate.now()) -> PAST_DOS
                    else -> FUTURE_DOS
                }
        }
    }

    enum class CheckoutResult(val displayName: String) {
        VIEW_ONLY("ViewOnly"),
        NO_EDITS("NoEdits"),
        BOTH_EDITS("LotsRemovedAndAdded"),
        LOT_REMOVED("LotRemoved"),
        LOT_ADDED("LotAdded")
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("ViewCheckoutContext", checkContext)
            put("ViewCheckoutRelativeTime", checkoutRelativeTime)
            put("ViewCheckoutResult", checkoutResult)
        }
    }
}
