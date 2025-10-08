/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class AppointmentChangeType {
    Created,
    Updated,
    Deleted,
    Unknown;

    companion object {
        private val map = values().associateBy(AppointmentChangeType::ordinal)

        fun fromInt(type: Int?) =
            when {
                type == null || type >= map.size -> Unknown
                else -> map[type] ?: Unknown
            }
    }
}
