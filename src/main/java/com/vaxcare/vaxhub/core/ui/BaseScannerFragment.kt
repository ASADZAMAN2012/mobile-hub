/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import androidx.work.WorkManager
import com.vaxcare.core.model.BarcodePayload
import com.vaxcare.core.model.ErrorBarcode
import com.vaxcare.core.model.TwoDeeBarcode
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.ProductSelectionMetric
import com.vaxcare.core.report.model.checkout.WrongProductDialogMetric
import com.vaxcare.core.ui.Constant
import com.vaxcare.core.ui.extension.clipOutlineCornerRadius
import com.vaxcare.core.ui.extension.invisible
import com.vaxcare.core.ui.extension.removeLined
import com.vaxcare.core.ui.extension.rotate
import com.vaxcare.core.ui.extension.setAlphaViewAnim
import com.vaxcare.core.ui.extension.underlined
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.animateFadeIn
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.databinding.ScannerErrorViewBinding
import com.vaxcare.vaxhub.di.args.InsertLotNumbersJobArgs
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.enums.LotNumberSources
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.checkout.dialog.InvalidScanMessageType
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel.BaseScannerState.BarcodeErrorFound
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel.BaseScannerState.DriverLicenseBarcodeFound
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel.BaseScannerState.LotNumberBarcodeFound
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel.BaseScannerState.ProductNotAllowed
import com.vaxcare.vaxhub.ui.scanner.ScannerState
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.ProductViewModel
import com.vaxcare.vaxhub.viewmodel.State
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import timber.log.Timber
import java.time.LocalDate

/**
 * Fragment implementation to handle barcode scanning.
 */
