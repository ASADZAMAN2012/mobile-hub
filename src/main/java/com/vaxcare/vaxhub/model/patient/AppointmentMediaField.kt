/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import kotlinx.parcelize.Parcelize

/**
 * Appointment Media field uris
 */
sealed class AppointmentMediaField(val value: String, val tag: Int) : InfoField {
    @Parcelize
    data class DriversLicenseFront(override var currentValue: String? = "") :
        AppointmentMediaField("DriversLicenseFront", 1)

    @Parcelize
    data class DriversLicenseBack(override var currentValue: String? = "") :
        AppointmentMediaField("DriversLicenseBack", 2)

    @Parcelize
    data class InsuranceCardFront(override var currentValue: String? = "") :
        AppointmentMediaField("InsuranceCardFront", 3)

    @Parcelize
    data class InsuranceCardBack(override var currentValue: String? = "") :
        AppointmentMediaField("InsuranceCardBack", 4)

    @Parcelize
    data class CreditCardFront(override var currentValue: String? = "") :
        AppointmentMediaField("CreditCardFront", 5)

    @Parcelize
    data class CreditCardBack(override var currentValue: String? = "") :
        AppointmentMediaField("CreditCardBack", 6)

    @Parcelize
    data class Signature(override var currentValue: String? = "") :
        AppointmentMediaField("Signature", 7)

    @Parcelize
    data class PatientRegistrationForm(override var currentValue: String? = "") :
        AppointmentMediaField("PatientRegistrationForm", 8)

    override fun getPatchPath(): String = InfoType.MEDIA_PATH
}
