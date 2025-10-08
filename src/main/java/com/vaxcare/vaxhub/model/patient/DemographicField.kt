/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import kotlinx.parcelize.Parcelize

/**
 * Fields for Demographic information updates
 */
sealed class DemographicField : InfoField {
    @Parcelize
    data class Phone(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.PHONE_PATH

        override fun equals(other: Any?): Boolean =
            when (other) {
                is InfoField -> other::class.java == this::class.java
                else -> false
            }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    @Parcelize
    data class FirstName(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.FIRSTNAME_PATH
    }

    @Parcelize
    data class LastName(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.LASTNAME_PATH
    }

    @Parcelize
    data class Gender(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.GENDER_PATH
    }

    @Parcelize
    data class DateOfBirth(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.DOB_PATH
    }

    @Parcelize
    data class AddressOne(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.ADDRESS1_PATH
    }

    @Parcelize
    data class AddressTwo(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.ADDRESS2_PATH
    }

    @Parcelize
    data class City(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.CITY_PATH
    }

    @Parcelize
    data class State(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.STATE_PATH
    }

    @Parcelize
    data class Zip(
        override var currentValue: String? = null
    ) : DemographicField() {
        override fun getPatchPath(): String = InfoType.ZIP_PATH
    }
}
