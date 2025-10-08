/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import android.content.Context
import android.os.Build
import com.vaxcare.core.model.BuildConfiguration
import com.vaxcare.core.report.ReportInfo
import com.vaxcare.core.report.ReportInfoImpl
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.analytics.AnalyticReportImpl
import com.vaxcare.core.report.analytics.AnalyticsTarget
import com.vaxcare.core.report.analytics.MetricDecorator
import com.vaxcare.core.report.crash.BaseUncaughtExceptionHandler
import com.vaxcare.core.report.crash.CrashReport
import com.vaxcare.core.report.di.AppCenter
import com.vaxcare.core.report.di.DataDog
import com.vaxcare.core.report.di.MixPanel
import com.vaxcare.core.storage.StorageInfo
import com.vaxcare.core.storage.StorageInfoImpl
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.storage.preference.UserSessionManager
import com.vaxcare.vaxhub.AppInfo
import com.vaxcare.vaxhub.AppInfoImpl
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.reporting.MetricDecoratorImpl
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.ui.VaxUncaughtExceptionHandler
import com.vaxcare.vaxhub.update.InAppUpdates
import com.vaxcare.vaxhub.update.InAppUpdatesImpl
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.HiltWorkManagerListenerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MHMetricDecorator

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MHAnalyticReport

@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {
    private const val MODEL_TYPE = "MobileHub"
    private val serial = try {
        Build.getSerial()
    } catch (e: Exception) {
        "NO_PERMISSION"
    }

    @Provides
    @Singleton
    fun provideBuildConfiguration(): BuildConfiguration =
        BuildConfiguration(
            debuggable = BuildConfig.DEBUG,
            buildType = BuildConfig.BUILD_TYPE,
            vhapiUrl = BuildConfig.VAX_VHAPI_URL
        )

    @Provides
    @Singleton
    fun provideStorageInfo(
        @ApplicationContext app: Context
    ): StorageInfo =
        StorageInfoImpl(
            applicationId = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
            deviceSerialNumber = serial,
            buildVariant = BuildConfig.BUILD_VARIANT,
            packageName = app.packageName,
            filesDirectoryPath = app.getExternalFilesDir(null)?.absolutePath ?: "",
            modelType = MODEL_TYPE
        )

    @Provides
    @Singleton
    fun provideReportInfo(): ReportInfo =
        ReportInfoImpl(
            applicationId = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
            buildVariant = BuildConfig.BUILD_VARIANT,
            appCenterKey = BuildConfig.APP_CENTER_KEY,
            mixpanelKey = BuildConfig.MIXPANEL_TOKEN
        )

    @Provides
    @Singleton
    fun provideAppInfo(
        @ApplicationContext app: Context
    ): AppInfo =
        AppInfoImpl(
            applicationId = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
            deviceSerialNumber = serial,
            appCenterKey = BuildConfig.APP_CENTER_KEY,
            buildVariant = BuildConfig.BUILD_VARIANT,
            packageName = app.packageName,
            filesDirectoryPath = app.getExternalFilesDir(null)?.absolutePath ?: "",
            mixpanelKey = BuildConfig.MIXPANEL_TOKEN,
            fileDirectory = app.filesDir
        )

    @Provides
    @Singleton
    fun provideBaseUncaughtExceptionHandler(report: CrashReport): BaseUncaughtExceptionHandler =
        VaxUncaughtExceptionHandler(report)

    @Provides
    @Singleton
    fun provideHiltWorkManagerListener(): HiltWorkManagerListener = HiltWorkManagerListenerImpl()

    @Provides
    @MHMetricDecorator
    @Singleton
    fun providesMetricDecorator(
        localStorage: LocalStorage,
        userSessionManager: UserSessionManager,
        networkMonitor: NetworkMonitor
    ): MetricDecorator = MetricDecoratorImpl(localStorage, userSessionManager, networkMonitor)

    @Provides
    @MHAnalyticReport
    @Singleton
    fun providesAnalyticsReport(
        @MixPanel mixPanelTarget: AnalyticsTarget,
        @AppCenter appCenterTarget: AnalyticsTarget,
        @MHMetricDecorator metricsDecorator: MetricDecorator,
        @DataDog dataDogTarget: AnalyticsTarget
    ): AnalyticReport =
        AnalyticReportImpl(
            mixPanelTarget = mixPanelTarget,
            appCenterTarget = appCenterTarget,
            metricsDecorator = metricsDecorator,
            dataDogTarget = dataDogTarget
        )

    @Provides
    @Singleton
    fun provideInAppUpdates(): InAppUpdates = InAppUpdatesImpl()
}
