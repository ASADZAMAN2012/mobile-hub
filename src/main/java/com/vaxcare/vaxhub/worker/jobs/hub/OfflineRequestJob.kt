/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import android.content.ContentResolver
import com.squareup.moshi.Moshi
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.data.dao.OfflineRequestDao
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.di.MobileOkHttpClient
import com.vaxcare.vaxhub.model.OfflineRequest
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.MEDIA
import com.vaxcare.vaxhub.model.metric.OfflineRequestMetric
import com.vaxcare.vaxhub.web.constant.IS_CALLED_BY_JOB
import com.vaxcare.vaxhub.web.offline.OfflineRequestResponseHandler
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active job - Called to send all stored requests. Fires when hub is connected to internet.
 */
@Singleton
class OfflineRequestJob @Inject constructor(
    private val offlineRequestDao: OfflineRequestDao,
    @MobileMoshi private val moshi: Moshi,
    @MobileOkHttpClient private val httpClient: OkHttpClient,
    @MHAnalyticReport private val analyticReport: AnalyticReport,
    private val fileStorage: FileStorage,
    private val responseHandler: OfflineRequestResponseHandler
) : BaseVaxJob(analyticReport) {
    private val maxCursorSizeKb = 200000
    private var running = false

    override suspend fun doWork(parameter: Any?) {
        if (!running) {
            running = true
            val offlineRequestList = offlineRequestDao.getOfflineRequestList()
            if (offlineRequestList.any { it.bodySize > maxCursorSizeKb }) {
                Timber.e("Deleting Offline Requests where body sizes are > 2MB")
                val tooLargeRequestIds = offlineRequestList
                    .filter { it.bodySize > maxCursorSizeKb }
                    .map { it.id }
                offlineRequestDao.deleteOfflineRequestsByIds(tooLargeRequestIds)
            }

            if (offlineRequestList.isNotEmpty()) {
                analyticReport.saveMetric(OfflineRequestMetric(offlineRequestList))
            }

            // get OfflineRequests
            Timber.d("Getting OfflineRequests...")
            val offlineRequests = offlineRequestDao.getAllAsync()

            // fire OfflineRequests
            var successfulRequestCount = 0

            Timber.d("Firing OfflineRequests...")
            offlineRequests?.forEachIndexed { index, offlineRequest ->
                val requestNum = index + 1

                Timber.d("Firing OfflineRequest ${offlineRequest.requestUri} $requestNum...")
                try {
                    val resolver = parameter as? ContentResolver

                    val requestBody = if (offlineRequest.requestUri.contains(Regex(MEDIA))) {
                        fileStorage.popFileContents(offlineRequest.requestBody, resolver)
                    } else {
                        offlineRequest.requestBody
                    }

                    requestBody?.let { body ->
                        Timber.d("OfflineRequest $requestNum processing response...")
                        val response = fireRequestAndGetResponse(offlineRequest, body)

                        responseHandler.handleResponse(response)
                        if (response.isSuccessful) {
                            Timber.d("OfflineRequest $requestNum succeeded")
                            Timber.d("Removing OfflineRequest $requestNum from queue...")

                            offlineRequestDao.delete(listOf(offlineRequest))

                            successfulRequestCount += 1
                        }
                    } ?: run {
                        Timber.d("File not found for request: ${offlineRequest.requestUri}")
                        offlineRequestDao.delete(listOf(offlineRequest))
                    }
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "An exception occurred while replaying OfflineRequest $requestNum"
                    )
                }
            }

            running = false
            Timber.d("OfflineRequest work complete")
        }
    }

    private fun fireRequestAndGetResponse(offlineRequest: OfflineRequest, body: String): Response {
        val requestBuilder = Request.Builder()
            .url(offlineRequest.requestUri)
            .headers(
                moshi.adapter(Headers::class.java)
                    .fromJson(offlineRequest.requestHeaders)!!
            )
            .method(
                offlineRequest.requestMethod,
                RequestBody.create(
                    moshi.adapter(MediaType::class.java)
                        .fromJson(offlineRequest.contentType),
                    body
                )
            )

        if (!offlineRequest.requestHeaders.contains(IS_CALLED_BY_JOB)) {
            requestBuilder.headers(Headers.Builder().add(IS_CALLED_BY_JOB, "true").build())
        }

        return httpClient.newCall(requestBuilder.build()).execute()
    }
}
