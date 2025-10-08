/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class CompensationSubStatus {
    Unspecified,
    InsuranceInfoCaptured,
    InsuranceInfoNotCaptured;

    fun isInReview() = this != Unspecified
}
