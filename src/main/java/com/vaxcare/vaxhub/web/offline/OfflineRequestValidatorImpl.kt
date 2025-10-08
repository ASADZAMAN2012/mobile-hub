/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.offline

import android.net.Uri
import com.squareup.moshi.Moshi
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.vaxhub.AppInfo
import com.vaxcare.vaxhub.model.OfflineRequest
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.ABANDON_APPT
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.CHECKOUT_APPT
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.LOT_CREATION
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.MEDIA
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.PATIENT
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.UNORDERED_DOSE_REASON
import com.vaxcare.vaxhub.web.constant.IGNORE_OFFLINE_STORAGE
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okio.Buffer
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OfflineRequestValidatorImpl(
    private val moshi: Moshi,
    private val fileStorage: FileStorage,
    private val appInfo: AppInfo
) : OfflineRequestValidator {
    private val interceptOfflineRequestUrls: List<String> by lazy {
        listOf(CHECKOUT_APPT, MEDIA, PATIENT, ABANDON_APPT, LOT_CREATION, UNORDERED_DOSE_REASON)
    }

    override fun validateRequest(request: Request): OfflineRequest? {
        val requestUri = request.url.toUri().toASCIIString()

        return when {
            interceptOfflineRequestUrls.find { requestUri.contains(Regex(it)) } == null ||
                (request.header(IGNORE_OFFLINE_STORAGE) == "true") ||
                (requestUri.contains(Regex(PATIENT)) && request.method != "PATCH") ||
                (requestUri.contains(Regex(LOT_CREATION)) && request.method != "POST") -> {
                Timber.d("We don't need to intercept this request $requestUri")
                null
            }

            else -> {
                Timber.d("Found offline request $requestUri")
                val buffer = Buffer()
                request.body?.writeTo(buffer)

                var requestBody = buffer.buffer.readUtf8()
                if (requestUri.contains(Regex(MEDIA))) {
                    // requestBody is a large blob. Need to move this to a file
                    val fileName = "${LocalDate.now()}_${UUID.randomUUID()}"

                    val path = fileStorage.createFile(
                        data = requestBody.byteInputStream(),
                        deleteIfExisting = false,
                        appInfo.fileDirectory!!.absolutePath,
                        fileName
                    )

                    requestBody = Uri.fromFile(File(path)).toString()
                }

                OfflineRequest(
                    // Need to ensure that the id is unique
                    (requestUri + requestBody).hashCode(),
                    requestUri,
                    request.method,
                    moshi.adapter(Headers::class.java).toJson(request.headers),
                    moshi.adapter(MediaType::class.java)
                        .toJson(request.body?.contentType()),
                    requestBody,
                    LocalDateTime.now()
                )
            }
        }
    }
}
