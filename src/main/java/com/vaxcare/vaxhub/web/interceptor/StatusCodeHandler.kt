/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.interceptor

import com.vaxcare.vaxhub.data.dao.OfflineRequestDao
import com.vaxcare.vaxhub.di.MobileOfflineValidator
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.web.offline.OfflineRequestValidator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusCodeHandler @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val offlineRequestDao: OfflineRequestDao,
    @MobileOfflineValidator private val offlineRequestValidator: OfflineRequestValidator
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (e.message != "Canceled") {
                Timber.d("An IOException occurred while attempting to make a request, ${e.printStackTrace()}")
                val url = request.url.toUri().toASCIIString()
                Timber.d("URL: $url")
                storeOfflineRequest(request)
                throw e
            } else {
                throw e
            }
        }

        if (request.url.toUri().toASCIIString().contains("vaxcare.com")) {
            networkMonitor.setResponseCode(response.code)

            if (!response.isSuccessful) {
                Timber.d("A request completed while offline")
                storeOfflineRequest(request)
            }
        }

        return response
    }

    private fun storeOfflineRequest(request: Request) {
        offlineRequestValidator.validateRequest(request = request)?.let { offlineRequest ->
            offlineRequestDao.insert(listOf(offlineRequest))
            Timber.d("OfflineRequest stored successfully")
        }
    }
}
