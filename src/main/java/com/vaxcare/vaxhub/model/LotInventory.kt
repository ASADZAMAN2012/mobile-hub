/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import com.squareup.moshi.JsonClass
import java.util.Date

@Entity(primaryKeys = ["inventorySource", "productId", "lotNumber"])
@JsonClass(generateAdapter = true)
data class LotInventory(
    val lotNumber: String,
    val onHand: Int,
    val inventorySource: Int,
    val productId: Int,
    val antigen: String,
    val inventoryGroup: String
)

data class LotInventoryResponse(
    val inventory: List<LotInventory>,
    val lastSync: Date,
    val nextSync: Date
)
