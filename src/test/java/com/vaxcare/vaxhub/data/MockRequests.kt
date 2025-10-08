/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.web.constant.IGNORE_OFFLINE_STORAGE
import okhttp3.Request

object MockRequests {
    val checkoutRequestIgnoreHeader = Request
        .Builder()
        .header(IGNORE_OFFLINE_STORAGE, "true")
        .url("http://vhapi.vaxcare.com/api/patients/appointment/123/checkout")
        .build()
    val checkoutRequest = Request
        .Builder()
        .url("http://vhapi.vaxcare.com/api/patients/appointment/123/checkout")
        .build()
}
