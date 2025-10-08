/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.partd

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.model.enums.PartDEligibilityStatusCode
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
@JsonClass(generateAdapter = true)
data class PartDResponse(
    @Json(name = "patientVisitId") override val patientVisitId: Int?,
    @Json(name = "copays") override val copays: List<PartDCopayResponse>,
    @Json(name = "systemHealthStatus") override val systemHealthStatus: Int? = null,
) : Parcelable, PartD

@Parcelize
@JsonClass(generateAdapter = true)
data class PartDCopayResponse(
    @Json(name = "NDC") override val ndc: String?,
    @Json(name = "hsProductId") override val productId: Int?,
    @Json(name = "copay") override val copay: BigDecimal?,
    @Json(name = "eligibilityStatusCode") override val eligibilityStatusCode: PartDEligibilityStatusCode?,
    @Json(name = "requestStatus") override val requestStatus: Int?
) : Parcelable, PartDCopay {
    fun isEligible() = eligibilityStatusCode == PartDEligibilityStatusCode.EligibilityVerified

    fun isFinished() = eligibilityStatusCode?.isFinished() ?: false
}
