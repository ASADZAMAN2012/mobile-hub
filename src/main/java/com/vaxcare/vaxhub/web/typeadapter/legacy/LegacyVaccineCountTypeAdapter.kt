/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.typeadapter.legacy

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.core.extension.intToEnum
import com.vaxcare.vaxhub.core.extension.readArray
import com.vaxcare.vaxhub.core.extension.readObject
import com.vaxcare.vaxhub.model.legacy.LegacyCount
import com.vaxcare.vaxhub.model.legacy.LegacyCountEntry
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LegacyVaccineCountTypeAdapter {
    @ToJson
    fun toJson(list: List<LegacyCount>): String {
        TODO("No reason to do this")
    }

    @FromJson
    fun fromJson(reader: JsonReader): List<LegacyCount> {
        val counts = mutableListOf<LegacyCount>()
        reader.readArray {
            var clinicId = 0L
            var createdOn = LocalDateTime.now()
            var guid = ""
            var partitionId = 0L
            var receiptKey = ""
            var userId = 0
            var stock = InventorySource.PRIVATE
            val entries = mutableListOf<LegacyCountEntry>()
            reader.readObject {
                when (reader.nextName()) {
                    "ClinicId" -> clinicId = reader.nextLong()
                    "CreatedOn" -> {
                        createdOn = LocalDateTime.parse(
                            reader.nextString(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                        )
                    }
                    "Guid" -> guid = reader.nextString()
                    "PartitionId" -> partitionId = reader.nextLong()
                    "ReceiptKey" -> receiptKey = reader.nextString()
                    "UserId" -> userId = reader.nextInt()
                    "Stock" -> stock = intToEnum(reader.nextInt(), InventorySource.PRIVATE)
                    "VaccineCountEntries" -> reader.readArray {
                        entries.add(readVaccineEntry(reader, guid))
                    }
                    else -> reader.skipValue()
                }
            }
            counts.add(
                LegacyCount(
                    clinicId,
                    entries,
                    createdOn,
                    guid,
                    stock,
                    userId
                )
            )
        }
        return counts
    }

    private fun readVaccineEntry(reader: JsonReader, guid: String): LegacyCountEntry {
        var adjustmentReason = ""
        var adjustmentReasonType = ""
        var adjustmentType = 0
        var doseValue = 0
        var entryId = ""
        var lotExp = LocalDateTime.now()
        var lotNumber = ""
        var newOnHand = 0
        var prevOnHand = 0
        var coreProductId = 0
        var productId = 0
        var epProductId = 0
        var stock = InventorySource.PRIVATE
        var userId = 0
        var userName = ""
        var isDirty = false
        reader.readObject {
            when (reader.nextName()) {
                "AdjustmentReason" -> adjustmentReason = reader.nextString()
                "AdjustmentReasonType" -> adjustmentReasonType = reader.nextString()
                "AdjustmentType" -> adjustmentType = reader.nextInt()
                "DoseValue" -> doseValue = (reader.nextString().toFloat() * 100).toInt()
                "EntryId" -> entryId = reader.nextString()
                "LotExpirationDate" -> lotExp = LocalDateTime.parse(
                    reader.nextString(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                )
                "LotNumber" -> lotNumber = reader.nextString()
                "NewOnHand" -> newOnHand = reader.nextInt()
                "PrevOnHand" -> prevOnHand = reader.nextInt()
                "CoreProductId" -> coreProductId = reader.nextInt()
                "EpProductId" -> epProductId = reader.nextInt()
                "Stock" -> stock = intToEnum(reader.nextInt(), InventorySource.PRIVATE)
                "UserId" -> userId = reader.nextInt()
                "UserName" -> userName = reader.nextString()
                "IsDirty" -> isDirty = reader.nextInt() == 1
                else -> reader.skipValue()
            }
        }

        return LegacyCountEntry(
            guid,
            doseValue,
            epProductId,
            entryId,
            lotNumber,
            newOnHand,
            prevOnHand
        )
    }
}
