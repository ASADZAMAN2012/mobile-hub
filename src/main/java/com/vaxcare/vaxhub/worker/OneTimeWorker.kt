/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.core.BaseWorker
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.di.args.AppointmentChangedJobArgs
import com.vaxcare.vaxhub.di.args.InsertLotNumbersJobArgs
import com.vaxcare.vaxhub.di.args.PartDJobArgs
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import com.vaxcare.vaxhub.model.enums.LotNumberSources
import com.vaxcare.vaxhub.model.partd.PartDCopayEvent
import com.vaxcare.vaxhub.web.typeadapter.TimeAdapter
import com.vaxcare.vaxhub.worker.jobs.PingJob
import com.vaxcare.vaxhub.worker.jobs.PingJob.Companion.PING_JOB_NAME
import com.vaxcare.vaxhub.worker.jobs.appointment.AppointmentChangedJob
import com.vaxcare.vaxhub.worker.jobs.appointment.PartDJob
import com.vaxcare.vaxhub.worker.jobs.appointment.PartDJob.Companion.PART_D_JOB_NAME
import com.vaxcare.vaxhub.worker.jobs.hub.DiagnosticJob
import com.vaxcare.vaxhub.worker.jobs.hub.DiagnosticJob.Companion.DIAGNOSTIC_JOB_NAME
import com.vaxcare.vaxhub.worker.jobs.hub.LocationJob
import com.vaxcare.vaxhub.worker.jobs.hub.OfflineRequestJob
import com.vaxcare.vaxhub.worker.jobs.hub.SessionCacheCleanupJob
import com.vaxcare.vaxhub.worker.jobs.inventory.InsertLotNumbersJob
import com.vaxcare.vaxhub.worker.jobs.inventory.SimpleOnHandInventoryJob
import com.vaxcare.vaxhub.worker.jobs.order.OrderGroupChangedJob
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class OneTimeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    @MHAnalyticReport override val analyticReport: AnalyticReport
) : BaseWorker(context, params) {
    companion object {
        private const val ONETIME_WORKER =
            "${BuildConfig.APPLICATION_ID}.WORKER.ONETIME.WORKER."

        /**
         * Enqueue the worker to be executed in the next second
         *
         * @param wm the [WorkManager] instance
         */
        fun buildOneTimeUniqueWorker(
            wm: WorkManager,
            parameters: OneTimeParams,
            listener: HiltWorkManagerListener? = null
        ) {
            val inputArgs = mutableMapOf<String, Any>("jobName" to parameters.name)
            inputArgs.putAll(parameters.args)
            val databuilder = Data.Builder().putAll(inputArgs)
            val suffix = if (parameters.shouldAddUniqueId) UUID.randomUUID() else ""
            val jobId = "$ONETIME_WORKER${parameters.name}$suffix"
            val requestBuilder = OneTimeWorkRequestBuilder<OneTimeWorker>()
                .setInitialDelay(1000, TimeUnit.MILLISECONDS)
                .setInputData(databuilder.build())

            parameters.constraints?.let { constraints ->
                requestBuilder.setConstraints(constraints)
            }

            val oneTimeWorkRequest = requestBuilder.build()

            wm.enqueueUniqueWork(
                jobId,
                ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest
            )

            listener?.onJobQueued(oneTimeWorkRequest)
        }
    }

    @Inject
    lateinit var appointmentChangedJob: AppointmentChangedJob

    @Inject
    lateinit var insertLotJob: InsertLotNumbersJob

    @Inject
    lateinit var locationJob: LocationJob

    @Inject
    lateinit var orderGroupChangedJob: OrderGroupChangedJob

    @Inject
    lateinit var offlineRequestJob: OfflineRequestJob

    @Inject
    lateinit var simpleOnHandInventoryJob: SimpleOnHandInventoryJob

    @Inject
    lateinit var sessionCacheCleanupJob: SessionCacheCleanupJob

    @Inject
    lateinit var diagnosticJob: DiagnosticJob

    @Inject
    lateinit var pingJob: PingJob

    @Inject
    lateinit var partDJob: PartDJob

    @MobileMoshi @Inject lateinit var moshi: Moshi

    override suspend fun doWork(): Result {
        var params: Any? = null
        val job = when (inputData.getString("jobName")) {
            "AppointmentChangedJob" -> {
                val reasonOrdinal = inputData.getInt("ChangeReason", -1)
                val typeOrdinal = inputData.getInt("ChangeType", -1)
                params = AppointmentChangedJobArgs(
                    appointmentId = inputData.getInt("AppointmentId", -1),
                    changeReason = AppointmentChangeReason.fromInt(reasonOrdinal),
                    changeType = AppointmentChangeType.fromInt(typeOrdinal),
                    context = applicationContext
                )
                appointmentChangedJob
            }

            "LocationJob" -> locationJob
            "InsertLotNumbersJob" -> {
                params = InsertLotNumbersJobArgs(
                    lotNumberName = inputData.getString("LotNumberName"),
                    epProductId = inputData.getString("productId")?.toInt(),
                    expiration = TimeAdapter().stringToLocalDate(
                        inputData.getString("expiration") ?: ""
                    ),
                    source = inputData.getString("source")?.toInt()
                        ?: LotNumberSources.ManualEntry.id
                )
                insertLotJob
            }

            "OrderGroupChangedJob" -> {
                params = inputData.getString("OrderGroupNumber")
                orderGroupChangedJob
            }

            "OfflineRequestJob" -> {
                params = applicationContext.contentResolver
                offlineRequestJob
            }

            DIAGNOSTIC_JOB_NAME -> diagnosticJob

            "SimpleOnHandInventoryJob" -> simpleOnHandInventoryJob
            "SessionCacheCleanupJob" -> sessionCacheCleanupJob
            PING_JOB_NAME -> pingJob
            PART_D_JOB_NAME -> {
                val copays = inputData.getString("Copays")?.let { raw ->
                    val listType =
                        Types.newParameterizedType(MutableList::class.java, PartDCopayEvent::class.java)
                    moshi.adapter<List<PartDCopayEvent>>(listType).fromJson(raw)
                } ?: emptyList()

                params = PartDJobArgs(
                    patientVisitId = inputData.getInt("PatientVisitId", -1),
                    copays = copays,
                    context = applicationContext
                )

                partDJob
            }

            else -> null
        }
        job?.execute(params)

        return Result.success()
    }
}

