/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.interceptor

import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.storage.preference.UserSessionManager
import com.vaxcare.vaxhub.service.NetworkMonitor
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class VaxHubIdentifier(
    private val localStorage: LocalStorage,
    private val sessionManager: UserSessionManager,
    private val networkMonitor: NetworkMonitor
) : Interceptor {
    companion object {
        private const val NO_USER_LOGGED_IN = "NO USER LOGGED IN"
        const val VAXHUB_IDENTIFIER_HEADER = "X-VaxHub-Identifier"
        const val VAXHUB_AI_TRACER_HEADER = "traceparent"
        const val VAXHUB_MOBILE_DATA_HEADER = "MobileData"
        const val USER_SESSION_ID = "UserSessionId"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header(VAXHUB_IDENTIFIER_HEADER, localStorage.getHeaderIdentifier())
            .header(VAXHUB_AI_TRACER_HEADER, getCorrelationId())
            .header(
                VAXHUB_MOBILE_DATA_HEADER,
                (networkMonitor.isCellular.value ?: false).toString()
            )
            .header(
                USER_SESSION_ID,
                sessionManager.getCurrentUserSessionId()?.toString() ?: NO_USER_LOGGED_IN
            )
            .build()
        return chain.proceed(request)
    }

    /**
     * Retrieve the correlation id
     *
     * @return the correlation id
     */
    private fun getCorrelationId(): String {
        val correlationId = UUID.randomUUID().toString().replace("-", "")
        val secondCorrelationId = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        return "00-$correlationId-$secondCorrelationId-01"
    }
}
