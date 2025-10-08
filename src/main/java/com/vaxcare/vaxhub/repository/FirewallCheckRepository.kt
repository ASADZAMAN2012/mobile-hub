/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import com.vaxcare.vaxhub.di.MobileOkHttpClient
import com.vaxcare.vaxhub.model.ServiceToPing
import com.vaxcare.vaxhub.model.ServiceToPingWithUrl
import com.vaxcare.vaxhub.web.PingApi
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

interface FirewallCheckRepository {
    suspend fun pingServices()
}

class FirewallCheckRepositoryImpl @Inject constructor(
    @MobileOkHttpClient private val httpClient: OkHttpClient,
    private val pingApi: PingApi,
    private val servicesToPingWithUrl: List<ServiceToPingWithUrl>,
) : FirewallCheckRepository {
    companion object {
        const val UNSUCCESSFUL_RESULT_CODE = -1
    }

    override suspend fun pingServices() {
        val results =
            servicesToPingWithUrl.map { service ->
                pingAndGetResponseCode(service)
            }

        pingApi.pingThirdparties(
            vaxcareResponseCode =
                results.find { it.serviceToPing == ServiceToPing.VAXCARE }?.resultCode
                    ?: UNSUCCESSFUL_RESULT_CODE,
            googleResponseCode =
                results.find { it.serviceToPing == ServiceToPing.GOOGLE }?.resultCode
                    ?: UNSUCCESSFUL_RESULT_CODE,
            codeCorpResponseCode =
                results.find { it.serviceToPing == ServiceToPing.CODE_CORP }?.resultCode
                    ?: UNSUCCESSFUL_RESULT_CODE,
            azureResponseCode =
                results.find { it.serviceToPing == ServiceToPing.AZURE }?.resultCode
                    ?: UNSUCCESSFUL_RESULT_CODE,
            appCenterResponseCode =
                results.find { it.serviceToPing == ServiceToPing.APP_CENTER }?.resultCode
                    ?: UNSUCCESSFUL_RESULT_CODE,
            mixpanelResponseCode =
                results.find { it.serviceToPing == ServiceToPing.MIXPANEL }?.resultCode
                    ?: UNSUCCESSFUL_RESULT_CODE,
            datadogResponseCode =
                results.find { it.serviceToPing == ServiceToPing.DATADOG }?.resultCode
                    ?: UNSUCCESSFUL_RESULT_CODE
        )
    }

    private fun pingAndGetResponseCode(serviceToPingWithUrl: ServiceToPingWithUrl): FirewallCheckResult =
        try {
            val request =
                Request
                    .Builder()
                    .url(serviceToPingWithUrl.url)
                    .build()
            httpClient.newCall(request).execute().use { response ->
                FirewallCheckResult(
                    serviceToPing = serviceToPingWithUrl.serviceToPing,
                    resultCode = response.code
                )
            }
        } catch (e: Exception) {
            FirewallCheckResult(
                serviceToPing = serviceToPingWithUrl.serviceToPing,
                resultCode = null
            )
        }
}

data class FirewallCheckResult(
    val serviceToPing: ServiceToPing,
    val resultCode: Int?
)
