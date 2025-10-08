/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.enums.NetworkStatus

data class CheckoutStartMetric(
    val networkStatus: NetworkStatus
) : CheckoutMetric(null, "Start") {
    private val connectivityError = when (networkStatus) {
        NetworkStatus.CONNECTED -> "None"
        NetworkStatus.CONNECTED_NO_INTERNET, NetworkStatus.DISCONNECTED -> "WiFi Offline"
        NetworkStatus.CONNECTED_VAXCARE_UNREACHABLE -> "System Offline"
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("Experience", "VaxCare 3.0")
            put("ConnectivityError", connectivityError)
        }
    }
}
