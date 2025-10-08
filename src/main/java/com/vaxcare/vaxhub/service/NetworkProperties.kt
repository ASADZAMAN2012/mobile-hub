/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

data class NetworkProperties(
    val ssid: String? = null,
    val security: String? = null,
    val frequency: Int? = null,
    val dBmSignal: Int? = null,
    val upSpeed: Int? = null,
    val downSpeed: Int? = null
)
