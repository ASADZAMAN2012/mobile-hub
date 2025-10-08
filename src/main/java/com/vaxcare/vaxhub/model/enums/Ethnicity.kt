/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class Ethnicity(val value: String) {
    HispanicOrLatinoSpanish("Hispanic or Latino Spanish"),
    NotHispanicOrLatino("Not Hispanic or Latino"),
    Unknown("Unspecified");

    companion object {
        private val map = values().associateBy(Ethnicity::value)

        fun fromString(type: String) = map[type]
    }
}
