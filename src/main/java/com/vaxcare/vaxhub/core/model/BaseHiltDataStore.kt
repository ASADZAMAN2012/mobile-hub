/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

@file:Suppress("UNCHECKED_CAST")

package com.vaxcare.vaxhub.core.model

import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobileOkHttpClient
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseHiltDataStore @Inject constructor(
    networkMonitor: NetworkMonitor,
    @MHAnalyticReport analytics: AnalyticReport,
    globalDestinations: GlobalDestinations,
    scannerManager: ScannerManager,
    captureFlowDestination: CaptureFlowDestination,
    @MobileOkHttpClient okHttpClient: OkHttpClient,
    @MobilePicasso picasso: Picasso
) {
    private val store = mutableMapOf<String?, Any>()

    /*
    The canonicalName does not work for all instances. Interfaces for example: AnalyticReport -
    the implementation class is called AnalyticReportImpl - so the name will be mismatched.
     */
    init {
        store.apply {
            put(NetworkMonitor::class.java.canonicalName, networkMonitor)
            put(AnalyticReport::class.java.canonicalName, analytics)
            put(GlobalDestinations::class.java.canonicalName, globalDestinations)
            put(ScannerManager::class.java.canonicalName, scannerManager)
            put(CaptureFlowDestination::class.java.canonicalName, captureFlowDestination)
            put(OkHttpClient::class.java.canonicalName, okHttpClient)
            put(Picasso::class.java.canonicalName, picasso)
        }
    }

    /**
     * Gets the stored dependency using class canonicalName or class instance
     *
     * @param T type to return
     * @param clazz wrapper class of T
     * @return dependency instance of T
     */
    operator fun <T> get(clazz: Class<T>): T {
        val key = clazz.canonicalName
        val dep = store[key]
        return if (clazz.isInstance(dep)) {
            dep as T
        } else {
            store.filter { clazz.isInstance(it.value) }.firstNotNullOfOrNull { it.value as T }
                ?: run {
                    throw Exception("Class: $key not found for lazy inject!")
                }
        }
    }
}
