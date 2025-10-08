/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@Deprecated("Use PartDResponse")
@JsonClass(generateAdapter = true)
data class MedDCheckResponse(
    val appointmentId: String,
    val eligible: Boolean,
    val copays: List<Copay>
) {
    @JsonClass(generateAdapter = true)
    data class Copay(
        val id: Int = 0,
        val appointmentId: Int = 0,
        val opportunityId: Int = 0,
        val antigen: String,
        val productName: String? = null,
        val copay: BigDecimal,
        val transactionId: String? = null,
        val provider: Int = 0
    )
}
