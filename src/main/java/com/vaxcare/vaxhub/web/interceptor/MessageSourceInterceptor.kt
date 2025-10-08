/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class MessageSourceInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header("MessageSource", "VaxMobile")
            .build()
        return chain.proceed(request)
    }
}
