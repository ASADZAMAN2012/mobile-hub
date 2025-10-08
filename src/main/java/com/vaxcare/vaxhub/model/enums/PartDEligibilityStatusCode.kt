/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PartDEligibilityStatusCode {
    New, // failure
    Pending, // failure
    EligibilityVerified, // covered
    NoCoverage, // not covered
    NonContracted, // not covered
    CardholderMismatch, // failure
    PatientNotFound, // failure
    PriorAuthRequired, // failure
    UnspecifiedException, // failure
    Failed; // failure

    fun isFinished() = this != New && this != Pending

    companion object {
        private val failureCodes = listOf(
            New,
            Pending,
            CardholderMismatch,
            PatientNotFound,
            PriorAuthRequired,
            UnspecifiedException,
            Failed
        )
        private val map = values().associateBy(PartDEligibilityStatusCode::ordinal)

        fun isFailure(status: PartDEligibilityStatusCode?) = status == null || status in failureCodes

        fun fromInt(type: Int?) =
            when {
                type == null -> Failed
                type < map.size -> map[type] ?: Failed
                else -> Failed
            }
    }
}
