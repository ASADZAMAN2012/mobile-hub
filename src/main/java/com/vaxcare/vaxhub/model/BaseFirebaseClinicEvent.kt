/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

abstract class BaseFirebaseClinicEvent {
    abstract val partnerId: Int?
    abstract val parentClinicId: Int?
    abstract val clinicId: Int?
    abstract val eventId: String?
    abstract val changedFeatureFlagIds: List<Int>?
}

@JsonClass(generateAdapter = true)
data class FirebaseClinicEvent(
    @Json(name = "PartnerId") override val partnerId: Int?,
    @Json(name = "ParentClinicId") override val parentClinicId: Int?,
    @Json(name = "ClinicId") override val clinicId: Int?,
    @Json(name = "EventId") override val eventId: String?,
    @Json(name = "ChangedFeatureFlagIds") override val changedFeatureFlagIds: List<Int>?,
) : BaseFirebaseClinicEvent()
