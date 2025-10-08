/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.ABANDON_APPT
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.CHECKOUT_APPT
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.LOT_CREATION
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.MEDIA
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.PATIENT
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.UNORDERED_DOSE_REASON
import com.vaxcare.vaxhub.model.OfflineRequestInfo
import java.time.LocalDateTime

data class OfflineRequestMetric(
    val offlineRequestList: List<OfflineRequestInfo>?
) : BaseMetric() {
    override var eventName: String = "OfflineRequestMetric"

    private var checkoutCount = 0
    private var checkoutMaxSize = 0
    private var checkoutOldestRequestDate: LocalDateTime? = null
    private var mediaCount = 0
    private var mediaMaxSize = 0
    private var mediaOldestRequestDate: LocalDateTime? = null
    private var patchPatientCount = 0
    private var patchPatientMaxSize = 0
    private var patchPatientOldestRequestDate: LocalDateTime? = null
    private var abandonAppointmentCount = 0
    private var abandonAppointmentMaxSize = 0
    private var abandonAppointmentOldestRequestDate: LocalDateTime? = null
    private var lotCreationCount = 0
    private var lotCreationMaxSize = 0
    private var lotCreationRequestDate: LocalDateTime? = null
    private var unorderedDoseReasonCount = 0
    private var unorderedDoseReasonMaxSize = 0
    private var unorderedDoseReasonRequestDate: LocalDateTime? = null

    override fun toMap(): MutableMap<String, String> {
        offlineRequestList?.filter {
            it.requestUri.contains(Regex(CHECKOUT_APPT))
        }?.apply {
            checkoutCount = size
            checkoutMaxSize = this.maxByOrNull { it.bodySize }?.bodySize ?: 0
            this.filter { it.originalDateTime != null }.apply {
                checkoutOldestRequestDate =
                    this.minByOrNull { it.originalDateTime!! }?.originalDateTime
            }
        }

        offlineRequestList?.filter {
            it.requestUri.contains(Regex(MEDIA))
        }?.apply {
            mediaCount = size
            mediaMaxSize = this.maxByOrNull { it.bodySize }?.bodySize ?: 0
            this.filter { it.originalDateTime != null }.apply {
                mediaOldestRequestDate =
                    this.minByOrNull { it.originalDateTime!! }?.originalDateTime
            }
        }

        offlineRequestList?.filter {
            it.requestUri.contains(Regex(PATIENT))
        }?.apply {
            patchPatientCount = size
            patchPatientMaxSize = this.maxByOrNull { it.bodySize }?.bodySize ?: 0
            this.filter { it.originalDateTime != null }.apply {
                patchPatientOldestRequestDate =
                    this.minByOrNull { it.originalDateTime!! }?.originalDateTime
            }
        }

        offlineRequestList?.filter {
            it.requestUri.contains(Regex(ABANDON_APPT))
        }?.apply {
            abandonAppointmentCount = size
            abandonAppointmentMaxSize = this.maxByOrNull { it.bodySize }?.bodySize ?: 0
            this.filter { it.originalDateTime != null }.apply {
                abandonAppointmentOldestRequestDate =
                    this.minByOrNull { it.originalDateTime!! }?.originalDateTime
            }
        }

        offlineRequestList?.filter {
            it.requestUri.contains(Regex(LOT_CREATION))
        }?.apply {
            lotCreationCount = size
            lotCreationMaxSize = this.maxByOrNull { it.bodySize }?.bodySize ?: 0
            this.filter { it.originalDateTime != null }.apply {
                lotCreationRequestDate =
                    this.minByOrNull { it.originalDateTime!! }?.originalDateTime
            }
        }

        offlineRequestList?.filter {
            it.requestUri.contains(Regex(UNORDERED_DOSE_REASON))
        }?.apply {
            unorderedDoseReasonCount = size
            unorderedDoseReasonMaxSize = this.maxByOrNull { it.bodySize }?.bodySize ?: 0
            this.filter { it.originalDateTime != null }.apply {
                unorderedDoseReasonRequestDate =
                    this.minByOrNull { it.originalDateTime!! }?.originalDateTime
            }
        }

        return super.toMap().toMutableMap().apply {
            put("isVaxCare3", "true")
            put("checkoutRequestCount", checkoutCount.toString())
            put("checkoutRequestMaxSize", checkoutMaxSize.toString())
            put("checkoutRequestOldestDate", checkoutOldestRequestDate.toString())
            put("mediaRequestCount", mediaCount.toString())
            put("mediaRequestMaxSize", mediaMaxSize.toString())
            put("mediaRequestOldestDate", mediaOldestRequestDate.toString())
            put("patchPatientRequestCount", patchPatientCount.toString())
            put("patchPatientRequestMaxSize", patchPatientMaxSize.toString())
            put("patchPatientRequestOldestDate", patchPatientOldestRequestDate.toString())
            put("abandonAppointmentCount", abandonAppointmentCount.toString())
            put("abandonAppointmentMaxSize", abandonAppointmentMaxSize.toString())
            put(
                "abandonAppointmentOldestRequestDate",
                abandonAppointmentOldestRequestDate.toString()
            )
            put("lotCreationCount", lotCreationCount.toString())
            put("lotCreationMaxSize", lotCreationMaxSize.toString())
            put("lotCreationRequestDate", lotCreationRequestDate.toString())
            put("unorderedDoseReasonCount", unorderedDoseReasonCount.toString())
            put("unorderedDoseReasonMaxSize", unorderedDoseReasonMaxSize.toString())
            put("unorderedDoseReasonRequestDate", unorderedDoseReasonRequestDate.toString())
        }
    }
}
