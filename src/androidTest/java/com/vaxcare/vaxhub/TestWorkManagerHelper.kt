/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.testing.WorkManagerTestInitHelper
import com.vaxcare.vaxhub.worker.DailyPeriodicWorker
import com.vaxcare.vaxhub.worker.HalfHourPeriodicWorker
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.ThreeHourPeriodicWorker
import com.vaxcare.vaxhub.worker.WorkerBuilder

class TestWorkManagerHelper {
    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Initialize the test workManager and kick off daily jobs
     *
     * @param workerFactory
     */
    fun startAllWorkers(workerFactory: HiltWorkerFactory) {
        initializeWorkManager(workerFactory)
        startWorkers()
    }

    /**
     * Cancel all workers and restart them. Used after syncing to a partner/clinic
     */
    fun resetAllWorkers() {
        stopWorkers()
        startWorkers()
    }

    /**
     * Hook for Job Listener
     *
     * @param request job request
     */
    private fun handleUniqueJobQueued(request: WorkRequest) {
        try {
            WorkManagerTestInitHelper.getTestDriver(context.applicationContext).let { testDriver ->
                testDriver?.setAllConstraintsMet(request.id)
                testDriver?.setPeriodDelayMet(request.id)
                testDriver?.setInitialDelayMet(request.id)
            }
        } catch (e: Exception) {
            Log.e("TestWorkManagerHelper", "Exception caught: ${e.message}", e)
        }
    }

    /**
     * Required for any instrumentation test to run, but doesn't actually kick off any jobs
     *
     * @param workerFactory
     */
    fun initializeWorkManager(workerFactory: HiltWorkerFactory, listener: HiltWorkManagerListener? = null) {
        listener?.handleJobQueued = ::handleUniqueJobQueued
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    /**
     * Kicks off all periodic jobs
     */
    private fun startWorkers() {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context.applicationContext)
        val dailyJobs = DailyPeriodicWorker.buildPeriodicWorker(
            workManager,
            WorkerBuilder.networkConstraints
        )
        val threeHourJobs = ThreeHourPeriodicWorker.buildPeriodicWorker(
            workManager,
            WorkerBuilder.networkConstraints
        )
        val halfHourJobs = HalfHourPeriodicWorker.buildPeriodicWorker(
            workManager,
            WorkerBuilder.networkConstraints
        )

        testDriver?.setAllConstraintsMet(dailyJobs.id)
        testDriver?.setPeriodDelayMet(dailyJobs.id)
        testDriver?.setAllConstraintsMet(threeHourJobs.id)
        testDriver?.setAllConstraintsMet(halfHourJobs.id)
    }

    /**
     * Cancel all workers
     */
    private fun stopWorkers() {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelAllWork()
    }
}
