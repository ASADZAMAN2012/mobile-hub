/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.Json

enum class RiskFactor {
    @Json(name = "CovidUnder65")
    COVID_UNDER_65,

    @Json(name = "RsvPregnant")
    RSV_PREGNANT
}
