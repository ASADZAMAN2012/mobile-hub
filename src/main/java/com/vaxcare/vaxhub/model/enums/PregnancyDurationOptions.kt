/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

enum class PregnancyDurationOptions(val displayName: String, val value: Int) {
    WEEKS_32("32 weeks", 32),
    WEEKS_33("33 weeks", 33),
    WEEKS_34("34 weeks", 34),
    WEEKS_35("35 weeks", 35),
    WEEKS_36("36 weeks", 36),
    DOES_NOT_QUALIFY("Patient does not qualify", 0)
}
