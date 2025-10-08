/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import com.vaxcare.vaxhub.model.enums.RiskMachineFlagType
import kotlinx.parcelize.Parcelize

/**
 * Appointment MetaData field flags
 */
sealed class AppointmentFlagsField(val flags: List<String>) : InfoField {
    @Parcelize
    data class PhoneOptIn(
        override var currentValue: String? = null
    ) : AppointmentFlagsField(listOf(RiskMachineFlagType.PatientContactPhoneOptInFlag.value)) {
        override fun getPatchPath(): String = InfoType.FLAG_PATH
    }

    @Parcelize
    data class PhoneOptOut(
        override var currentValue: String? = null
    ) : AppointmentFlagsField(listOf()) {
        override fun getPatchPath(): String = InfoType.FLAG_PATH
    }
}
