/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.typeconverter

import androidx.room.TypeConverter
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.core.extension.intToEnum

class InventorySourceEnumTypeConverters {
    @TypeConverter
    fun fromInventorySources(list: List<InventorySource>): String = list.map { it.id.toString() }.joinToString(",")

    @TypeConverter
    fun toInventorySources(string: String) = string.split(",").map { intToEnum(it.toInt(), InventorySource.PRIVATE) }

    @TypeConverter
    fun fromInventorySource(invSource: InventorySource): Int = invSource.id

    @TypeConverter
    fun toInventorySource(int: Int) = intToEnum(int, InventorySource.PRIVATE)
}
