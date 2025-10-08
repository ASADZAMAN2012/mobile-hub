/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import android.app.Application
import android.os.Build
import android.util.Log
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.BackPressureMitigation
import com.datadog.android.core.configuration.BackPressureStrategy
import com.datadog.android.core.configuration.BatchSize
import com.datadog.android.core.configuration.UploadFrequency
import com.datadog.android.log.Logger
import com.datadog.android.log.Logs
import com.datadog.android.log.LogsConfiguration
import com.datadog.android.ndk.NdkCrashReports
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.rum.configuration.VitalsUpdateFrequency
import com.datadog.android.rum.metric.interactiontonextview.TimeBasedInteractionIdentifier
import com.datadog.android.rum.metric.networksettled.TimeBasedInitialResourceIdentifier
import com.datadog.android.rum.tracking.MixedViewTrackingStrategy
import com.datadog.android.sessionreplay.ImagePrivacy
import com.datadog.android.sessionreplay.SessionReplay
import com.datadog.android.sessionreplay.SessionReplayConfiguration
import com.datadog.android.sessionreplay.TextAndInputPrivacy
import com.datadog.android.sessionreplay.TouchPrivacy
import com.datadog.android.sessionreplay.compose.ComposeExtensionSupport
import com.datadog.android.sessionreplay.material.MaterialExtensionSupport
import com.datadog.android.timber.DatadogTree
import com.vaxcare.core.config.VaxCareConfig
import com.vaxcare.core.config.VaxCareConfigListener
import com.vaxcare.core.config.VaxCareConfigResult
import com.vaxcare.core.model.FeatureFlag
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.StorageInfo
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.di.MHAnalyticReport
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataDogManager @Inject constructor(
    private val config: VaxCareConfig,
    @MHAnalyticReport private val analyticReport: AnalyticReport,
    private val localStorage: LocalStorage,
    private val storageInfo: StorageInfo,
    private val context: Application
) : VaxCareConfigListener {
    private val applicationId: String
        get() = config.dataDogLicense.applicationId

    private val clientToken: String
        get() = config.dataDogLicense.clientToken

    private val enabled: Boolean
        get() = config.dataDogLicense.enabled

    private val rumSampleRate: Float
        get() = config.dataDogLicense.rumSampleRate

    // will come from dataDogLicense/VHApi in a future commit
    private val sessionReplaySampleRate: Float
        get() = config.dataDogLicense.sessionReplaySampleRate

    private val site: DatadogSite
        get() = try {
            DatadogSite.valueOf(config.dataDogLicense.site)
        } catch (_: IllegalArgumentException) {
            Timber.e("Provided datadog site is invalid: ${config.dataDogLicense.site}")
            DatadogSite.US3
        }

    private var featureFlags: List<FeatureFlag> = listOf()

    private var isDataDogRunning: Boolean = false

    private val environmentName =
        if (BuildConfig.BUILD_VARIANT == "staging") "stg" else BuildConfig.BUILD_VARIANT

    private val appVariantName = BuildConfig.DATADOG_BUILD_VARIANT_NAME

    override fun onFetchFailure(e: Exception) {
        // do nothing, the exception is already logged in the composite
    }

    override fun onFetchSuccess(vaxCareConfigResult: VaxCareConfigResult) {
        featureFlags = vaxCareConfigResult.featureFlags
        if (vaxCareConfigResult.isDataDogUpdated || !isDataDogRunning) {
            restartDataDog()
        }
    }

    private fun restartDataDog() {
        stopDataDog()
        startDataDog()
    }

    private fun stopDataDog() {
        Timber.d("Stopping DataDog...")
        Datadog.stopInstance()
        isDataDogRunning = false
    }

    private fun startDataDog() {
        if (!enabled) {
            Timber.d("DataDog is not enabled.")
            return
        }

        Timber.d("Starting DataDog...")
        val configuration = com.datadog.android.core.configuration.Configuration.Builder(
            clientToken,
            environmentName,
            appVariantName
        )
            .useSite(site)
            .setBatchSize(BatchSize.SMALL)
            .setUploadFrequency(UploadFrequency.FREQUENT)
            .setBackpressureStrategy(
                BackPressureStrategy(
                    32,
                    { },
                    { },
                    BackPressureMitigation.DROP_OLDEST
                )
            )
            .build()

        Datadog.initialize(
            context = context,
            configuration = configuration,
            trackingConsent = TrackingConsent.GRANTED
        )

        val rumConfiguration = RumConfiguration.Builder(applicationId)
            .trackUserInteractions()
            .trackLongTasks()
            .setSessionSampleRate(rumSampleRate)
            .setVitalsUpdateFrequency(VitalsUpdateFrequency.AVERAGE)
            .setInitialResourceIdentifier(TimeBasedInitialResourceIdentifier())
            .setLastInteractionIdentifier(TimeBasedInteractionIdentifier())
            .useViewTrackingStrategy(MixedViewTrackingStrategy(trackExtras = true))
            .trackBackgroundEvents(true)
            .build()

        Rum.enable(rumConfiguration)

        NdkCrashReports.enable()

        val sessionReplayConfig = SessionReplayConfiguration.Builder(sessionReplaySampleRate)
            .addExtensionSupport(MaterialExtensionSupport())
            .addExtensionSupport(ComposeExtensionSupport())
            .setTextAndInputPrivacy(TextAndInputPrivacy.MASK_SENSITIVE_INPUTS)
            .setImagePrivacy(ImagePrivacy.MASK_NONE)
            .setTouchPrivacy(TouchPrivacy.SHOW)
            .build()
        SessionReplay.enable(sessionReplayConfig)

        // explicitly casting values to strings so they can be filtered in datadog
        // numbers are treated as performance metrics and can only be graphed with
        GlobalRumMonitor.get().addAttribute("androidSdk", "${Build.VERSION.SDK_INT}")
        GlobalRumMonitor.get().addAttribute("androidVersion", Build.VERSION.RELEASE)
        GlobalRumMonitor.get().addAttribute("assetTag", "-1")
        GlobalRumMonitor.get().addAttribute("clinicId", "${localStorage.clinicId}")
        GlobalRumMonitor.get().addAttribute("deviceSerialNumber", localStorage.deviceSerialNumber)
        GlobalRumMonitor.get().addAttribute("partnerId", "${localStorage.partnerId}")
        GlobalRumMonitor.get().addAttribute("version", "${storageInfo.versionCode}")
        GlobalRumMonitor.get().addAttribute("versionName", storageInfo.versionName)
        GlobalRumMonitor.get().addAttribute("modelType", storageInfo.modelType)
        if (localStorage.imei.isNotEmpty() && localStorage.iccid.isNotEmpty()) {
            GlobalRumMonitor.get().addAttribute("imei", localStorage.imei)
            GlobalRumMonitor.get().addAttribute("iccid", localStorage.iccid)
        }

        Datadog.setUserInfo("${localStorage.userId}", "", localStorage.userName)

        Datadog.setVerbosity(Log.INFO)
        val logsConfig = LogsConfiguration.Builder().build()
        Logs.enable(logsConfig)
        val logger = Logger.Builder()
            .setNetworkInfoEnabled(false)
            .setService("MobileHubLogs")
            .setLogcatLogsEnabled(false)
            .setRemoteSampleRate(100f)
            .setRemoteLogThreshold(Log.INFO)
            .setBundleWithTraceEnabled(false)
            .setBundleWithRumEnabled(true)
            .build()

        logger.addTag("serial number", localStorage.deviceSerialNumber)

        Timber.plant(Timber.DebugTree(), DatadogTree(logger))

        for (featureFlag in featureFlags) {
            GlobalRumMonitor.get().addFeatureFlagEvaluation(featureFlag.featureFlagName, true)
        }

        isDataDogRunning = true
    }
}
