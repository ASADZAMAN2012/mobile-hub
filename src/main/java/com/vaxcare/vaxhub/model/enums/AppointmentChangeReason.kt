/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class AppointmentChangeReason {
    PatientDataChanged,
    AppointmentDataChanged,
    RiskInvoked,
    RiskUpdated,
    CheckoutCompleted,
    UndoCheckoutCompleted,
    DiscoveryInvoked,
    DiscoveryCompleted,
    MedDInvoked,
    MedDCompleted,
    MedDError,
    Unknown;

    companion object {
        private val map = values().associateBy(
            AppointmentChangeReason::ordinal
        )

        fun fromInt(type: Int?) =
            when {
                type == null -> Unknown
                type < map.size -> map[type] ?: Unknown
                else -> Unknown
            }
    }
}
