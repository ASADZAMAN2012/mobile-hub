/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class Race(val value: String) {
    AmericanIndianOrAlaskaNative("American Indian or Alaska Native"),
    Asian("Asian"),
    BlackOrAfricanAmerican("Black or African American"),
    NativeHawaiianOrOtherPacificIslander("Native Hawaiian or Other Pacific Islander"),
    White("White"),
    OtherRace("Other"),
    DeclinedToSpecify("Declined to Specify");

    companion object {
        private val map = values().associateBy(Race::value)

        fun fromString(type: String) = map[type]
    }
}
