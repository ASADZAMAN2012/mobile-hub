/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.appointment

enum class AppointmentIconColor(val value: String) {
    NONE("None"),
    PURPLE("Purple"),
    GREEN("Green"),
    MAGENTA("Magenta"),
    BLUE("Blue"),
    YELLOW("Yellow"),
    RED("Red");

    companion object {
        private val map = values().associateBy(AppointmentIconColor::value)

        fun fromString(value: String) = map[value] ?: NONE
    }
}
