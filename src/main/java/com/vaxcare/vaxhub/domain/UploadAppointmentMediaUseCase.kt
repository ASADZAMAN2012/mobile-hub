/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.AppointmentMedia
import com.vaxcare.vaxhub.model.AppointmentMediaType
import com.vaxcare.vaxhub.model.metric.UploadMediaMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadAppointmentMediaUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    @MHAnalyticReport private val analyticReport: AnalyticReport
) {
    suspend fun uploadDriverLicenseFront(mediaPath: String, appointmentId: Int) =
        uploadFileSaveMetricAndDeleteFile(
            appointmentId = appointmentId,
            mediaPath = mediaPath,
            mediaType = AppointmentMediaType.DRIVERS_LICENSE_FRONT
        )

    suspend fun uploadInsuranceCardFront(mediaPath: String, appointmentId: Int) =
        uploadFileSaveMetricAndDeleteFile(
            appointmentId = appointmentId,
            mediaPath = mediaPath,
            mediaType = AppointmentMediaType.INSURANCE_CARD_FRONT
        )

    suspend fun uploadInsuranceCardBack(mediaPath: String, appointmentId: Int) =
        uploadFileSaveMetricAndDeleteFile(
            appointmentId = appointmentId,
            mediaPath = mediaPath,
            mediaType = AppointmentMediaType.INSURANCE_CARD_BACK
        )

    private suspend fun uploadFileSaveMetricAndDeleteFile(
        appointmentId: Int,
        mediaPath: String,
        mediaType: AppointmentMediaType
    ) {
        try {
            appointmentRepository.uploadAppointmentMedia(
                AppointmentMedia(
                    appointmentId = appointmentId,
                    mediaType = mediaType.value,
                    contentType = JPEG,
                    encodedBytes = getEncodedBytesFromMediaPath(mediaPath)
                )
            )
            analyticReport.saveMetric(
                UploadMediaMetric(
                    appointmentId = appointmentId,
                    success = true,
                    mediaType = mediaType
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload appointment media")
            analyticReport.saveMetric(
                UploadMediaMetric(
                    appointmentId = appointmentId,
                    success = false,
                    mediaType = mediaType
                )
            )
        } finally {
            deleteFile(mediaPath)
        }
    }

    private fun getEncodedBytesFromMediaPath(mediaPath: String): String {
        val bitmap = BitmapFactory.decodeFile(mediaPath)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(
            // format =
            Bitmap.CompressFormat.JPEG,
            // quality =
            FULL_IMAGE_QUALITY_PERCENTAGE,
            // stream =
            byteArrayOutputStream
        )
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        bitmap.recycle()

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun deleteFile(mediaPath: String) {
        var isDeleted = false
        var deleteRetries = 0

        while (!isDeleted && deleteRetries++ < MAX_NUMBER_OF_DELETE_RETRIES) {
            isDeleted = File(mediaPath).delete()
        }

        if (isDeleted) {
            Timber.i("file $mediaPath deleted")
        } else {
            Timber.e("unable to delete file $mediaPath")
        }
    }

    companion object {
        private const val JPEG = "image/jpeg"
        private const val FULL_IMAGE_QUALITY_PERCENTAGE = 100
        private const val MAX_NUMBER_OF_DELETE_RETRIES = 3
    }
}
