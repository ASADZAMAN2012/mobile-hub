/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

enum class ServiceToPing {
    VAXCARE,
    GOOGLE,
    CODE_CORP,
    AZURE,
    APP_CENTER,
    MIXPANEL,
    DATADOG
}

data class ServiceToPingWithUrl(
    val serviceToPing: ServiceToPing,
    val url: String
)
