/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.BaseMetric
import timber.log.Timber

abstract class BaseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    protected val wm by lazy { WorkManager.getInstance(context) }
    protected abstract val analyticReport: AnalyticReport

    protected fun retry(
        payload: Data? = null,
        exception: Exception = Exception(),
        message: String? = null,
        metric: BaseMetric? = null
    ): Result {
        Timber.e(exception, message ?: "Error running worker")

        if (runAttemptCount <= 2) {
            return Result.retry()
        }

        metric?.let { analyticReport.saveMetric(it) }

        return if (payload != null) {
            Result.failure(payload)
        } else {
            Result.failure()
        }
    }
}
