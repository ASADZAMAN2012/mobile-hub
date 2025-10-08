/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class CheckData(
    val result: Boolean,
    val tabletId: String
)
