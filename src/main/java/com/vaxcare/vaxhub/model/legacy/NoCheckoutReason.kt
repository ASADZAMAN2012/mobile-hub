/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy

import java.time.LocalDateTime
import java.util.UUID

data class NoCheckoutReason(
    val id: String = UUID.randomUUID().toString(),
    val patientVisitId: Int,
    val ormOrderNumber: String,
    val noCheckoutReason: Int,
    val lastModified: LocalDateTime,
    val versionNumber: Int
)
