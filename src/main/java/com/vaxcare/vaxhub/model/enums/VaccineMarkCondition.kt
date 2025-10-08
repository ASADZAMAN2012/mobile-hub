/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.vaxcare.vaxhub.R

enum class VaccineMarkCondition(val medDTitleResId: Int) {
    NOT_COVERED(
        R.string.med_d_checkout_vaccine_immunization_not_covered
    ),
    OUT_OF_AGE(
        R.string.med_d_checkout_vaccine_out_of_age_indication
    ),
    MISSING_INFO(
        R.string.med_d_checkout_vaccine_missing_info
    )
}
