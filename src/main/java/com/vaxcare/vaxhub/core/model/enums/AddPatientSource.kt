/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.model.enums

enum class AddPatientSource {
    ADD_NEW_PARENT_PATIENT,
    ADD_SUGGEST_PARENT_PATIENT,
    OTHER;

    companion object {
        private val map = values().associateBy(AddPatientSource::ordinal)

        fun fromInt(type: Int) = map[type] ?: OTHER
    }
}
