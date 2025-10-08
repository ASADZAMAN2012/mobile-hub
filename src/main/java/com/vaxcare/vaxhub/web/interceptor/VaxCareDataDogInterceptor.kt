/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.interceptor

import com.datadog.android.okhttp.DatadogInterceptor
import com.datadog.android.trace.TracingHeaderType

// not paying for tracing, at this time.
class VaxCareDataDogInterceptor {
    fun createDataDogInterceptor(): DatadogInterceptor {
        val tracedHosts = mapOf(
            "vaxcare.com" to setOf(
                TracingHeaderType.DATADOG, TracingHeaderType.TRACECONTEXT
            )
        )
        return DatadogInterceptor.Builder(tracedHosts).build()
    }
}
