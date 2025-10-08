/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class NetworkStatus(val value: Int) {
    CONNECTED(0),
    CONNECTED_VAXCARE_UNREACHABLE(1),
    CONNECTED_NO_INTERNET(2),
    DISCONNECTED(-1);

    companion object {
        private val map = values().associateBy(NetworkStatus::value)

        fun fromInt(type: Int) = map[type] ?: DISCONNECTED
    }
}
