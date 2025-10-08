/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.appointment

import android.content.Context
import android.content.Intent
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.args.PartDJobArgs
import com.vaxcare.vaxhub.model.partd.PartDCopayEvent
import com.vaxcare.vaxhub.model.partd.PartDCopayResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartDJob @Inject constructor(
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    companion object {
        const val PART_D_JOB_NAME = "PartDJob"
    }

    override suspend fun doWork(parameter: Any?) {
        (parameter as? PartDJobArgs)?.let { args ->
            args.context?.sendPartDEventBroadcast(args.patientVisitId, args.copays)
        }
    }

    private fun Context.sendPartDEventBroadcast(patientVisitId: Int?, copays: List<PartDCopayEvent>) {
        val copayResponses = copays.map { it.toCopayResponse() }
        sendBroadcast(
            Intent(Receivers.PART_D_ACTION).apply {
                putExtra(Receivers.PART_D_PATIENT_VISIT_ID, patientVisitId)
                putExtra(Receivers.PART_D_COPAYS, copayResponses.toTypedArray())
            }
        )
    }
}

private fun PartDCopayEvent.toCopayResponse() =
    PartDCopayResponse(
        ndc = ndc,
        productId = productId,
        copay = copay,
        eligibilityStatusCode = eligibilityStatusCode,
        requestStatus = requestStatus
    )
