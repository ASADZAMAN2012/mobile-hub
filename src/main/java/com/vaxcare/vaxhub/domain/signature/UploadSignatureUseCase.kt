/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.domain.signature

import android.content.ContentResolver
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.AppointmentMedia
import com.vaxcare.vaxhub.model.AppointmentMediaType
import com.vaxcare.vaxhub.model.metric.UploadMediaMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadSignatureUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    @MHAnalyticReport private val analyticReport: AnalyticReport,
    private val fileStorage: FileStorage
) {
    companion object {
        private const val SIGNATURE_CONTENT_TYPE = "image/jpeg"
    }

    suspend operator fun invoke(
        appointmentId: Int,
        fileUri: String,
        contentResolver: ContentResolver
    ) {
        try {
            val base64Signature = fileStorage.popFileContents(
                fileUri,
                contentResolver
            )

            appointmentRepository.uploadAppointmentMedia(
                AppointmentMedia(
                    appointmentId = appointmentId,
                    mediaType = AppointmentMediaType.SIGNATURE.value,
                    contentType = SIGNATURE_CONTENT_TYPE,
                    encodedBytes = base64Signature!!
                )
            )
            analyticReport.saveMetric(
                UploadMediaMetric(
                    appointmentId = appointmentId,
                    success = true,
                    mediaType = AppointmentMediaType.SIGNATURE
                )
            )
        } catch (e: Exception) {
            Timber.e(e)
            analyticReport.saveMetric(
                UploadMediaMetric(
                    appointmentId = appointmentId,
                    success = false,
                    mediaType = AppointmentMediaType.SIGNATURE
                )
            )
        }
    }
}
