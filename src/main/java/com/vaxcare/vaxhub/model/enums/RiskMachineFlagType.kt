/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

sealed class RiskMachineFlagType(val value: String) {
    object PatientContactPhoneOptInFlag : RiskMachineFlagType("PatientContactPhoneOptIn")
}
