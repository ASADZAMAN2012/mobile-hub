/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ShotStatus(val value: Int) {
    PreShot(0),
    PostShot(1);

    companion object {
        fun fromValue(value: Int) =
            when (value) {
                0 -> PreShot
                1 -> PostShot
                else -> PreShot
            }
    }
}
