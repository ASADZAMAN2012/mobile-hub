/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.model.legacy.enums.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@JsonClass(generateAdapter = true)
data class Count(
    @Json(name = "ClinicId") val clinicId: Long,
    @Json(name = "VaccineCountEntries") val vaccineCountEntries: List<CountEntry>,
    @Json(name = "CreatedOn") val createdOn: LocalDateTime = LocalDateTime.now(),
    @Json(name = "Guid") val guid: String,
    @Json(name = "Stock") val stock: InventorySource,
    @Json(name = "UserId") val userId: Int
)

abstract class Transaction {
    open val adjustmentReason: String? = null
    open val adjustmentReasonType: String? = null
    open var adjustmentReturnDates: String? = null
    open val email: String? = null
    open val isAm: Boolean? = null
    open val isLotDataManuallyEntered: Boolean = false
    open val isPm: Boolean? = null
    open val receiptKey: String = UUID.randomUUID().toString()

    abstract val adjustmentType: TransactionType
    abstract val delta: Int
    abstract val doseValue: Float
    abstract val groupGuid: String
    abstract val lotExpirationDate: LocalDate
    abstract val lotNumber: String
    abstract val productId: Int
    abstract val stock: InventorySource
    abstract val userId: Int
    abstract val userName: String
}

@JsonClass(generateAdapter = true)
data class CountEntry(
    @Json(name = "CountGuid") val countGuid: String,
    @Json(name = "EpProductId") val epProductId: Int,
    @Json(name = "EntryId") val entryId: String = UUID.randomUUID().toString(),
    @Json(name = "IsDirty") val isDirty: Int = 1,
    @Json(name = "NewOnHand") val newOnHand: Int,
    @Json(name = "PrevOnHand") val prevOnHand: Int,
    @Json(name = "AdjustmentType") override val adjustmentType: TransactionType,
    @Json(name = "Delta") override val delta: Int,
    @Json(name = "DoseValue") override val doseValue: Float,
    @Json(name = "GroupGuid") override val groupGuid: String,
    @Json(name = "LotExpirationDate") override val lotExpirationDate: LocalDate,
    @Json(name = "LotNumber") override val lotNumber: String,
    @Json(name = "ProductId") override val productId: Int,
    @Json(name = "Stock") override val stock: InventorySource,
    @Json(name = "UserId") override val userId: Int,
    @Json(name = "UserName") override val userName: String
) : Transaction()

@JsonClass(generateAdapter = true)
data class Adjustment(
    override val adjustmentReason: String? = null,
    override val adjustmentReasonType: String? = null,
    override var adjustmentReturnDates: String? = null,
    override val adjustmentType: TransactionType,
    override val delta: Int,
    override val doseValue: Float,
    override var email: String? = null,
    override val groupGuid: String,
    override var isAm: Boolean? = null,
    override val isLotDataManuallyEntered: Boolean = false,
    override var isPm: Boolean? = null,
    override val lotExpirationDate: LocalDate,
    override val lotNumber: String,
    override val productId: Int,
    override val receiptKey: String = UUID.randomUUID().toString(),
    override val stock: InventorySource,
    override val userId: Int,
    override val userName: String
) : Transaction()
