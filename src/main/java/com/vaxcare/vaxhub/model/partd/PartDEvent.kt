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
data class PartDEvent(
    @Json(name = "Copays") override val copays: List<PartDCopayEvent>,
    @Json(name = "PatientVisitId") override val patientVisitId: Int,
    @Json(name = "SystemHealthStatus") override val systemHealthStatus: Int? = null
) : Parcelable, PartD

@Parcelize
@JsonClass(generateAdapter = true)
data class PartDCopayEvent(
    @Json(name = "NDC") override val ndc: String?,
    @Json(name = "Copay") override val copay: BigDecimal?,
    @Json(name = "HsProductId") override val productId: Int?,
    @Json(name = "EligibilityStatusCode") override val eligibilityStatusCode: PartDEligibilityStatusCode?,
    @Json(name = "RequestStatus") override val requestStatus: Int?
) : Parcelable, PartDCopay
