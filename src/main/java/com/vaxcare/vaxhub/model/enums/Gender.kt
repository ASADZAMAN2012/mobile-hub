/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.model.Patient

@JsonClass(generateAdapter = false)
enum class Gender {
    MF,
    M,
    F;

    companion object {
        private val map = values().associateBy(Gender::ordinal)

        fun fromInt(type: Int) = map[type]
    }

    fun toPatientGender() =
        when (this) {
            M -> Patient.PatientGender.MALE
            else -> Patient.PatientGender.FEMALE
        }
}
