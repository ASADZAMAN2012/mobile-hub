/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Relation
import com.squareup.moshi.JsonClass
import com.vaxcare.core.model.enums.InventorySource

@JsonClass(generateAdapter = true)
class LocationData(
    val clinicId: Int,
    val partnerId: Int,
    val partnerName: String?,
    val clinicNumber: String?,
    val clinicName: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val primaryPhone: String?,
    val contactId: Int?,
    // TODO: Fix serialization of booleans
//    val scheduler: Boolean,
//    val vfc: Boolean,
//    val terminationInProgress: Boolean,
//    val estimatedShots: Int?,
//    val partnerActiveContract: Boolean,
//    val isVaxBuyEnabled: Boolean,
//    val isManageInventoryEnabled: Boolean,
//    val isLotLevelCountRequired: Boolean,
//    val showReturnTransactions: Boolean,
    val parentClinicId: Int?,
    val inventorySources: List<InventorySource>,
    @Relation(parentColumn = "clinicId", entityColumn = "featureFlagId")
    var activeFeatureFlags: List<FeatureFlag>,
    var integrationType: IntegrationType? = null
)
