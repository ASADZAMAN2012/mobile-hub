/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.appointment

enum class AppointmentServiceType(val id: Int, val display: String) {
    UNKNOWN(0, "Unknown"),
    VACCINE(1, "Vaccine"),
    MEDD(2, "MedD"),
    LARC(3, "Larc");

    companion object {
        private val map = values().associateBy(AppointmentServiceType::display)

        fun fromString(display: String) = map[display] ?: UNKNOWN
    }
}
