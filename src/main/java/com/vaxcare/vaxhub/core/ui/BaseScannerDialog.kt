/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.vaxcare.core.ui.extension.animateFadeIn
import com.vaxcare.vaxhub.core.extension.clipOutlineCornerRadius
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.scanner.ScannerState
import timber.log.Timber

abstract class BaseScannerDialog<VB : ViewBinding> : BaseDialog<VB>() {
    protected abstract val scannerManager: ScannerManager

    protected open val scan1d = false
    protected open val scan2d = true
    private var scannerActivated = false

    override fun init(view: View, savedInstanceState: Bundle?) {
        scannerManager.state.observe(viewLifecycleOwner) { state ->
            Timber.d("Scanner State: $state")
            when (state) {
                is ScannerState.Activation -> {
                    scannerActivated = state.success
                    startScannerManager(scannerActivated)
                }

                ScannerState.Refreshed,
                ScannerState.Started -> refreshScanner()

                ScannerState.Uninitialized -> scannerManager.initializeScanner()
                else -> Timber.d("Scanner State: $state")
            }
        }

        scannerManager.configureScanner(scan1d, scan2d, ScannerManager.ScanType.DOSE)
    }

    private fun startScannerManager(activated: Boolean) {
        try {
            val scannerViewport = baseDialogProperties.scannerViewport?.let {
                view?.findViewById(it) as? ViewGroup
            }

            val scannerPreview = baseDialogProperties.scannerPreview?.let {
                view?.findViewById(it) as? ViewGroup
            }

            if (activated) {
                refreshScannerView(scannerViewport, scannerPreview)
                onScannerLicenseValid()
            } else {
                onScannerLicenseInvalid()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting scanner!")
        }
    }

    private fun refreshScanner() {
        val scannerViewport = baseDialogProperties.scannerViewport?.let {
            view?.findViewById(it) as? ViewGroup
        }
        val scannerPreview = baseDialogProperties.scannerPreview?.let {
            view?.findViewById(it) as? ViewGroup
        }
        refreshScannerView(scannerViewport, scannerPreview)
        onScannerLicenseValid()
    }

    private fun refreshScannerView(viewport: ViewGroup?, preview: ViewGroup?) {
        viewport?.apply {
            clipOutlineCornerRadius()
            animateFadeIn()
            children.filter { it != preview }.forEach { it.isVisible = true }
        }

        scannerManager.startScanner()
        scannerManager.getCameraPreview()?.let { cameraPreview ->
            preview?.apply {
                removeAllViews()
                (cameraPreview.parent as? ViewGroup)?.removeView(cameraPreview)
                addView(cameraPreview, 0)
                cameraPreview.invalidate()
            }
        }
    }

    override fun onResume() {
        Timber.i("onResume")
        super.onResume()

        scannerManager.refreshScanner(scan1d, scan2d)
    }

    override fun onPause() {
        Timber.i("onPause")
        stopScannerManager()

        super.onPause()
    }

    private fun stopScannerManager() {
        scannerManager.stopScanner()
        Timber.d("Scanner: stopped")
    }

    abstract fun onScannerLicenseValid()

    abstract fun onScannerLicenseInvalid()
}
