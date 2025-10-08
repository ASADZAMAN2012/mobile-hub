/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory

import androidx.room.Entity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.core.model.enums.InventorySource

@Entity(primaryKeys = ["lotNumberName", "inventorySource"])
@JsonClass(generateAdapter = true)
data class SimpleOnHandProductDTO(
    @Json(name = "lotNumber")
    val lotNumberName: String,
    val inventorySource: Int,
    @Json(name = "onHand")
    val onHandAmount: Int
)

fun SimpleOnHandProductDTO.toSimpleOnHandProduct(): SimpleOnHandProduct =
    SimpleOnHandProduct(
        lotNumberName = lotNumberName,
        inventorySource = InventorySource.valueOfSourceId(inventorySource),
        onHandAmount = onHandAmount
    )

data class SimpleOnHandProduct(
    val lotNumberName: String,
    val inventorySource: InventorySource,
    val onHandAmount: Int
)
