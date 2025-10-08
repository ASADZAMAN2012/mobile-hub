/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy

import androidx.room.Entity
import androidx.room.Ignore
import com.vaxcare.core.model.enums.InventorySource
import java.time.LocalDate

@Entity(primaryKeys = ["lotName", "inventorySource", "productId"])
data class LegacyLotInventory(
    val doseValue: Int,
    val inventorySource: InventorySource,
    val lotName: String,
    val onHand: Int,
    val productId: Int,
    val salesProductId: Int
) {
    @Ignore
    var editing: Boolean = false

    @Ignore
    var transferred: Int? = null

    @Ignore
    var expDate: LocalDate? = null

    @Ignore
    var confirmed: Int? = null

    fun hasBeenEdited() = transferred != null || confirmed != null

    fun editedValue() = transferred ?: confirmed
}