/**
 * Parameters for specific on demand jobs with optional parameters
 *
 * @property name name of VaxJob
 * @property args args for VaxJob
 * @property shouldAddUniqueId whether to add a UUID to the suffix
 * @property constraints needed for worker to run
 */
sealed class OneTimeParams(
    val name: String,
    val args: Map<String, Any>,
    val shouldAddUniqueId: Boolean = true,
    val constraints: Constraints? = null
) {
    data class AppointmentChanged(
        val params: AppointmentChangedJobArgs = AppointmentChangedJobArgs()
    ) : OneTimeParams(
            name = "AppointmentChangedJob",
            args = params.toMap(),
            constraints = WorkerBuilder.networkConstraints
        )

    data class InsertLotNumbers(
        val params: InsertLotNumbersJobArgs = InsertLotNumbersJobArgs()
    ) : OneTimeParams(
            name = "InsertLotNumbersJob",
            args = params.toMap()
        )

    object Location : OneTimeParams(
        name = "LocationJob",
        args = mapOf(),
        constraints = WorkerBuilder.networkConstraints
    )

    object SimpleOnHand : OneTimeParams(
        name = "SimpleOnHandInventoryJob",
        args = mapOf(),
        constraints = WorkerBuilder.networkConstraints
    )

    data class OrderGroupChanged(val orderGroupNumber: String) : OneTimeParams(
        name = "OrderGroupChangedJob",
        args = mapOf("OrderGroupNumber" to orderGroupNumber),
        constraints = WorkerBuilder.networkConstraints
    )

    object OfflineRequest : OneTimeParams(
        name = "OfflineRequestJob",
        args = emptyMap(),
        shouldAddUniqueId = false,
        constraints = WorkerBuilder.networkConstraints
    )

    object SessionCacheCleanup : OneTimeParams(
        name = "SessionCacheCleanupJob",
        args = mapOf()
    )

    object DiagnosticJob : OneTimeParams(
        name = DIAGNOSTIC_JOB_NAME,
        args = emptyMap(),
        constraints = WorkerBuilder.networkConstraints,
    )

    object PingJob : OneTimeParams(
        name = PING_JOB_NAME,
        args = emptyMap(),
        constraints = WorkerBuilder.networkConstraints,
    )

    data class PartD(
        val params: PartDJobArgs = PartDJobArgs()
    ) : OneTimeParams(
            name = PART_D_JOB_NAME,
            args = params.toMap()
        )
}
