/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.vaxcare.core.model.enums.InventorySource

@Entity(tableName = "Location")
@JsonClass(generateAdapter = true)
data class Location(
    @PrimaryKey
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
    val integrationType: IntegrationType?
)
