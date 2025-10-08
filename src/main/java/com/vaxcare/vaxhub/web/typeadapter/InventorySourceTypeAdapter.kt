/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.typeadapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.core.extension.readArray
import com.vaxcare.vaxhub.core.extension.readObject

class InventorySourceTypeAdapter {
    @ToJson
    fun toJson(list: List<InventorySource>): String {
        TODO("NOT IMPLEMENTED")
    }

    @FromJson
    fun fromJson(reader: JsonReader): List<InventorySource> {
        val sources = mutableListOf<InventorySource>()
        // Always add private
        sources.add(InventorySource.PRIVATE)

        reader.readArray {
            reader.readObject {
                when (reader.nextName()) {
                    "id" -> {
                        val id = reader.nextInt()
                        val source = when (id) {
                            1 -> InventorySource.PRIVATE
                            2 -> InventorySource.VFC
                            3 -> InventorySource.STATE
                            4 -> InventorySource.THREE_SEVENTEEN
                            else -> InventorySource.PRIVATE
                        }
                        sources.add(source)
                    }
                    else -> reader.skipValue()
                }
            }
        }

        return sources
    }

    @ToJson
    fun sourceToJson(source: InventorySource): String {
        return source.id.toString()
    }

    @FromJson
    fun jsonToSource(reader: JsonReader): InventorySource {
        TODO()
    }
}
