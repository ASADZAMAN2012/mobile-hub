/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

enum class AppointmentMediaType(val value: String, val tag: Int) {
    DRIVERS_LICENSE_FRONT("DriversLicenseFront", 1),
    DRIVERS_LICENSE_BACK("DriversLicenseBack", 2),
    INSURANCE_CARD_FRONT("InsuranceCardFront", 3),
    INSURANCE_CARD_BACK("InsuranceCardBack", 4),
    CREDIT_CARD_FRONT("CreditCardFront", 5),
    CREDIT_CARD_BACK("CreditCardBack", 6),
    SIGNATURE("Signature", 7),
    PATIENT_REGISTRATION_FORM("PatientRegistrationForm", 8)
}
