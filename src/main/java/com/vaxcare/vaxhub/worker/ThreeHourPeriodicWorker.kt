/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.worker.JobExecutor
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.core.BaseWorker
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.ThreeHourJobs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class ThreeHourPeriodicWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    @MHAnalyticReport override val analyticReport: AnalyticReport
) : BaseWorker(context, params) {
    companion object {
        private const val WORKER_NAME = "${BuildConfig.APPLICATION_ID}.WORKER.THREE_HOUR"

        /**
         * Builds a periodic worker and queues it up in the work manager.
         *
         * @param wm WorkManager instance
         */
        fun buildPeriodicWorker(wm: WorkManager, networkConstraints: Constraints): PeriodicWorkRequest {
            val work = PeriodicWorkRequestBuilder<ThreeHourPeriodicWorker>(3, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORKER_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
            return work
        }
    }

    @Inject
    @ThreeHourJobs
    lateinit var executor: JobExecutor

    @Inject
    lateinit var localStorage: LocalStorage

    override suspend fun doWork(): Result =
        try {
            Timber.d("Three Hour Worker started")
            if (localStorage.clinicId == 0L) {
                Timber.e("ThreeHourPeriodicWorker failure - ClinicID is not set up")
                Result.failure()
            } else {
                executor.executeJobs(WeakReference(applicationContext))
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e)
            retry(exception = e, message = "Error with ThreeHourPeriodicWorker")
        }
}
