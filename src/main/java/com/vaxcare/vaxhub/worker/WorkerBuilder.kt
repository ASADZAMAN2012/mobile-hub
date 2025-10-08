/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager

object WorkerBuilder {
    val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * All the workers that are required to run in a periodic manner
     *
     * @param context
     */
    fun initialize(context: Context) {
        val wm = WorkManager.getInstance(context)
        DailyPeriodicWorker.buildPeriodicWorker(wm, networkConstraints)
        ThreeHourPeriodicWorker.buildPeriodicWorker(wm, networkConstraints)
        HalfHourPeriodicWorker.buildPeriodicWorker(wm, networkConstraints)
    }

    fun destroy(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWork()
    }
}
