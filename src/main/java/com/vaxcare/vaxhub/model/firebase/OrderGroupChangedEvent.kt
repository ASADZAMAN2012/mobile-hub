/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.firebase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.model.BaseFirebaseClinicEvent

@JsonClass(generateAdapter = true)
data class OrderGroupChangedEvent(
    @Json(name = "PartnerId") override val partnerId: Int,
    @Json(name = "ParentClinicId") override val parentClinicId: Int,
    @Json(name = "ClinicId") override val clinicId: Int,
    @Json(name = "EventId") override val eventId: String,
    @Json(name = "PlacerGroupNumber") val orderGroupNumber: String?,
    @Json(name = "ChangedFeatureFlagIds") override val changedFeatureFlagIds: List<Int>?
) : BaseFirebaseClinicEvent()