abstract class BaseScannerFragment<VB : ViewBinding, VM : BaseScannerViewModel> :
    BaseFragment<VB>() {
    companion object {
        /**
         * Tag to apply to ScannerErrorView when inflated
         */
        const val SCAN_ERROR_TAG = "SCAN_ERROR_TAG"
        const val MAX_REFRESH_ATTEMPTS = 2
    }

    abstract val scanType: ScannerManager.ScanType

    protected val productViewModel: ProductViewModel by activityViewModels()
    protected val appointmentViewModel: AppointmentViewModel by activityViewModels()
    protected abstract val scannerManager: ScannerManager

    protected abstract val viewModel: VM

    protected open val scan1d = false
    protected open val scan2d = true

    /**
     * Counter for refresh attempts, checked against above const
     */
    private var currentRefreshAttempt = 0

    /**
     * Reference to rotating animator
     */
    private var errorRotator: ObjectAnimator? = null

    /**
     * Viewbinding for ScannerError
     */
    private var scanErrorBinding: ScannerErrorViewBinding? = null

    /**
     * Handle specific states for your View Model.
     * General states related to scanning bar/qr codes should be handled globally
     *
     * @param state the state
     */
    protected abstract fun handleState(state: State)

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

    override fun init(view: View, savedInstanceState: Bundle?) {
        scannerManager.resetLibrary()
        scannerManager.state.observe(viewLifecycleOwner) { state ->
            Timber.d("Scanner State: $state")
            when (state) {
                is ScannerState.Activation -> startScannerManager(state.success)
                is ScannerState.Barcode -> processBarcode(state.barcodePayload)
                is ScannerState.DriverLicenseBarcode -> processDriverLicenseScan(
                    state.symbology,
                    state.driverLicense
                )

                ScannerState.Uninitialized -> scannerManager.initializeScanner()
                ScannerState.Started -> onScannerStarted()
                ScannerState.Refreshed -> refreshScanner()
                ScannerState.Resumed -> Timber.d("Scanner Resumed")
                ScannerState.Paused -> Timber.i("Scanner Paused")
                ScannerState.Stopped -> onScannerStopped()
                else -> Timber.d("Unknown scanner state: ${state::class.simpleName}")
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LotNumberBarcodeFound -> onLotNumberBarcodeFound(state)
                is DriverLicenseBarcodeFound -> onDriverLicenseBarcodeFound(
                    metric = state.productSelectionMetric,
                    driverLicense = state.driverLicense
                )

                is BarcodeErrorFound -> handleBarcodeError(
                    productSelectionMetric = state.productSelectionMetric,
                    errorMessage = state.errorMessage
                )

                is ProductNotAllowed -> onScannedProductNotAllowed(
                    ndcScanned = state.ndcCode,
                    messageToShow = state.errorMessage
                )

                else -> handleState(state)
            }
        }

        scannerManager.configureScanner(scan1d, scan2d, scanType)
    }

    private fun onLotNumberBarcodeFound(barcode: LotNumberBarcodeFound) {
        reportProductSelectionMetric(barcode.productSelectionMetric)

        if (barcode.isNewLotNumber) {
            onNewLotNumber(
                barcode.lotNumberWithProduct.name,
                barcode.lotNumberWithProduct.productId,
                barcode.lotNumberWithProduct.expirationDate
            )
        }

        handleLotNumberWithProduct(
            barcode.lotNumberWithProduct,
            barcode.featureFlags
        )
        scannerManager.resumeDecoding()
    }

    /**
     * Handle barcode error for your screen
     *
     * @param productSelectionMetric the metrics to be submitted
     * @param errorMessage the error message
     */
    protected open fun handleBarcodeError(productSelectionMetric: ProductSelectionMetric, errorMessage: String) {
        scannerManager.resumeDecoding()
    }

    private fun onNewLotNumber(
        lotNumberName: String?,
        productId: Int?,
        expiration: LocalDate?
    ) {
        context?.let { ctx ->
            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = WorkManager.getInstance(ctx),
                parameters = OneTimeParams.InsertLotNumbers(
                    InsertLotNumbersJobArgs(
                        lotNumberName = lotNumberName,
                        epProductId = productId,
                        expiration = expiration,
                        source = LotNumberSources.VaxHubScan.id
                    )
                )
            )
        }
    }

    private fun onDriverLicenseBarcodeFound(metric: ProductSelectionMetric, driverLicense: DriverLicense) {
        reportProductSelectionMetric(metric)
        handleDriverLicenseFound(driverLicense)
        scannerManager.resumeDecoding()
        viewModel.resetState()
    }

    private fun onScannedProductNotAllowed(ndcScanned: String, messageToShow: String) {
        reportWrongProductDialogMetric(WrongProductDialogMetric(ndcScanned, messageToShow))
        handleScannedProductNotAllowed(
            messageToShow = messageToShow,
            title = getString(R.string.dialog_invalid_scan_title),
            messageType = InvalidScanMessageType.HTML
        )
    }

    protected open fun onScannerStarted() {
        Timber.i("Scanner has started")
    }

    protected open fun onScannerStopped() {
        Timber.i("Scanner has Stopped")
    }

    protected fun resumeScanner() = scannerManager.resumeDecoding()

    protected fun pauseScanner() = scannerManager.pauseDecoding()

    private fun processBarcode(barcodePayload: BarcodePayload) {
        when (barcodePayload) {
            is TwoDeeBarcode -> viewModel.onTwoDeeBarcode(barcodePayload)
            is ErrorBarcode -> viewModel.onErrorBarcode(barcodePayload)
            else -> Unit // consent form was scanner, no handler for this
        }
    }

    private fun processDriverLicenseScan(symbology: String, driverLicense: DriverLicense) {
        viewModel.onDriverLicenseBarcode(symbology, driverLicense)
    }

    private fun startScannerManager(activated: Boolean) {
        try {
            val scannerViewport = fragmentProperties.scannerViewport?.let {
                view?.findViewById(it) as? ViewGroup
            }

            val searchIcon = fragmentProperties.scannerSearchIcon?.let {
                view?.findViewById(it) as? View
            }

            val preview = view?.findViewById(
                fragmentProperties.scannerPreview
                    ?: throw Exception("Scanner preview must be defined")
            ) as? ViewGroup

            when {
                activated -> refreshScannerView(scannerViewport, searchIcon, preview)
                !containsErrorChild(preview) -> showErrorBinding(
                    scannerViewport,
                    searchIcon,
                    preview
                )

                currentRefreshAttempt >= MAX_REFRESH_ATTEMPTS -> disableErrorBinding()

                else -> {
                    scanErrorBinding?.apply {
                        root.setOnSingleClickListener { onRefreshClick(this) }
                        message.underlined()
                        message.invalidate()
                        errorRotator?.cancel()
                        icon.clearAnimation()
                        icon.invalidate()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting scanner!")
        }
    }

    /**
     * Listener for ScannerErrorViewBinding click
     *
     * @param viewBinding ScannerErrorViewBinding to apply
     */
    private fun onRefreshClick(viewBinding: ScannerErrorViewBinding) {
        viewBinding.apply {
            currentRefreshAttempt++
            root.setOnSingleClickListener { }
            message.removeLined()
            message.invalidate()
            errorRotator = icon.rotate(1000L, true)
            scannerManager.refreshScanner(
                scan1d = scan1d,
                scan2d = scan2d,
                isForceRefresh = true
            )
        }
    }

    /**
     * Hide refresh icon and display the "try again later" text
     */
    private fun disableErrorBinding() {
        scanErrorBinding?.apply {
            icon.invisible()
            message.text = getString(R.string.scanner_problem_try_again)
        }

        onScannerStarted()
    }

    /**
     * Detect if any of the child views from incoming viewgroup have a tag with the SCAN_ERROR_TAG
     *
     * @param parent viewgroup to check
     * @return true if present
     */
    private fun containsErrorChild(parent: ViewGroup?) =
        parent?.children?.map { it.tag }?.contains(SCAN_ERROR_TAG) == true

    /**
     * Inflate the ScannerErrorViewBinding onto the preview
     *
     * @param scannerViewport the parent viewport
     * @param searchIcon search icon on the side of the viewport
     * @param preview preview the scannerManager will use
     */
    private fun showErrorBinding(
        scannerViewport: ViewGroup?,
        searchIcon: View?,
        preview: ViewGroup?
    ) {
        scannerViewport?.apply {
            clipOutlineCornerRadius()
            setAlphaViewAnim(duration = 1000L, delay = Constant.DELAY_MIDDLE, fromValue = alpha)
            children.filter { it != preview }.forEach { it.isVisible = false }
        }

        searchIcon?.apply {
            setAlphaViewAnim(duration = 1000L, delay = Constant.DELAY_MIDDLE, fromValue = alpha)
        }

        context?.let {
            scanErrorBinding = ScannerErrorViewBinding.inflate(
                it.getLayoutInflater(),
                preview,
                true
            ).apply {
                root.tag = SCAN_ERROR_TAG
                if (currentRefreshAttempt >= MAX_REFRESH_ATTEMPTS) {
                    icon.invisible()
                    message.removeLined()
                    message.text = getString(R.string.scanner_problem_try_again)
                    message.invalidate()
                    root.setOnSingleClickListener { }
                } else {
                    message.underlined()
                    root.setOnSingleClickListener { onRefreshClick(this) }
                }
            }
        }

        onScannerStarted()
    }

    private fun refreshScanner() {
        val scannerViewport = fragmentProperties.scannerViewport?.let {
            view?.findViewById(it) as? ViewGroup
        }

        val searchIcon = fragmentProperties.scannerSearchIcon?.let {
            view?.findViewById(it) as? View
        }

        val preview = view?.findViewById(
            fragmentProperties.scannerPreview
                ?: throw Exception("Scanner preview must be defined")
        ) as? ViewGroup

        refreshScannerView(scannerViewport, searchIcon, preview)
    }

    private fun refreshScannerView(
        viewport: ViewGroup?,
        searchIcon: View?,
        preview: ViewGroup?
    ) {
        viewport?.apply {
            clipOutlineCornerRadius()
            animateFadeIn()
            children.filter { it != preview }.forEach { it.isVisible = true }
        }

        searchIcon?.animateFadeIn()

        scannerManager.startScanner()
        scannerManager.getCameraPreview()?.let { cameraPreview ->
            preview?.removeAllViews()
            (cameraPreview.parent as? ViewGroup)?.removeView(cameraPreview)
            preview?.addView(cameraPreview, 0)
            cameraPreview.invalidate()
        }
    }

    private fun stopScannerManager() {
        scannerManager.stopScanner()
        Timber.d("Scanner: stopped")
    }

    private fun fadeInPreview() {
        val viewport = fragmentProperties.scannerViewport?.let { view?.findViewById(it) as? View }
        val icon = fragmentProperties.scannerSearchIcon?.let { view?.findViewById(it) as? View }

        viewport?.apply {
            clipOutlineCornerRadius()
            animateFadeIn()
        }

        icon?.apply {
            animateFadeIn()
        }
    }

    /**
     * After handling the product and not found any general handled condition you could do custom
     * implementations using this method, for specific conditions.
     * In the pass this was handled by the 'callback' function
     *
     * @param lotNumberWithProduct the product found
     * @param featureFlags feature flags available
     */
    protected abstract fun handleLotNumberWithProduct(
        lotNumberWithProduct: LotNumberWithProduct,
        featureFlags: List<FeatureFlag>
    )

    protected abstract fun handleDriverLicenseFound(driverLicense: DriverLicense)

    protected abstract fun handleScannedProductNotAllowed(
        messageToShow: String,
        title: String,
        messageType: InvalidScanMessageType
    )

    protected open fun reportWrongProductDialogMetric(wrongProductDialogMetric: WrongProductDialogMetric) {
        try {
            saveMetric(wrongProductDialogMetric)
        } catch (e: Exception) {
            Timber.e(e, "Error sending WrongProductDialogMetric")
        }
    }

    protected open fun reportProductSelectionMetric(productSelectionMetric: ProductSelectionMetric) {
        try {
            saveMetric(productSelectionMetric)
        } catch (e: Exception) {
            Timber.e(e, "Error sending ProductSelectionMetric")
        }
    }

    private fun saveMetric(metric: BaseMetric) {
        analytics.saveMetric(metric)
    }
}
