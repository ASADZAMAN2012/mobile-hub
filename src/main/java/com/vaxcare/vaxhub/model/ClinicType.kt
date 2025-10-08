/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ClinicType {
    Permanent,
    Temporary
}
