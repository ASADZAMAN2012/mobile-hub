/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

enum class MedDVaccines(val value: String) {
    TDAP("Tdap"),
    ZOSTER("Zoster"),
    RSV("RSV"),
    UNKNOWN("unknown");

    companion object {
        fun isMedDVaccine(value: String) = value.uppercase() in values().map { it.value.uppercase() }

        fun getByAntigen(antigen: String) =
            values()
                .firstOrNull { it.value.uppercase() == antigen.uppercase() } ?: UNKNOWN
    }
}
