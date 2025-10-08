/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.enums.ProductPresentation
import com.vaxcare.vaxhub.model.enums.ProductStatus
import java.time.LocalDateTime

@Entity
data class LegacyCount(
    val clinicId: Long,
    val createdOn: LocalDateTime,
    @PrimaryKey val guid: String,
    val stock: InventorySource,
    val userId: Int
) {
    @Ignore
    constructor(
        clinicId: Long,
        countEntries: List<LegacyCountEntry>,
        createdOn: LocalDateTime,
        guid: String,
        stock: InventorySource,
        userId: Int
    ) : this(
        clinicId,
        createdOn,
        guid,
        stock,
        userId
    ) {
        countList = countEntries
    }

    @Ignore
    var countList: List<LegacyCountEntry> = listOf()
}

@Entity
data class LegacyCountEntry(
    val countGuid: String,
    val doseValue: Int,
    val epProductId: Int,
    @PrimaryKey val guid: String,
    val lotNumber: String,
    val newOnHand: Int,
    val prevOnHand: Int
)

data class LegacyProductWithCountDto(
    val antigen: String,
    val categoryId: ProductCategory,
    val description: String,
    val displayName: String,
    val id: Int,
    val lossFee: Int?,
    val onHand: Int,
    val presentation: ProductPresentation,
    val purchaseOrderFee: Int?,
    val status: ProductStatus,
    // Count Data
    val createdOn: LocalDateTime?,
    val guid: String?,
    val lotNumber: String?,
    val stock: InventorySource?,
    val newOnHand: Int?,
    val prevOnHand: Int?,
    val doseValue: Int?
)

data class LegacyProductWithCount(
    val antigen: String,
    val categoryId: ProductCategory,
    val description: String,
    val displayName: String,
    val id: Int,
    val lossFee: Int?,
    val onHand: Int,
    val presentation: ProductPresentation,
    val purchaseOrderFee: Int?,
    val status: ProductStatus,
    val counts: List<LegacyCountGroup>
)

data class LegacyCountGroup(
    val countedLots: List<LegacyCountLot>,
    val createdOn: LocalDateTime,
    val doseValue: Int,
    val guid: String,
    val stock: InventorySource,
    val newOnHand: Int,
    val prevOnHand: Int
)

data class LegacyCountLot(
    val lotNumber: String,
    val financialImpact: Int,
    val variance: Int
)
