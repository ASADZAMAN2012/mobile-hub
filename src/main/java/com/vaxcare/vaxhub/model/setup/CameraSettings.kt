/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.setup

import com.codecorp.CDCamera
import com.codecorp.CDCamera.CDFocus
import com.vaxcare.core.config.VaxCareConfig
import com.vaxcare.core.config.VaxCareConfigListener
import com.vaxcare.core.config.VaxCareConfigResult
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScanMetric
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.metric.AbnormalScanMetric
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraSettings @Inject constructor(
    private val config: VaxCareConfig,
    @MHAnalyticReport private val report: AnalyticReport
) {
    val cameraApi: CDCamera.CDCameraAPI = CDCamera.CDCameraAPI.camera2
    val resolution: CDCamera.CDResolution = CDCamera.CDResolution.res1920x1080
    val focus: CDFocus = CDFocus.auto
    val agcMode = CDCamera.CDAGCMode.DISABLE
    val duplicateScanTimeoutMs = 2000
    val enableAudio = true
    val enableTorch: CDCamera.CDTorch = CDCamera.CDTorch.off

    val customerId: String
        get() = config.codeCorplicense.customerId

    val license: String
        get() = config.codeCorplicense.key

    val expiration: LocalDateTime
        get() = config.codeCorplicense.expiration

    fun checkValues(listener: VaxCareConfigListener, isForceRefresh: Boolean = false) {
        if (isForceRefresh || LocalDateTime.now() > expiration) {
            config.registerListener(listener)
            config.refresh()
        } else {
            listener.onFetchSuccess(
                VaxCareConfigResult().apply { isCodeCorpUpdated = false }
            )
        }
    }

    fun reportBarcode(rawBarcode: String, symbology: String) {
        report.saveMetric(
            ScanMetric(
                rawBarcode = rawBarcode,
                symbology = symbology
            )
        )
    }

    fun reportAbnormalBarcode(rawBarcode: String, abnormality: String) {
        report.saveMetric(
            AbnormalScanMetric(
                barcode = rawBarcode,
                abnormality = abnormality
            )
        )
    }
}
