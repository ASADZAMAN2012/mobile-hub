/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.appointment

enum class AppointmentIcon(val value: String) {
    NONE("None"),
    STAR("Star"),
    FULL_CIRCLE("FullCircle"),
    HALF_CIRCLE("HalfCircle"),
    DOTTED_CIRCLE("DottedCircle"),
    DOLLAR("Dollar");

    companion object {
        private val map = values().associateBy(AppointmentIcon::value)

        fun fromString(value: String?) = map[value]
    }
}
