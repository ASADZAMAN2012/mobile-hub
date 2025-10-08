/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ProductSelectionMetric
import com.vaxcare.core.report.model.checkout.CancelCheckoutValidationMetric
import com.vaxcare.core.report.model.checkout.CheckoutInsuranceCardCollectionFlow
import com.vaxcare.core.report.model.checkout.CheckoutPromptResult
import com.vaxcare.core.report.model.checkout.UnorderedDoseValidationMetric
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.AndroidBug5497Workaround.Companion.assistActivity
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.core.extension.getEnum
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.getResultValue
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.isCheckoutDemographicsCollectionDisabled
import com.vaxcare.vaxhub.core.extension.isCreditCardCaptureDisabled
import com.vaxcare.vaxhub.core.extension.isDuplicateRSVDisabled
import com.vaxcare.vaxhub.core.extension.isInsurancePhoneCaptureDisabled
import com.vaxcare.vaxhub.core.extension.isInsuranceScanDisabled
import com.vaxcare.vaxhub.core.extension.isMbiForMedDEnabled
import com.vaxcare.vaxhub.core.extension.isMissingPayerFields
import com.vaxcare.vaxhub.core.extension.isNewInsurancePromptDisabled
import com.vaxcare.vaxhub.core.extension.isPaymentModeSelectionDisabled
import com.vaxcare.vaxhub.core.extension.isPayorSelectionEnabled
import com.vaxcare.vaxhub.core.extension.isRightPatientRightDoseEnabled
import com.vaxcare.vaxhub.core.extension.isVaxCare3Enabled
import com.vaxcare.vaxhub.core.extension.popResultValue
import com.vaxcare.vaxhub.core.extension.registerBroadcastReceiver
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.safeContext
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.core.extension.safeWith
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toMillis
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseScannerFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.core.ui.BottomDialogOptions
import com.vaxcare.vaxhub.core.ui.VaxPopupMenu
import com.vaxcare.vaxhub.databinding.FragmentPatientCheckoutBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.AdministeredVaccine
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.CheckoutTerminus
import com.vaxcare.vaxhub.model.DoseReasons
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.MissingInfo
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.model.VaccineAdapterOrderDto
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.VaccineWithIssues
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.model.checkout.CheckoutOptions
import com.vaxcare.vaxhub.model.checkout.CheckoutOptions.CHANGE_STOCK
import com.vaxcare.vaxhub.model.checkout.CheckoutOptions.REFRESH
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.PRIVATE
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.STATE
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.THREE_SEVENTEEN
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_AMERICAN_INDIAN_OR_ALASKA_NATIVE
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_ENROLLED_IN_MEDICAID
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_UNDERINSURED
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_UNINSURED
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.checkout.getValidCopay
import com.vaxcare.vaxhub.model.checkout.toCheckoutStockOption
import com.vaxcare.vaxhub.model.checkout.toInventorySourceAndFinancialClass
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.DeleteActionType
import com.vaxcare.vaxhub.model.enums.DialogAction
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.PregnancyDurationOptions
import com.vaxcare.vaxhub.model.enums.RiskFactor
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.model.enums.VfcFinancialClass
import com.vaxcare.vaxhub.model.getInventorySource
import com.vaxcare.vaxhub.model.inventory.AgeWarning
import com.vaxcare.vaxhub.model.inventory.DoseReasonContext
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.OrderDose
import com.vaxcare.vaxhub.model.inventory.OrderProductWrapper
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.inventory.Site
import com.vaxcare.vaxhub.model.inventory.validator.HubFeatures
import com.vaxcare.vaxhub.model.inventory.validator.ProductVerifier
import com.vaxcare.vaxhub.model.metric.AgeWarningPromptAnsweredMetric
import com.vaxcare.vaxhub.model.metric.CheckoutDoseClickMetric
import com.vaxcare.vaxhub.model.metric.CheckoutFinishMetric
import com.vaxcare.vaxhub.model.metric.CheckoutFinishMetric.CheckoutResult
import com.vaxcare.vaxhub.model.metric.CheckoutProductDeletedMetric
import com.vaxcare.vaxhub.model.metric.CheckoutSetStockClickMetric
import com.vaxcare.vaxhub.model.metric.DuplicateProductBannerMetric
import com.vaxcare.vaxhub.model.metric.MedDCheckRunMetric
import com.vaxcare.vaxhub.model.metric.MedDCopayCheckRequiredDialogClick
import com.vaxcare.vaxhub.model.metric.MedDPromptMetric
import com.vaxcare.vaxhub.model.metric.MedDRunCopayCheckClick
import com.vaxcare.vaxhub.model.metric.SetRouteDialogClick
import com.vaxcare.vaxhub.model.metric.SetSiteDialogClickMetric
import com.vaxcare.vaxhub.model.order.OrderEntity
import com.vaxcare.vaxhub.model.patient.InfoType
import com.vaxcare.vaxhub.model.patient.InvalidInfoWrapper
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.Main
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineItemAdapter
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineItemAdapterListener
import com.vaxcare.vaxhub.ui.checkout.dialog.AgeWarningBooleanPromptDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.BackCheckoutDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.BaseOneTouchCheckoutDialog.Companion.ONE_TOUCH_BUNDLE_RESULT_KEY
import com.vaxcare.vaxhub.ui.checkout.dialog.CheckoutOutOfAgeExclusionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.CheckoutPromptExclusionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.CheckoutPromptMedDExclusionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.CheckoutPromptRemoveDoseDialog.Companion.CHECKOUT_REMOVE_DOSE_DIALOG_RESULT_KEY
import com.vaxcare.vaxhub.ui.checkout.dialog.DuplicateRSVExceptionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogArgs
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton.PRIMARY_BUTTON
import com.vaxcare.vaxhub.ui.checkout.dialog.InvalidScanMessageType
import com.vaxcare.vaxhub.ui.checkout.dialog.MedDReviewCopayDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.MedDReviewCopayDialog.Option.REMOVE_DOSE
import com.vaxcare.vaxhub.ui.checkout.dialog.MedDReviewCopayDialog.Option.RUN_COPAY_CHECK
import com.vaxcare.vaxhub.ui.checkout.dialog.RefreshOrdersDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.RouteRequiredDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ScannedDoseIssueDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.SelectDoseSeriesDialog.Companion.DOSE_SERIES_CHANGED
import com.vaxcare.vaxhub.ui.checkout.dialog.SelectDoseSeriesDialog.Companion.SELECTED_DOSE_SERIES
import com.vaxcare.vaxhub.ui.checkout.dialog.SelectDoseSeriesDialog.Companion.SELECTED_VACCINE_ADAPTER_MODEL_ID
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.issue.ProductPendingUserResult
import com.vaxcare.vaxhub.ui.checkout.extensions.checkoutStatus
import com.vaxcare.vaxhub.ui.navigation.CheckoutPatientDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.patient.edit.BaseEditInsuranceFragment
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel.Companion.FORCED_RISK_FREE_ASSESSMENT_ID
import com.vaxcare.vaxhub.viewmodel.CheckoutPatientViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutPatientViewModel.CheckoutPatientState
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutPatientFragment :
    BaseScannerFragment<FragmentPatientCheckoutBinding, CheckoutPatientViewModel>(),
    VaccineItemAdapterListener {
    companion object {
        const val DUPLICATE_PRODUCT_TOAST_FADEOUT_TIME = 3000L
        const val UNKNOWN_PRODUCT_TOAST_FADEOUT_TIME = 6000L
        const val WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_KEY =
            "weeksPregnantAgeWarningDialogResultKey"
        const val WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_BUTTON =
            "weeksPregnantAgeWarningDialogResultButton"
    }

    private val screenTitle = "ScanDoses"

    override val viewModel: CheckoutPatientViewModel by viewModels()

    private val args: CheckoutPatientFragmentArgs by navArgs()

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var scannerManager: ScannerManager

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CheckoutPatientDestination
    private val vaccineItemAdapter by lazy { VaccineItemAdapter(this) }
    val popupMenu by lazy { VaxPopupMenu(requireContext()) }

    override val scanType: ScannerManager.ScanType = ScannerManager.ScanType.DOSE
    private val appointmentId by lazy { args.appointmentCheckoutMetaData.appointmentId }
    private val isForceRiskFree by lazy { args.appointmentCheckoutMetaData.isForceRiskFree }
    private val isLocallyCreated by lazy { args.appointmentCheckoutMetaData.isLocallyCreated }

    private val updatePatientData by lazy { args.appointmentCheckoutMetaData.updateData?.mergeUpdatePatientData() }
    private var appointmentSessionData = CheckoutSessionData()
    private var receiverRegistered = false
    private val features: HubFeatures = HubFeatures()

    data class HubFeatures(
        var isNewInsurancePromptDisabled: Boolean = false,
        var isCheckoutDemographicsCollectionDisabled: Boolean = false,
        var isPaymentModeSelectionDisabled: Boolean = false,
        var isSelectPayorEnabled: Boolean = false,
        var isInsuranceScanDisabled: Boolean = false,
        var isInsurancePhoneCollectDisabled: Boolean = false,
        var isCreditCardCaptureDisabled: Boolean = false
    )

    private val isVaxCare3
        get() = appointmentViewModel.currentCheckout.isVaxCare3

    override val fragmentProperties = FragmentProperties(
        resource = R.layout.fragment_patient_checkout,
        hasMenu = false,
        hasToolbar = false,
        scannerPreview = R.id.scanner_preview,
        scannerSearchIcon = R.id.patient_checkout_lot_search_btn,
        scannerViewport = R.id.patient_checkout_search_preview
    )

    private fun observeDialogResult(key: String) {
        setFragmentResultListener(key, viewModel::resolveDialogIssue)
    }

    /**
     * Seeing some blu devices (not all) need this when navigating back from lot lookup
     * Lot lookup should clear the activity assist, but isn't reliable on blus
     */
    override fun onResume() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.latestMedDInfo()
                .flowWithLifecycle(this@CheckoutPatientFragment.lifecycle)
                .collectLatest { (medDInfo, isMedD) ->
                    handleMedDInfoReceived(medDInfo = medDInfo, isAppointmentMedD = isMedD)
                }
        }
        super.onResume()
        assistActivity(requireActivity()).stopAssistingActivity()
    }

    override fun canShowConnection(): Boolean = false

    override fun bindFragment(container: View): FragmentPatientCheckoutBinding =
        FragmentPatientCheckoutBinding.bind(container)

    override fun handleLotNumberWithProduct(
        lotNumberWithProduct: LotNumberWithProduct,
        featureFlags: List<FeatureFlag>
    ) {
        lifecycleScope.launch {
            var series: Int? = null
            if (lotNumberWithProduct.product.antigen.uppercase() == "COVID" &&
                locationViewModel.getFeatureFlagByName(FeatureFlagConstant.FeatureCovidAssist) != null
            ) {
                series = appointmentViewModel.getCovidSeries(lotNumberWithProduct.salesProductId)
            }
            verifyProduct(lotNumberWithProduct, series)
        }
    }

    override fun handleScannedProductNotAllowed(
        messageToShow: String,
        title: String,
        messageType: InvalidScanMessageType
    ) {
        destination.goToInvalidScanPrompt(
            fragment = this@CheckoutPatientFragment,
            title = title,
            messageToShow = messageToShow,
            messageType = messageType
        )
    }

    override fun handleDriverLicenseFound(driverLicense: DriverLicense) {
        // Do nothing
    }

    override fun handleBarcodeError(productSelectionMetric: ProductSelectionMetric, errorMessage: String) {
        productSelectionMetric.screenSource = "Checkout"
        analytics.saveMetric(productSelectionMetric)
        (activity as? Main)?.showToastMessage(
            R.string.scanned_dose_unknown_product_header,
            R.string.scanned_dose_unknown_product_body,
            UNKNOWN_PRODUCT_TOAST_FADEOUT_TIME
        )
        super.handleBarcodeError(productSelectionMetric, errorMessage)
    }

    private fun onBack() {
        appointmentViewModel.currentCheckout.selectedAppointment?.let { appointment ->
            analytics.saveMetric(
                CheckoutFinishMetric(
                    visitId = appointmentId,
                    doseCount = vaccineItemAdapter.itemCount,
                    isCheckedOut = appointment.checkedOut,
                    paymentMethod = appointment.paymentMethod,
                    duration = LocalDateTime.now()
                        .toMillis() - appointment.appointmentTime.toMillis(),
                    result = CheckoutResult.ABANDONED,
                    missingInfoCaptured = args.appointmentCheckoutMetaData.updateData != null,
                    networkStatus = networkMonitor.networkStatus.value
                        ?: NetworkStatus.DISCONNECTED,
                    relativeDoS = appointment.getRelativeDoS(),
                    paymentType = "N/A"
                )
            )
        }
        if (isLocallyCreated && appointmentViewModel.currentCheckout.selectedAppointment?.checkedOut == false
        ) {
            pauseScanner()
            viewModel.abandonAppointment(appointmentId)
        } else {
            appointmentViewModel.clearCurrentCheckout()
            if (args.appointmentCheckoutMetaData.curbsideNewPatient) {
                globalDestinations.goBackToSplash(this@CheckoutPatientFragment)
            } else {
                leaveAppointment()
            }
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        super.onDestinationChanged(controller, destination, arguments)

        if (destination.id != R.id.checkoutPatientFragment) {
            viewModel.resetState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.uiFlow.collectLatest { handleState(it) }
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        revertPaymentFlips()
        observeBackStack()
        logScreenNavigation(screenTitle)
        binding?.patientInfoContainer?.apply {
            if (getResultValue<Boolean>(MedDCheckFragment.COPAY_REVIEWED) == true) {
                setExpanded(true)
            }
            outlineProvider = null
        }

        if (isForceRiskFree) {
            viewModel.fetchLocalAppointmentDataDueToRiskFree(appointmentId = appointmentId)
        } else {
            viewModel.fetchUpdatedAppointment(
                appointmentId = appointmentId,
                forceFetch = shouldForceFetch(),
                medDCoverageNotFoundId = R.string.fragment_checkout_patient_copay_dose_not_found,
                medDNoCoverageId = R.string.fragment_checkout_patient_copay_dose_not_covered
            )
        }
    }

    private fun handleMedDInfoReceived(medDInfo: MedDInfo?, isAppointmentMedD: Boolean) {
        if (isAppointmentMedD) {
            binding?.patientInfo?.setMedDData(medDInfo)
            alignCopays(medDInfo)
        }
    }

    private fun refreshAppointment(appointmentId: Int) {
        appointmentSessionData = CheckoutSessionData()
        vaccineItemAdapter.vaccineItems.removeAll { it.doseState == DoseState.ADDED }
        viewModel.refreshAppointment(appointmentId)
    }

    private fun VaccineItemAdapter.hasAddedDosesInCart(): Boolean = vaccineItems.any { it.doseState == DoseState.ADDED }

    /**
     * Reverts payment flips that may have occurred during the phone collection flow
     * but navigating back from summary screen
     */
    private fun revertPaymentFlips() {
        appointmentViewModel.currentCheckout.stagedProducts
            .filter { it.missingInsuranceSelfPay || it.missingInsurancePartnerBill }
            .forEach {
                if (it.missingInsuranceSelfPay) {
                    it.flipSelfPay(false)
                } else {
                    it.flipPartnerBill(false)
                }

                it.paymentModeReason = it.originalPaymentModeReason
            }
    }

    /**
     * Handle navigation callbacks for the phone collection flow
     */
    private fun observeBackStack() {
        observePhoneCollection()
        observeNoInsuranceCardSelected()
    }

    private fun observePhoneCollection() {
        getResultLiveData<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)?.observe(
            viewLifecycleOwner
        ) { data ->
            // handle phone collection flow
            val agreed =
                appointmentViewModel.addPhoneCollectField(
                    tagSet = COLLECT_PHONE_DATA_FRAGMENT_TAG,
                    data = data,
                    PhoneContactReasons.INSURANCE_CARD
                )
            if (isVaxCare3 && !agreed) {
                appointmentViewModel.currentCheckout.flipPartnerBill(
                    reason = PaymentModeReason.RequestedMediaNotProvided
                )
            }

            removeResult<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)
            destination.toCheckoutSummary(this@CheckoutPatientFragment, appointmentId)
        }
    }

    private fun observeNoInsuranceCardSelected() {
        getResultLiveData<Payer?>(BaseEditInsuranceFragment.NO_CARD_FLOW)?.observe(
            viewLifecycleOwner
        ) {
            // handle "No Insurance Card" was selected from the Collect Insurance fragment
            removeResult<Payer>(BaseEditInsuranceFragment.NO_CARD_FLOW)
            goToInsurancePhoneCollection()
        }
    }

    override fun handleState(state: State) {
        Timber.d("Incoming State: ${state.javaClass.simpleName}")
        when (state) {
            LoadingState -> showLoading()
            CheckoutPatientState.ProductAddCanceled -> resumeScanner()
            CheckoutPatientState.AceTimingOut -> showLoading(R.string.taking_a_few_more_seconds)
            CheckoutPatientState.AceSavingInfo ->
                showLoading(R.string.fragment_checkout_stock_saving_info)

            is CheckoutPatientState.StockError -> {
                hideLoading()
                handleStockPatchError(
                    selectedStockId = state.selectedStockId,
                    selectedFinancialClass = state.selectedFinancialClass
                )
            }

            is CheckoutPatientState.StockTimeout -> setUpdatedAppointment(state.riskFreeAppointment)
            is CheckoutPatientState.AppointmentUpdated -> setUpdatedAppointment(state.appointment)
            is CheckoutPatientState.ErrorGettingAppointment ->
                Timber.e("Error getting Appointment")

            is CheckoutPatientState.AppointmentLoaded -> {
                if (state.appointment.isEditable == false) {
                    redirectToCheckoutSummary()
                }

                features.apply {
                    isNewInsurancePromptDisabled = state.flags.isNewInsurancePromptDisabled()
                    isCheckoutDemographicsCollectionDisabled =
                        state.flags.isCheckoutDemographicsCollectionDisabled()
                    isPaymentModeSelectionDisabled = state.flags.isPaymentModeSelectionDisabled()
                    isSelectPayorEnabled = state.flags.isPayorSelectionEnabled()
                    isInsuranceScanDisabled = state.flags.isInsuranceScanDisabled()
                    isInsurancePhoneCollectDisabled = state.flags.isInsurancePhoneCaptureDisabled()
                    isCreditCardCaptureDisabled = state.flags.isCreditCardCaptureDisabled()
                }

                appointmentViewModel.currentCheckout.isVaxCare3 = state.flags.isVaxCare3Enabled()
                setupUi(
                    appointment = state.appointment,
                    rprd = state.flags.isRightPatientRightDoseEnabled(),
                    disableDuplicateRSV = state.flags.isDuplicateRSVDisabled(),
                    checkMbi = state.flags.isMbiForMedDEnabled(),
                    shotAdmin = state.shotAdmin,
                    refresh = state.isForceRefresh
                )

                state.availableCheckoutOptions?.let { opts ->
                    if (opts.any { it == CHANGE_STOCK }) {
                        setUpStockSelectorUI(
                            checkoutStockOptions = state.inventorySources
                                .sortedWith(
                                    StockOptionsDisplayOrderComparator()
                                ),
                            appointment = state.appointment
                        )
                    }

                    setMenuWithCheckoutOptions(options = opts)
                }
            }

            is CheckoutPatientState.ProductVerified ->
                addProductToView(vaccineWithIssues = state.vaccineWithIssues)

            is CheckoutPatientState.ProductHasIssues ->
                handleDialogIssue(
                    issue = state.issue,
                    vaccine = state.vaccineWithIssues,
                    appointment = state.appointment
                )

            is CheckoutPatientState.AppointmentRefreshed -> {
                hideLoading()
                setupUi(
                    appointment = state.appointment,
                    rprd = appointmentSessionData.isRprdAndNotLocallyCreated,
                    disableDuplicateRSV = appointmentSessionData.isDisableDuplicateRSV,
                    shotAdmin = state.shotAdmin,
                    checkMbi = appointmentSessionData.isCheckMbi,
                    refresh = true
                )

                state.availableCheckoutOptions?.let {
                    setMenuWithCheckoutOptions(options = it)
                }

                state.availableCheckoutOptions?.let {
                    if (it.any { it == CHANGE_STOCK }) {
                        setUpStockSelectorUI(
                            checkoutStockOptions = state.inventorySources
                                .sortedWith(
                                    StockOptionsDisplayOrderComparator()
                                ),
                            appointment = state.appointment
                        )
                    }

                    setMenuWithCheckoutOptions(options = it)
                }
            }

            is CheckoutPatientState.AppointmentAbandoned -> {
                appointmentViewModel.clearCurrentCheckout()
                leaveAppointment()
            }

            CheckoutPatientState.PendingUserActionStock -> showStockSelectorBottomDialog()

            else -> Unit
        }
    }

    private fun setMenuWithCheckoutOptions(options: List<CheckoutOptions>) {
        popupMenu.clearMenuItems()

        val menuItems: List<VaxPopupMenu.MenuItem> = options.map { option ->
            when (option) {
                REFRESH -> {
                    VaxPopupMenu.MenuItem(
                        title = getString(R.string.menu_refresh_appointment),
                        onClickListener = {
                            if (vaccineItemAdapter.hasAddedDosesInCart()) {
                                showRefreshOrdersDialog()
                            } else {
                                refreshAppointment(appointmentId)
                            }
                        }
                    )
                }

                CHANGE_STOCK -> {
                    VaxPopupMenu.MenuItem(
                        title = getString(R.string.menu_stock_selector_set_stock),
                        onClickListener = {
                            appointmentSessionData.currentAppointmentStock?.displayName?.let {
                                viewModel.saveMetric(
                                    CheckoutSetStockClickMetric(
                                        visitId = appointmentId,
                                        currentStock = getString(it)
                                    )
                                )
                            }
                            showStockSelectorBottomDialog()
                        }
                    )
                }
            }
        }

        popupMenu.addMenuItems(menuItems)

        binding?.topBar?.setupRightIcon1(R.drawable.ic_kebab_menu) {
            binding?.topBar?.rightIcon1?.let { popupMenu.show(it) }
        }
    }

    /**
     * Set updated appointment info on the UI - does not touch administered doses or register new
     * observers
     *
     * @param appointment updated appointment
     */
    private fun setUpdatedAppointment(appointment: Appointment) {
        appointmentViewModel.currentCheckout.selectedAppointment = appointment
        setupAppointmentInfo(appointment)
        if (appointmentSessionData.stockSelectorEnabled) {
            binding?.patientInfo?.displayAppointmentStock(appointment)
            setUpStockSelectorUI(
                checkoutStockOptions = appointmentSessionData.checkoutStockList,
                appointment = appointment
            )
        }
        vaccineItemAdapter.updateItems { oldItems ->
            oldItems.map {
                when (it) {
                    is VaccineAdapterProductDto -> it.apply {
                        it.appointmentPaymentMethod = appointment.paymentMethod
                        copay = null
                        val (flippedPaymentMode, flippedReason) =
                            if (appointment.isPrivate()) {
                                originalPaymentMode to originalPaymentModeReason
                            } else {
                                null to null
                            }

                        paymentMode = flippedPaymentMode
                        paymentModeReason = flippedReason
                    }

                    else -> it
                }
            }
        }
        hideLoading()
        viewModel.resolvePendingProductUserAction(
            resolvedUserAction = ProductPendingUserResult.SET_STOCK_COMPLETE,
            appointment = appointment
        )
    }

    private fun setUpStockSelectorUI(checkoutStockOptions: List<CheckoutStockOption>, appointment: Appointment) {
        binding?.patientInfo?.displayAppointmentStock(appointment)

        val currentStock = appointment.patient.paymentInformation?.vfcFinancialClass?.let {
            appointment.getInventorySource()
                .toCheckoutStockOption(
                    runCatching {
                        VfcFinancialClass.valueOf(it)
                    }.getOrNull() ?: VfcFinancialClass.V02
                )
        } ?: appointment.getInventorySource().toCheckoutStockOption()
        appointmentSessionData.apply {
            stockSelectorEnabled = true
            checkoutStockList = checkoutStockOptions
            currentAppointmentStock = currentStock
        }
    }

    private fun showStockSelectorBottomDialog() {
        appointmentSessionData.checkoutStockList
        val setStockBottomDialog = BottomDialog.newInstance(
            title = getString(R.string.menu_stock_selector_set_stock),
            values = appointmentSessionData.checkoutStockList.map { getString(it.displayName) },
            selectedIndex = appointmentSessionData.checkoutStockList.indexOfFirst {
                it.displayName == appointmentSessionData.currentAppointmentStock?.displayName
            }
        ).apply {
            isSelectedItemClickEnabled = false
        }

        var optionSelected = false
        setStockBottomDialog.onSelected = { index ->
            optionSelected = true
            val (sourceId, financialClass) = appointmentSessionData.checkoutStockList[index]
                .toInventorySourceAndFinancialClass()
            registerReceiverForAce()
            viewModel.onStockSelected(
                selectedStockId = sourceId,
                selectedFinancialClass = financialClass,
                appointmentId = appointmentId
            )
        }

        setStockBottomDialog.onDismissed = {
            if (!optionSelected) {
                viewModel.resolvePendingProductUserAction(
                    resolvedUserAction = ProductPendingUserResult.SET_STOCK_CANCEL,
                    appointment = null
                )
            }
        }

        setStockBottomDialog.show(childFragmentManager, "AvailableStockOptions")
    }

    /**
     * Registers the ACE receiver for partial appointment updates
     */
    private fun registerReceiverForAce() {
        if (!receiverRegistered) {
            safeWith(context) {
                registerBroadcastReceiver(
                    receiver = appointmentChangedEventReceiver,
                    intentFilter = IntentFilter(Receivers.ACE_ACTION)
                )

                receiverRegistered = true
            }
        }
    }

    private fun showLoading(
        @StringRes loadingTextRes: Int = R.string.load_appointment_details_in_progress
    ) {
        binding?.apply {
            patientInfo.hide()
            textViewLoadingDescription.setText(loadingTextRes)
            loadingBackground.show()
        }
        startLoading()
    }

    private val appointmentChangedEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val (incomingAppointmentId, changeReasonOrdinal) =
                intent?.extras?.getInt(Receivers.ACE_APPOINTMENT_ID) to
                    intent?.extras?.getInt(Receivers.ACE_CHANGE_REASON)
            if (incomingAppointmentId == appointmentId) {
                when (AppointmentChangeReason.fromInt(changeReasonOrdinal)) {
                    AppointmentChangeReason.RiskUpdated -> {
                        receiverRegistered = false
                        context?.unregisterReceiver(this)
                        viewModel.fetchUpdatedAppointmentAfterAce(appointmentId)
                    }

                    else -> Unit
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun setupUi(
        appointment: Appointment,
        rprd: Boolean,
        disableDuplicateRSV: Boolean,
        checkMbi: Boolean,
        shotAdmin: ShotAdministrator? = null,
        refresh: Boolean = false
    ) {
        val refreshed = refresh || appointmentViewModel.currentCheckout.isEditCheckoutFromComplete
        appointmentSessionData.apply {
            isDisableDuplicateRSV = disableDuplicateRSV
            isCheckMbi = checkMbi
            isRprdAndNotLocallyCreated = rprd && !isLocallyCreated
            dataCollectedBeforeCheckout = updatePatientData?.dataCollected() == true
        }

        appointmentViewModel.currentCheckout.isLocallyCreated = isLocallyCreated
        appointmentViewModel.currentCheckout.administrator = shotAdmin

        binding?.patientCheckoutLotSearchBtn?.setOnClickListener {
            globalDestinations.toLotLookup(
                fragment = this,
                appointmentId = appointmentId,
                relativeDoS = appointmentViewModel.currentCheckout.selectedAppointment?.getRelativeDoS()?.name
            )
        }

        binding?.topBar?.onCloseAction = { handleBack() }

        setupAppointmentInfo(appointment)

        binding?.fabNext?.setOnSingleClickListener { validateCheckoutAndNavigate() }

        object : CheckoutPatientSwipeHelper(requireContext(), binding?.rvVaccines!!) {}

        // Listen to requests to delete/restore items
        viewModel.deletedItem.observe(viewLifecycleOwner) {
            if (it != -1) {
                viewModel.deletedItem.value = -1
                (vaccineItemAdapter.vaccineItems[it] as? VaccineAdapterProductDto)?.let { data ->
                    when (data.doseState) {
                        DoseState.ADDED, DoseState.ADMINISTERED -> {
                            // requests to delete items
                            vaccineItemAdapter.removeByPosition(
                                it,
                                appointment.checkoutStatus() == EditCheckoutStatus.ACTIVE_CHECKOUT,
                                appointmentViewModel.currentCheckout.selectedAppointment?.orders
                                    ?: emptyList()
                            )
                        }

                        DoseState.ADMINISTERED_REMOVED, DoseState.REMOVED -> {
                            // requests to restore items
                            vaccineItemAdapter.restoreDoseByPosition(it)
                        }

                        else -> Unit
                    }
                }
            }
        }

        binding?.rvVaccines?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = vaccineItemAdapter
        }

        val hasAdministeredDoses = appointment.administeredVaccines.any { !it.isDeleted }
        val hasProductsInCart = vaccineItemAdapter.vaccineItems
            .any { it.doseState != DoseState.ORDERED }
        val hideEmptyState = hasAdministeredDoses || hasProductsInCart

        updateDoseUsingStatus(hideEmptyState)
        setupLotNumberListener()

        when (appointment.checkoutStatus()) {
            EditCheckoutStatus.PAST_CHECKOUT -> {
                if (appointmentViewModel.currentCheckout.stagedProducts.isEmpty() &&
                    appointment.administeredVaccines.isNotEmpty()
                ) {
                    val verifier =
                        ProductVerifier(
                            HubFeatures(
                                isRprdAndNotLocallyCreated = appointmentSessionData.isRprdAndNotLocallyCreated,
                                isDisableDuplicateRSV = appointmentSessionData.isDisableDuplicateRSV
                            )
                        )
                    addAdministeredVaccinesToView(appointment, verifier)
                }

                addOrdersToView(appointment)
            }

            EditCheckoutStatus.ACTIVE_CHECKOUT -> addOrdersToView(appointment)
            else -> Unit
        }

        binding?.fabNext?.isEnabled = isHasEdited()

        getResultLiveData<String>(DOSE_SERIES_CHANGED)?.observe(viewLifecycleOwner) { data ->
            val doseData = JSONObject(data)
            vaccineItemAdapter.changeDoseSeries(
                doseData.getString(SELECTED_VACCINE_ADAPTER_MODEL_ID),
                doseData.getInt(SELECTED_DOSE_SERIES)
            )
        }

        if (refresh && hideEmptyState) {
            binding?.patientInfoContainer?.setExpanded(false)
        }
        if (refreshed || !hideEmptyState) {
            hideLoading()
        }
    }

    private fun setupAppointmentInfo(appointment: Appointment) {
        binding?.patientInfo?.apply {
            onRunCopayCheck = {
//                appointmentViewModel.currentCheckout.hasMedCheckAttempted = true
                analytics.saveMetric(MedDRunCopayCheckClick(appointmentId))
                destination.goToMedDCheckFromCheckout(
                    fragment = this@CheckoutPatientFragment,
                    appointmentId = appointmentId,
                    duringCheckout = MedDCheckRunMetric.CheckContext.DURING_CHECKOUT,
                    isCheckMbi = appointmentSessionData.isCheckMbi,
                    isMedDCheckStartedAlready = viewModel.isMedDCheckRunStarted,
                    medDCheckStartedAt = viewModel.medDCheckStartedAt?.toString()
                )
            }
            setAppointmentData(appointment = appointment)
        }

        appointmentViewModel.currentCheckout.apply {
            selectedAppointment = appointment
            presentedRiskAssessmentId = if (isForceRiskFree) {
                FORCED_RISK_FREE_ASSESSMENT_ID
            } else {
                appointment.encounterState?.vaccineMessage?.riskAssessmentId
            }
        }
    }

    private fun handleStockPatchError(selectedStockId: Int, selectedFinancialClass: String?) {
        getResultLiveData<ErrorDialogButton>(ErrorDialog.RESULT)?.observe(
            viewLifecycleOwner
        ) {
            val (userChoice, tryAgain) = when (it) {
                // "Try Again"
                PRIMARY_BUTTON -> getString(R.string.dialog_trouble_connecting_cta1) to true

                // "OK"
                else -> getString(R.string.dialog_trouble_connecting_cta2) to false
            }

            with(viewModel) {
                saveCheckoutStockErrorOptionSelectedMetric(
                    choice = userChoice,
                    appointmentId = appointmentId
                )

                if (tryAgain) {
                    changeAppointmentStock(
                        selectedStockId = selectedStockId,
                        selectedFinancialClass = selectedFinancialClass,
                        appointmentId = appointmentId
                    )
                } else {
                    resolvePendingProductUserAction(
                        resolvedUserAction = ProductPendingUserResult.SET_STOCK_CANCEL,
                        appointment = null
                    )
                }
            }

            removeResult<ErrorDialogButton>(ErrorDialog.RESULT)
        }

        globalDestinations.goToErrorDialog(
            fragment = this@CheckoutPatientFragment,
            title = R.string.dialog_trouble_connecting_title,
            body = R.string.dialog_error_requires_wifi,
            primaryBtn = R.string.dialog_trouble_connecting_cta1,
            secondaryBtn = R.string.dialog_trouble_connecting_cta2
        )
    }

    private fun shouldForceFetch() =
        appointmentViewModel.currentCheckout.isEditCheckoutFromComplete ||
            popResultValue<Boolean>(MedDCheckFragment.COPAY_REVIEWED) == true

    private fun onClose() {
        if (isHasEdited()) {
            getResultLiveData<Boolean>(BackCheckoutDialog.BACK_CHECKOUT_RESULT)?.observe(
                viewLifecycleOwner
            ) {
                analytics.saveMetric(
                    CancelCheckoutValidationMetric(
                        appointmentId,
                        if (it) {
                            CheckoutPromptResult.CANCEL_CHECKOUT
                        } else {
                            CheckoutPromptResult.KEEP_CHECKOUT
                        }
                    )
                )

                if (it) {
                    onBack()
                }
            }
            destination.toBackCheckoutDialog(this@CheckoutPatientFragment)
        } else {
            onBack()
        }
    }

    override fun onSwipeActionButton(product: VaccineAdapterProductDto, position: Int) {
        viewModel.saveMetric(
            CheckoutProductDeletedMetric(
                deleteType = DeleteActionType.PARTIAL_SWIPE,
                lotNumber = product.lotNumber,
                productIssuesCsv = product.vaccineIssues
                    .joinToString { it::class.simpleName.toString() }
            )
        )
        viewModel.deletedItem.value = position
    }

    override fun onDeleteAttempt(
        product: VaccineAdapterProductDto,
        position: Int,
        deleteActionType: DeleteActionType
    ) {
        if (product.doseState in listOf(
                DoseState.ADMINISTERED_REMOVED,
                DoseState.REMOVED
            )
        ) {
            viewModel.deletedItem.value = position
        } else {
            setFragmentResultListener(CHECKOUT_REMOVE_DOSE_DIALOG_RESULT_KEY) { _, bundle ->
                when (bundle.getEnum(ONE_TOUCH_BUNDLE_RESULT_KEY, DialogAction.CANCEL)) {
                    DialogAction.POSITIVE -> {
                        viewModel.saveMetric(
                            CheckoutProductDeletedMetric(
                                deleteType = deleteActionType,
                                lotNumber = product.lotNumber,
                                productIssuesCsv = product.vaccineIssues
                                    .joinToString { it::class.simpleName.toString() }
                            )
                        )
                        viewModel.deletedItem.value = position
                    }

                    else -> {
                        vaccineItemAdapter.resetSwipeState(position)
                    }
                }
            }

            destination.toCheckoutPromptRemoveDose(this)
        }
    }

    override fun onCheckoutDoseSeries(product: VaccineAdapterProductDto, position: Int) {
        destination.toSelectDoseSeries(this, product)
    }

    private fun updateDoseUsingStatus(shouldHide: Boolean) {
        binding?.patientEmptyText?.isGone = shouldHide
    }

    private fun verifyProduct(lotNumberWithProduct: LotNumberWithProduct, doseSeries: Int?) {
        binding?.patientInfoContainer?.setExpanded(false)

        onDoseInSeriesVerify(lotNumberWithProduct) {
            val appointment =
                requireNotNull(appointmentViewModel.currentCheckout.selectedAppointment)
            viewModel.evaluateProductForIssues(
                appointment = appointment,
                lotNumberWithProduct = lotNumberWithProduct,
                stagedProducts = vaccineItemAdapter.vaccineItems,
                doseSeries = doseSeries,
                manualDob = appointmentViewModel.currentCheckout.manualDob,
                isLocallyCreated = isLocallyCreated,
            )
        }
    }

    private fun addProductToView(vaccineWithIssues: VaccineWithIssues?) {
        vaccineWithIssues?.let { vaccine -> vaccineItemAdapter.addItem(vaccine) }
        hideLoading()
        resumeScanner()
    }

    private fun handleDialogIssue(
        issue: ProductIssue,
        vaccine: VaccineWithIssues,
        appointment: Appointment
    ) {
        // Don't pause the scanner for toast messages, only for dialogs
        if (issue != ProductIssue.DuplicateProduct && issue != ProductIssue.DuplicateLot) {
            pauseScanner()
        }

        when (issue) {
            ProductIssue.DuplicateProductException ->
                showDuplicateProductExceptionDialog(
                    otPayRate = vaccine.lotNumberWithProduct.oneTouch?.selfPayRate?.toString(),
                    shouldShowPaymentFlip(appointment)
                )

            ProductIssue.DuplicateProduct,
            ProductIssue.DuplicateLot ->
                showDuplicateProductBannerAndSaveMetric(
                    appointment = appointment,
                    vaccine = vaccine
                )

            ProductIssue.LarcAdded -> {
                handleScannedProductNotAllowed(
                    messageToShow = getString(R.string.scanned_dose_larc_added_body),
                    title = getString(R.string.scanned_dose_larc_added_title),
                    InvalidScanMessageType.TEXT
                )
            }

            ProductIssue.Unordered -> showUnorderedDoseDialog()
            ProductIssue.CopayRequired -> showRequireMedDRun(
                appointmentId = appointment.id,
                product = vaccine.lotNumberWithProduct
            )

            ProductIssue.ProductNotCovered -> {
                if (shouldShowPaymentFlip(appointment)) {
                    showDoseNotCoveredDialog(
                        appointment = appointment,
                        otPayRate = vaccine.lotNumberWithProduct.oneTouch?.selfPayRate?.toDouble()
                    )
                } else {
                    showScannedDoseIssueDialog(vaccine, ProductIssue.ProductNotCovered)
                }
            }

            is ProductIssue.OutOfAgeWarning -> {
                if (issue.title == null || issue.message == null) return
                when (issue.promptType) {
                    AgeWarning.PromptType.BOOLEAN -> {
                        showAgeWarningBooleanPrompt(issue.title, issue.message)
                    }

                    AgeWarning.PromptType.SINGLE_SELECT -> {
                        showPregnancyWeekSelectorBottomDialog(issue.title)
                    }

                    null -> {}
                }
            }

            ProductIssue.MissingLotNumber,
            ProductIssue.Expired,
            ProductIssue.RestrictedProduct,
            ProductIssue.OutOfAgeIndication -> {
                if (issue == ProductIssue.OutOfAgeIndication && shouldShowPaymentFlip(appointment)) {
                    showOutOfAgeIndicationDialog(
                        appointment,
                        vaccine.lotNumberWithProduct.oneTouch?.selfPayRate?.toDouble(),
                    )
                } else {
                    showScannedDoseIssueDialog(vaccineWithIssues = vaccine, issue = issue)
                }
            }

            ProductIssue.WrongStock -> {
                showWrongStockDialog(
                    appointment.getInventorySource()
                )
            }

            ProductIssue.RouteSelectionRequired ->
                showRouteSelectionDialog(
                    productId = vaccine.lotNumberWithProduct.productId,
                    productDisplayName = vaccine.lotNumberWithProduct.product.displayName,
                    lotNumber = vaccine.lotNumberWithProduct.name
                )
        }
    }

    private fun showDuplicateProductExceptionDialog(otPayRate: String?, shouldShorPaymentFlip: Boolean) {
        observeDialogResult(DuplicateRSVExceptionDialog.DUPLICATE_RSV_EXCEPTION_RESULT_KEY)

        destination.goToDuplicateRSVExceptionDialog(
            fragment = this@CheckoutPatientFragment,
            shouldShowPaymentFlip = shouldShorPaymentFlip,
            otPayRate = otPayRate
        )
    }

    private fun showRefreshOrdersDialog() {
        setFragmentResultListener(RefreshOrdersDialog.REQUEST_KEY) { _, listener ->
            when (listener.getInt(RefreshOrdersDialog.OPTION_SELECTED_BUNDLE_KEY)) {
                RefreshOrdersDialog.Option.REFRESH.ordinal -> {
                    refreshAppointment(appointmentId)
                }
            }
        }

        destination.goToRefreshOrdersDialog(this@CheckoutPatientFragment)
    }

    private fun showDuplicateProductBannerAndSaveMetric(appointment: Appointment, vaccine: VaccineWithIssues) {
        (activity as? Main)?.showToastMessage(
            R.string.scanned_dose_duplicate_product_header,
            R.string.scanned_dose_duplicate_product_body,
            DUPLICATE_PRODUCT_TOAST_FADEOUT_TIME
        )
        saveDuplicateProductBannerMetric(appointment.id, vaccine.lotNumberWithProduct)
        viewModel.cancelDialogIssues()
    }

    private fun showUnorderedDoseDialog() {
        observeErrorDialogAction { action ->
            when (action) {
                PRIMARY_BUTTON -> {
                    analytics.saveMetric(
                        UnorderedDoseValidationMetric(
                            appointmentId,
                            CheckoutPromptResult.KEEP_DOSE
                        )
                    )

                    viewModel.getNextIssue()
                }

                else -> {
                    analytics.saveMetric(
                        UnorderedDoseValidationMetric(
                            appointmentId,
                            CheckoutPromptResult.REMOVE_DOSE
                        )
                    )

                    viewModel.cancelDialogIssues()
                }
            }
        }

        destination.toUnorderedDosePrompt(
            this,
            ErrorDialogArgs(
                title = R.string.orders_unordered_dose_prompt_title,
                body = R.string.orders_unordered_dose_prompt_message,
                primaryButton = R.string.orders_unordered_dose_prompt_yes,
                secondaryButton = R.string.orders_unordered_dose_prompt_no
            )
        )
    }

    private fun showDoseNotCoveredDialog(appointment: Appointment, otPayRate: Double?) {
        val copayCheckUnavailable =
            appointment.encounterState?.medDMessage?.callToAction == CallToAction.None

        val (navPrompt, dialogKey) =
            if (copayCheckUnavailable) {
                R.id.action_checkoutVaccineFragment_to_checkoutPromptMedDExclusionDialog to
                    CheckoutPromptMedDExclusionDialog.MEDD_EXCLUSION_DIALOG_RESULT_KEY
            } else {
                R.id.action_checkoutVaccineFragment_to_checkoutPromptExclusionDialog to
                    CheckoutPromptExclusionDialog.CHECKOUT_EXCLUSION_DIALOG_RESULT_KEY
            }

        observeDialogResult(dialogKey)
        destination.showPromoDialog(
            fragment = this,
            appointment = appointment,
            resId = navPrompt,
            otPayRate = otPayRate
        )
    }

    private fun showRouteSelectionDialog(
        productId: Int,
        productDisplayName: String,
        lotNumber: String
    ) {
        observeDialogResult(RouteRequiredDialog.ROUTE_REQUIRED_DIALOG_FRAGMENT_RESULT_KEY)
        destination.goToRouteRequiredDialog(
            fragment = this,
            productId = productId,
            productName = productDisplayName,
            lotNumber = lotNumber,
            appointmentId = appointmentId
        )
    }

    private fun showWrongStockDialog(appointmentInventorySource: InventorySource) {
        observeDialogResult(WrongStockDialog.WRONG_STOCK_DIALOG_RESULT_KEY)
        destination.goToWrongStockDialog(
            fragment = this,
            appointmentInventorySource = appointmentInventorySource,
            isStockSelectorEnabled = appointmentSessionData.stockSelectorEnabled
        )
    }

    private fun shouldShowPaymentFlip(appointment: Appointment) =
        appointment.paymentMethod == PaymentMethod.InsurancePay &&
            appointment.isPrivate() && !features.isPaymentModeSelectionDisabled

    private fun observeErrorDialogAction(onActionChanged: (action: ErrorDialogButton) -> Unit) {
        getResultLiveData<ErrorDialogButton>(ErrorDialog.RESULT)?.observe(viewLifecycleOwner) { action ->
            action ?: return@observe
            removeResult<ErrorDialogButton>(ErrorDialog.RESULT)
            resumeScanner()
            onActionChanged.invoke(action)
        }
    }

    private fun showRequireMedDRun(appointmentId: Int, product: LotNumberWithProduct) {
        observeDialogResult(MedDCheckFragment.MEDD_COPAY_CHECK_FRAGMENT_RESULT_KEY)
        analytics.saveMetric(
            MedDPromptMetric(
                appointmentId,
                MedDPromptMetric.PromptContext.DURING_CHECKOUT.displayName
            )
        )

        setFragmentResultListener(MedDReviewCopayDialog.REQUEST_KEY) { _, bundle ->
            when (
                bundle.getEnum(
                    MedDReviewCopayDialog.OPTION_SELECTED_BUNDLE_KEY,
                    MedDReviewCopayDialog.Option.REMOVE_DOSE
                )
            ) {
                RUN_COPAY_CHECK -> {
                    viewModel.saveMetric(
                        MedDCopayCheckRequiredDialogClick(
                            RUN_COPAY_CHECK,
                            appointmentId
                        )
                    )
                }

                REMOVE_DOSE -> {
                    viewModel.saveMetric(
                        MedDCopayCheckRequiredDialogClick(
                            REMOVE_DOSE,
                            appointmentId
                        )
                    )
                }
            }
        }

        destination.goToMedDReviewCopay(
            fragment = this@CheckoutPatientFragment,
            appointmentId = appointmentId,
            antigen = product.product.antigen,
            isCheckMbi = appointmentSessionData.isCheckMbi,
            isMedDCheckStartedAlready = viewModel.isMedDCheckRunStarted
        )
    }

    private fun showAgeWarningBooleanPrompt(title: String, message: String) {
        observeDialogResult(AgeWarningBooleanPromptDialog.DIALOG_RESULT_KEY)
        getResultLiveData<DialogAction>(AgeWarningBooleanPromptDialog.DIALOG_RESULT_KEY)
            ?.observe(viewLifecycleOwner) { action ->
                analytics.saveMetric(
                    AgeWarningPromptAnsweredMetric(
                        visitId = appointmentId,
                        promptTitle = title,
                        promptMessage = message,
                        userSelection = action.toString()
                    )
                )
                if (action == DialogAction.POSITIVE) {
                    // currently, COVID is the only user of this new prompt
                    appointmentViewModel.currentCheckout.riskFactors += RiskFactor.COVID_UNDER_65
                }
            }
        destination.toAgeWarningBooleanPromptDialog(this, title, message)
    }

    private fun showPregnancyWeekSelectorBottomDialog(title: String) {
        observeDialogResult(WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_KEY)
        val userOptions: ArrayList<PregnancyDurationOptions> = arrayListOf(
            PregnancyDurationOptions.WEEKS_32,
            PregnancyDurationOptions.WEEKS_33,
            PregnancyDurationOptions.WEEKS_34,
            PregnancyDurationOptions.WEEKS_35,
            PregnancyDurationOptions.WEEKS_36,
            PregnancyDurationOptions.DOES_NOT_QUALIFY
        )
        val dialogOptions = BottomDialogOptions(
            listHeader = null,
            isCancelable = false,
            isCondensedHeight = true,
            isLargerTitle = true
        )

        val bottomDialog = BottomDialog.newInstance(
            title = getString(R.string.age_warning_weeks_pregnant_dialog_title),
            values = userOptions.map { it.displayName },
            selectedIndex = -1,
            options = dialogOptions
        )
        bottomDialog.onSelected = { index ->
            setFragmentResult(
                WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_KEY,
                bundleOf(WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_BUTTON to index)
            )

            with(appointmentViewModel.currentCheckout) {
                riskFactors += RiskFactor.RSV_PREGNANT
                weeksPregnant = userOptions[index].value
            }
        }

        appointmentViewModel.currentCheckout.pregnancyPrompt = true
        bottomDialog.show(childFragmentManager, title)
    }

    private fun showOutOfAgeIndicationDialog(appointment: Appointment, otPayRate: Double?) {
        observeDialogResult(CheckoutOutOfAgeExclusionDialog.OUT_OF_AGE_EXCLUSION_DIALOG_RESULT_KEY)
        destination.showPromoDialog(
            this,
            appointment,
            R.id.action_checkoutVaccineFragment_to_checkoutOutOfAgeExclusionDialog,
            otPayRate
        )
    }

    private fun showScannedDoseIssueDialog(vaccineWithIssues: VaccineWithIssues, issue: ProductIssue) {
        appointmentViewModel.currentCheckout.pendingProduct = vaccineWithIssues
        observeDialogResult(ScannedDoseIssueDialog.SCANNED_PRODUCT_ISSUE_DIALOG_RESULT_KEY)
        destination.toScannedDoseIssue(this, issue)
    }

    private fun onDoseInSeriesVerify(
        lotNumberProduct: LotNumberWithProduct,
        callback: (lotNumberProduct: LotNumberWithProduct) -> Unit
    ) {
        lifecycleScope.safeLaunch(Dispatchers.IO) {
            val mapping = viewModel.findMapping(lotNumberProduct.productId)
            lotNumberProduct.dosesInSeries = mapping?.dosesInSeries ?: 1

            lotNumberProduct.oneTouch =
                viewModel.findOneTouch(lotNumberProduct.productId)

            safeContext(Dispatchers.Main) {
                callback.invoke(lotNumberProduct)
            }
        }
    }

    private fun alignCopays(medDInfo: MedDInfo?) {
        vaccineItemAdapter.updateItems { list ->
            list.onEach {
                if (it is VaccineAdapterProductDto) {
                    it.copay = medDInfo.getValidCopay(it.product.antigen)
                }
            }
        }
    }

    private fun setupLotNumberListener() {
        productViewModel.lotNumberSelection.observe(viewLifecycleOwner) { lotNumberWithProduct ->
            if (lotNumberWithProduct != null) {
                handleLotNumberWithProduct(lotNumberWithProduct, emptyList())
                viewModel.saveProductSelectionMetric(
                    lotNumberWithProduct = lotNumberWithProduct,
                    relativeDoS = appointmentViewModel.currentCheckout.selectedAppointment?.getRelativeDoS(),
                    appointmentId = appointmentId
                )
                productViewModel.lotNumberSelection.value = null
            }
        }
    }

    private fun updateItemBySelectSite(site: Site.SiteValue, position: Int) {
        vaccineItemAdapter.updateSite(site, position)
    }

    private fun updateItemBySelectRoute(route: RouteCode, position: Int) {
        vaccineItemAdapter.updateRouteAtPosition(route, position)
    }

    private fun getSitesByProduct(product: Product?): List<Site.SiteValue> {
        return Site.siteMap[product?.routeCode?.name]?.map { it } ?: emptyList()
    }

    override fun onDestroyView() {
        binding?.rvVaccines?.adapter = null
        productViewModel.lotNumberSelection.removeObservers(viewLifecycleOwner)
        productViewModel.lotNumberSelection.value = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        appointmentViewModel.currentCheckout.stagedProducts.clear()
        appointmentViewModel.currentCheckout.isEditCheckoutFromComplete = false
        super.onDestroy()
    }

    override fun onSiteClicked(product: VaccineAdapterProductDto, position: Int) {
        viewModel.saveMetric(
            CheckoutDoseClickMetric(
                appointmentId,
                product.product.displayName,
                product.lotNumber,
                CheckoutDoseClickMetric.Click.SET_SITE
            )
        )

        val sites = ArrayList(getSitesByProduct(product.product))
        if (sites.isEmpty()) {
            return
        }

        val selectedIndex = sites.indexOfFirst { it == product.site }
        val bottomDialog = BottomDialog.newInstance(
            title = getString(R.string.patient_lookup_site),
            values = sites.map { it.displayName },
            selectedIndex = selectedIndex
        )
        bottomDialog.onSelected = { index ->
            viewModel.saveMetric(
                SetSiteDialogClickMetric(
                    patientVisitId = appointmentId,
                    productName = product.product.displayName,
                    lotNumber = product.lotNumber,
                    siteValue = sites[index]
                )
            )
            updateItemBySelectSite(
                site = sites[index],
                position = position
            )
        }

        bottomDialog.show(
            childFragmentManager,
            getString(R.string.patient_lookup_site)
        )
    }

    override fun onRouteClicked(product: VaccineAdapterProductDto, position: Int) {
        viewModel.saveMetric(
            CheckoutDoseClickMetric(
                appointmentId,
                product.product.displayName,
                product.lotNumber,
                CheckoutDoseClickMetric.Click.SET_ROUTE
            )
        )

        val routes: ArrayList<RouteCode> = arrayListOf(RouteCode.IM, RouteCode.SC)
        val selectedIndex = routes.indexOfFirst { it == product.product.routeCode }

        val bottomDialog = BottomDialog.newInstance(
            title = getString(R.string.patient_lookup_route),
            values = routes.map { it.displayName },
            selectedIndex = selectedIndex
        )
        bottomDialog.onSelected = { index ->
            viewModel.saveMetric(
                SetRouteDialogClick(
                    patientVisitId = appointmentId,
                    productName = product.product.displayName,
                    lotNumber = product.lotNumber,
                    route = routes[index]
                )
            )

            updateItemBySelectRoute(
                route = routes[index],
                position = position
            )
        }

        bottomDialog.show(
            childFragmentManager,
            getString(R.string.patient_lookup_route)
        )
    }

    override fun onAdapterChanged() {
        hideLoading()
        binding?.fabNext?.isEnabled = isHasEdited()
        updateDoseUsingStatus(
            vaccineItemAdapter.vaccineItems
                .any { it.doseState != DoseState.ORDERED }
        )
    }

    /*
     * Here is Ordered/ No Doses Scanned,but now not sure which is Ordered / Other Doses Scanned
     */
    private fun addAdministeredVaccinesToView(appointment: Appointment, verifier: ProductVerifier) {
        viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.IO) {
            val administeredVaccines = appointment.administeredVaccines.filter { !it.isDeleted }
            val vaccinesToAdd = safeContext(Dispatchers.IO) {
                administeredVaccines.mapNotNull { administeredVaccine ->
                    if (vaccineItemAdapter.vaccineItems.filterIsInstance<VaccineAdapterProductDto>()
                            .any { it.lotNumber == administeredVaccine.lotNumber }
                    ) {
                        null
                    } else {
                        val lotNumberProduct =
                            viewModel.findLotNumberByNameAndProductId(
                                administeredVaccine.lotNumber,
                                administeredVaccine.productId
                            )
                        if (lotNumberProduct == null) {
                            Timber.e("lot number not found for ${administeredVaccine.lotNumber}!")
                        }

                        lotNumberProduct?.let { prod ->
                            prod.oneTouch = viewModel.findOneTouch(lotNumberProduct.productId)

                            val mapping = viewModel.findMapping(lotNumberProduct.productId)
                            prod.dosesInSeries = mapping?.dosesInSeries ?: 1

                            val orderNumber =
                                if (appointmentSessionData.isRprdAndNotLocallyCreated) {
                                    appointment.orders.firstOrNull {
                                        it.patientVisitId == appointmentId &&
                                            prod.salesProductId in it.satisfyingProductIds
                                    }?.orderNumber
                                } else {
                                    null
                                }

                            val vaccineWithIssues = verifier.getProductIssues(
                                productToCheck = lotNumberProduct,
                                appointment = appointment,
                                stagedProducts = vaccineItemAdapter.vaccineItems,
                                doseSeries = administeredVaccine.doseSeries,
                                manualDob = appointmentViewModel.currentCheckout.manualDob,
                                initialDoseState = DoseState.ADMINISTERED,
                                simpleOnHandProductInventory = viewModel.getSimpleOnHandInventoryByProductLotName(
                                    lotNumberProduct.name
                                ),
                                isVaxCare3 = isVaxCare3,
                                medDInfo = null
                            )
                            vaccineWithIssues?.overrideSiteAndRoute(
                                site = administeredVaccine.site,
                                route = administeredVaccine.method
                            )
                            vaccineWithIssues?.orderNumber = orderNumber
                            vaccineWithIssues?.toAdapterDto()?.apply {
                                isDeleted = administeredVaccine.isDeleted
                            }
                        }
                    }
                }
            }

            val toAdd = vaccinesToAdd.sortedWith(
                compareBy(
                    { (it as? VaccineAdapterProductDto)?.isDeleted == true },
                    { it.product.displayName.lowercase(Locale.US) },
                    { it.weight }
                )
            )

            withContext(Dispatchers.Main) {
                vaccineItemAdapter.addAllItems(toAdd)
            }
        }
    }

    private fun addOrdersToView(appointment: Appointment?) {
        if (appointmentSessionData.isRprdAndNotLocallyCreated) {
            viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.IO) {
                val currentDate = LocalDateTime.now()
                val orderDtos = appointment?.orders
                    ?.filter { order -> orderCanBeConsidered(appointment, order) }
                    ?.mapNotNull { order ->
                        if (order.expirationDate > currentDate) {
                            viewModel.findProductsBySalesProductIds(order.satisfyingProductIds)
                                ?.firstOrNull()?.let { product ->
                                    val mapping = productViewModel.findMapping(product.id)
                                    VaccineAdapterOrderDto(
                                        hasEdit = false,
                                        product = product,
                                        order = order,
                                        orderNumber = order.orderNumber,
                                        salesProductId = mapping?.id ?: product.id
                                    )
                                }
                        } else {
                            null
                        }
                    }

                val ordersToAdd = orderDtos?.filter {
                    val valid =
                        isOrderValidToAdd(
                            administeredVaccines = appointment.administeredVaccines,
                            order = it.order
                        )

                    it.doseState = DoseState.ORDERED

                    valid
                }

                withContext(Dispatchers.Main) {
                    vaccineItemAdapter.addAllItems(ordersToAdd ?: emptyList())
                }
            }
        }
    }

    private fun orderCanBeConsidered(appointment: Appointment, order: OrderEntity): Boolean =
        order.patientVisitId == null || order.patientVisitId == appointment.id

    private fun isOrderValidToAdd(administeredVaccines: List<AdministeredVaccine>, order: OrderEntity?): Boolean {
        val productNotInCart =
            !administeredVaccines.any { vac -> order?.satisfyingProductIds?.contains(vac.productId) == true }
        val orderNotInCart =
            !vaccineItemAdapter.vaccineItems.any { it.orderNumber == order?.orderNumber }
        val salesProductNotInCart =
            !vaccineItemAdapter.vaccineItems.any {
                order?.satisfyingProductIds?.contains(it.salesProductId) == true
            }
        return productNotInCart && orderNotInCart && salesProductNotInCart
    }

    private fun isHasEdited() = vaccineItemAdapter.vaccineItems.any { it.hasEdit && it.doseState != DoseState.ORDERED }

    private fun validateCheckoutAndNavigate() {
        val appointment = appointmentViewModel.currentCheckout.selectedAppointment
        if (features.isCreditCardCaptureDisabled && isVaxCare3) {
            changeSelfPayItemsToPartnerBill()
        }
        saveCheckoutWithProduct()
        when {
            appointmentSessionData.isRprdAndNotLocallyCreated -> populateOrderProblems(appointment)
            else -> populateInfoProblems(appointment)
        }
    }

    private fun handleCheckoutDestinations(destinations: List<CheckoutTerminus>) {
        when {
            destinations.isNotEmpty() -> {
                destinations.filterIsInstance<DoseReasons>().firstOrNull()?.let {
                    destination.toDoseReasonFragment(this@CheckoutPatientFragment, destinations)
                } ?: kotlin.run {
                    destinations.filterIsInstance<MissingInfo>().firstOrNull()?.let { missingInfo ->
                        when (missingInfo.invalidInfoWrapper.infoType) {
                            is InfoType.Demographic -> {
                                if (features.isCheckoutDemographicsCollectionDisabled) {
                                    destination.toCheckoutSummary(
                                        this@CheckoutPatientFragment,
                                        appointmentId
                                    )
                                } else {
                                    destination.toPatientInfoScreen(
                                        this@CheckoutPatientFragment,
                                        missingInfo
                                    )
                                }
                            }

                            is InfoType.Payer -> navigateToPayerFlow(missingInfo)

                            is InfoType.Both -> {
                                if (features.isCheckoutDemographicsCollectionDisabled) {
                                    navigateToPayerFlow(missingInfo)
                                } else {
                                    destination.toPatientInfoScreen(
                                        this@CheckoutPatientFragment,
                                        missingInfo
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> destination.toCheckoutSummary(
                fragment = this@CheckoutPatientFragment,
                appointmentId = appointmentId
            )
        }
    }

    private fun navigateToPayerFlow(missingInfo: MissingInfo) {
        val isNewInsuranceNavigation =
            !features.isNewInsurancePromptDisabled &&
                !missingInfo.invalidInfoWrapper.infoType.fields.isMissingPayerFields()

        when {
            isNewInsuranceNavigation -> destination.toNewPayerScreen(
                fragment = this@CheckoutPatientFragment,
                missingInfo = missingInfo
            )

            features.isSelectPayorEnabled -> destination.toSelectPayerScreen(
                fragment = this@CheckoutPatientFragment,
                infoWrapper = missingInfo.invalidInfoWrapper
            )

            !features.isInsuranceScanDisabled -> destination.goToScanInsuranceScreen(this@CheckoutPatientFragment)
            !features.isInsurancePhoneCollectDisabled -> goToInsurancePhoneCollection()
            else -> destination.toCheckoutSummary(
                this@CheckoutPatientFragment,
                appointmentId
            )
        }
    }

    private fun goToInsurancePhoneCollection() {
        appointmentViewModel.currentCheckout.insuranceCollectionMethod =
            CheckoutInsuranceCardCollectionFlow.InsuranceCardCollectionMethod.PHONE_CAPTURE
        appointmentViewModel.currentCheckout.selectedAppointment?.let { appointment ->
            destination.goToInsurancePhoneCollection(
                this@CheckoutPatientFragment,
                appointmentId = appointmentId,
                patientId = appointment.patient.id,
                currentPhone = appointment.patient.phoneNumber
            )
        } ?: throw Exception("selected appointment was null when navigating to phone collect")
    }

    /**
     * Populates the Order issues Terminus for the Checkout flow.
     */
    private fun populateOrderProblems(appointment: Appointment?) {
        val orderIssues = extractOrderIssues()
        appointmentViewModel.currentCheckout.orderWrapper = when {
            orderIssues.all { it == DoseReasonContext.NONE } -> null
            else -> buildOrderProductWrapper(orderIssues)
        }

        populateInfoProblems(appointment)
    }

    private fun extractOrderIssues() =
        setOf(
            if (vaccineItemAdapter.vaccineItems
                    .any { it.doseState == DoseState.ORDERED }
            ) {
                DoseReasonContext.ORDER_UNFILLED
            } else {
                DoseReasonContext.NONE
            },
            if (vaccineItemAdapter.vaccineItems
                    .any { !it.isRemoveDoseState() && it.orderNumber == null }
            ) {
                DoseReasonContext.DOSES_NOT_ORDERED
            } else {
                DoseReasonContext.NONE
            }
        )

    /**
     * Builds the MissingInfo terminus if applicable
     * @param appointment - associated appointment
     */
    private fun populateInfoProblems(appointment: Appointment?) {
        val terminusList = appointment?.encounterState?.let { state ->
            val infoType =
                if (!appointment.checkedOut && state.vaccineMessage?.status in listOf(
                        AppointmentStatus.AT_RISK_DATA_MISSING,
                        AppointmentStatus.AT_RISK_DATA_INCORRECT
                    )
                ) {
                    when {
                        state.vaccineMessage?.callToAction == CallToAction.PatientAndInsuranceData ->
                            InfoType.Both(
                                appointment.getAllRequiredDemographicFields() +
                                    appointment.getAllRequiredPayerFields()
                            )

                        state.hasMissingDemoInfo ->
                            InfoType.Demographic(appointment.getAllRequiredDemographicFields())

                        state.hasMissingPayerInfo ->
                            InfoType.Payer(appointment.getAllRequiredPayerFields())

                        else -> null
                    }
                } else {
                    null
                }
            listOf(
                appointmentViewModel.currentCheckout.orderWrapper?.let { DoseReasons(it) },
                infoType?.let {
                    buildMissingInfoTerminus(
                        it,
                        appointment.id,
                        appointment.patient.id
                    )
                }
            )
        }

        handleCheckoutDestinations(terminusList?.mapNotNull { it } ?: emptyList())
    }

    private fun redirectToCheckoutSummary() {
        destination.redirectToCheckoutSummary(
            fragment = this@CheckoutPatientFragment,
            appointmentId = appointmentId
        )
    }

    /**
     * Build CheckoutTerminus for missing info with the associate type and apptId
     */
    private fun buildMissingInfoTerminus(
        type: InfoType,
        appointmentId: Int,
        patientId: Int
    ) = MissingInfo(
        InvalidInfoWrapper(
            infoType = type,
            appointmentId = appointmentId,
            patientId = patientId
        )
    )

    private fun buildOrderProductWrapper(orderIssues: Set<DoseReasonContext>): OrderProductWrapper {
        val contexts = orderIssues.filter { it != DoseReasonContext.NONE }
        val ordersNotAdministered = vaccineItemAdapter.vaccineItems
            .filterIsInstance<VaccineAdapterOrderDto>()
            .filter { it.doseState == DoseState.ORDERED }
            .map {
                with(it.product) {
                    OrderDose(
                        rawDisplay = it.order.shortDescription,
                        salesProductId = it.salesProductId,
                        orderNumber = it.orderNumber,
                        iconResId = presentation.icon,
                        reasonContext = DoseReasonContext.ORDER_UNFILLED
                    )
                }
            }
        val unorderedProducts = vaccineItemAdapter.vaccineItems
            .filter { it.orderNumber == null && !it.isRemoveDoseState() }
            .map {
                with(it.product) {
                    OrderDose(
                        rawDisplay = displayName,
                        salesProductId = it.salesProductId,
                        orderNumber = it.orderNumber,
                        iconResId = presentation.icon,
                        reasonContext = DoseReasonContext.DOSES_NOT_ORDERED
                    )
                }
            }

        return OrderProductWrapper(
            reasonContexts = contexts.toSet(),
            products = ordersNotAdministered + unorderedProducts,
            appointmentId = appointmentId
        )
    }

    private fun changeSelfPayItemsToPartnerBill() {
        val appointmentIsSelfPay =
            appointmentViewModel.currentCheckout.selectedAppointment?.paymentMethod == PaymentMethod.SelfPay
        vaccineItemAdapter.vaccineItems.filterIsInstance<VaccineAdapterProductDto>()
            .filter { appointmentIsSelfPay || it.hasCopay() || it.isSelfPayAndNonZeroRate() }
            .forEach {
                if (it.appointmentPaymentMethod == PaymentMethod.SelfPay || it.paymentMode == PaymentMode.SelfPay) {
                    it.paymentModeReason = PaymentModeReason.SelfPayOptOut
                }
                it.overridePaymentModeAndOriginalPaymentMode(PaymentMode.PartnerBill)
            }
    }

    private fun saveCheckoutWithProduct() {
        appointmentViewModel.currentCheckout.stagedProducts =
            vaccineItemAdapter.vaccineItems.filterIsInstance<VaccineAdapterProductDto>()
                .toMutableList()
    }

    private fun hideLoading() {
        endLoading()
        binding?.loadingBackground?.hide()
        binding?.patientInfo?.show()
    }

    private fun saveDuplicateProductBannerMetric(appointmentId: Int, lotNumberProduct: LotNumberWithProduct) {
        analytics.saveMetric(
            DuplicateProductBannerMetric(
                visitId = appointmentId,
                lotNumber = lotNumberProduct.name,
                productId = lotNumberProduct.productId,
                salesProductId = lotNumberProduct.salesProductId
            )
        )
    }

    override fun onStop() {
        if (receiverRegistered) {
            context?.unregisterReceiver(appointmentChangedEventReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun handleBack(): Boolean {
        onClose()
        return true
    }

    private fun leaveAppointment() {
        if (!destination.popBackToCurbsidePatientSearch(this)) {
            globalDestinations.goBackToAppointmentList(this)
        }
    }

    override fun reportProductSelectionMetric(productSelectionMetric: ProductSelectionMetric) {
        val metric = productSelectionMetric.apply {
            patientVisitId = appointmentId
            relativeDoS =
                appointmentViewModel.currentCheckout.selectedAppointment?.getRelativeDoS()
        }
        viewModel.saveProductSelectionMetric(metric)
    }

    private class StockOptionsDisplayOrderComparator(
        private val desiredOrder: List<CheckoutStockOption> = listOf(
            PRIVATE,
            VFC_ENROLLED_IN_MEDICAID,
            VFC_UNINSURED,
            VFC_UNDERINSURED,
            VFC_AMERICAN_INDIAN_OR_ALASKA_NATIVE,
            STATE,
            THREE_SEVENTEEN
        )
    ) : Comparator<CheckoutStockOption> {
        override fun compare(p0: CheckoutStockOption?, p1: CheckoutStockOption?): Int {
            val index1 = desiredOrder.indexOf(p0).takeIf { it != -1 } ?: Int.MAX_VALUE
            val index2 = desiredOrder.indexOf(p1).takeIf { it != -1 } ?: Int.MAX_VALUE
            return index1.compareTo(index2)
        }
    }

    /**
     * Object to hold the data loaded from initialization
     *
     * @property isRprdAndNotLocallyCreated RPRD is on and the appointment was not just created
     * @property isDisableDuplicateRSV DisableDoubleRSVDoses flag is on
     * @property isCheckMbi EnableMbiForMedicatePartD flag is on
     * @property dataCollectedBeforeCheckout data was collected before checkout aka "softRiskFree"
     * @property stockSelectorEnabled MobileStockSelector flag is on along with public stocks
     * @property checkoutStockList list of stock options to show on the stock selector
     * @property currentAppointmentStock stock of the current appointment associated with the vaccineSupply
     */
    private data class CheckoutSessionData(
        var isRprdAndNotLocallyCreated: Boolean = false,
        var isDisableDuplicateRSV: Boolean = false,
        var isCheckMbi: Boolean = false,
        // softRiskFree
        var dataCollectedBeforeCheckout: Boolean = false,
        // stock selector specific
        var stockSelectorEnabled: Boolean = false,
        var checkoutStockList: List<CheckoutStockOption> = emptyList(),
        var currentAppointmentStock: CheckoutStockOption? = null
    )
}
