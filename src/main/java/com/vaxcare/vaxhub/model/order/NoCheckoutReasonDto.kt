/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.order

import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class NoCheckoutReasonDto(
    val id: String,
    val patientVisitId: Int,
    val ormOrderNumber: String,
    val noCheckOutReason: Int,
    val lastModified: LocalDateTime,
    val versionNumber: Int
)
