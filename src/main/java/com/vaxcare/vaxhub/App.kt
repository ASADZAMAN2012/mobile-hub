/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.vaxcare.core.config.VaxCareConfig
import com.vaxcare.core.report.crash.CrashReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.log.VaxCareLog
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.service.DataDogManager
import com.vaxcare.vaxhub.service.ScannerManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * The main application class. We initialize the worker classes here
 * modules as necessary.
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject
    lateinit var crashReport: CrashReport

    @Inject
    lateinit var config: VaxCareConfig

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dataDogManager: DataDogManager

    @Inject
    lateinit var scannerManager: ScannerManager

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var localStorage: LocalStorage

    val wm: WorkManager by lazy {
        WorkManager.getInstance(this)
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        Timber.plant(
            VaxCareLog(
                BuildConfig.VERBOSE_LOG_ENABLED,
                BuildConfig.DEBUG_LOG_ENABLED,
                BuildConfig.INFO_LOG_ENABLED,
                BuildConfig.BUILD_TYPE,
                crashReport
            )
        )

        Timber.d("Display Density: ${resources.displayMetrics.density} ")
        Timber.d("Display Density Dpi: ${resources.displayMetrics.densityDpi}")
        Timber.d("Display Scaled Density: ${resources.displayMetrics.scaledDensity}")
        Timber.d(
            "Display Size Pixels: ${resources.displayMetrics.widthPixels} x ${resources.displayMetrics.heightPixels}"
        )
        Timber.d("Display Size DP: ${resources.displayMetrics.xdpi} x ${resources.displayMetrics.ydpi}")
        localStorage.clearUpdateSeverityCache()
        config.registerListener(dataDogManager)
        config.registerListener(scannerManager)
        config.refresh()
    }

    private fun LocalStorage.clearUpdateSeverityCache() {
        lastUpdateSeverity = null
        lastUpdateSeverityFetchDate = null
    }
}
