/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.ProductSelectionMetric
import com.vaxcare.core.report.model.checkout.RelativeDoS
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.MMR_II
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.PRO_QUAD
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.VARIVAX
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.extension.isDuplicateRSVDisabled
import com.vaxcare.vaxhub.core.extension.isMaskedSsnFormatted
import com.vaxcare.vaxhub.core.extension.isMbiFormatAndNotNull
import com.vaxcare.vaxhub.core.extension.isMedDAutoRunEnabled
import com.vaxcare.vaxhub.core.extension.isMobileStockSelectorEnabled
import com.vaxcare.vaxhub.core.extension.isRightPatientRightDoseEnabled
import com.vaxcare.vaxhub.core.extension.isVaxCare3Enabled
import com.vaxcare.vaxhub.core.extension.to
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.core.extension.trimDashes
import com.vaxcare.vaxhub.core.model.enums.MedDIDType
import com.vaxcare.vaxhub.core.model.enums.MedDIDType.MBI
import com.vaxcare.vaxhub.core.model.enums.MedDIDType.SSN
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.MedDCheckRequestBody
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.model.VaccineAdapterDto
import com.vaxcare.vaxhub.model.VaccineWithIssues
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.checkout.CheckoutOptions
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.checkout.getValidCopay
import com.vaxcare.vaxhub.model.checkout.toCheckoutStockOptions
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProduct
import com.vaxcare.vaxhub.model.inventory.validator.HubFeatures
import com.vaxcare.vaxhub.model.inventory.validator.ProductVerifier
import com.vaxcare.vaxhub.model.metric.CheckoutAppointmentRefreshAttemptMetric
import com.vaxcare.vaxhub.model.metric.CheckoutStockOptionSelectedMetric
import com.vaxcare.vaxhub.model.metric.MedDCheckTriggeredMetric
import com.vaxcare.vaxhub.model.metric.MedDCheckTriggeredMetric.TriggeredMode.AUTO_RUN
import com.vaxcare.vaxhub.model.metric.StockChangeRetryPromptShownMetric
import com.vaxcare.vaxhub.model.metric.StockChangeTimeOutMetric
import com.vaxcare.vaxhub.model.patient.PayerField
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.OrdersRepository
import com.vaxcare.vaxhub.repository.OrdersRepository.Companion.SyncContextFrom.REFRESH_APPOINTMENT
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.ShotAdministratorRepository
import com.vaxcare.vaxhub.repository.SimpleOnHandInventoryRepository
import com.vaxcare.vaxhub.repository.WrongProductRepository
import com.vaxcare.vaxhub.service.PartDService
import com.vaxcare.vaxhub.ui.checkout.dialog.issue.DialogIssueListener
import com.vaxcare.vaxhub.ui.checkout.dialog.issue.DialogIssueMachine
import com.vaxcare.vaxhub.ui.checkout.dialog.issue.ProductIssueOperator
import com.vaxcare.vaxhub.ui.checkout.dialog.issue.ProductPendingUserAction
import com.vaxcare.vaxhub.ui.checkout.dialog.issue.ProductPendingUserResult
import com.vaxcare.vaxhub.ui.checkout.extensions.toCheckoutAppointmentOpenedMetric
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel
import com.vaxcare.vaxhub.util.launchWithTimeoutIterations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CheckoutPatientViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val shotAdministratorRepository: ShotAdministratorRepository,
    private val orderRepository: OrdersRepository,
    @MHAnalyticReport private val analytics: AnalyticReport,
    productRepository: ProductRepository,
    locationRepository: LocationRepository,
    wrongProductRepository: WrongProductRepository,
    private val simpleOnHandInventoryRepository: SimpleOnHandInventoryRepository,
    private val dispatcherProvider: DispatcherProvider,
    partDService: PartDService
) : BaseScannerViewModel(locationRepository, productRepository, wrongProductRepository),
    PartDService by partDService {
    companion object {
        private const val ACE_TIMEOUT_ITERATION = 2
        private const val ACE_TIMEOUT_MILLIS = 10_000L
        private const val PRIVATE_RISK_FREE_SECONDARY_MESSAGE = "Ready (Risk Free)"
        private const val PUBLIC_RISK_FREE_SECONDARY_MESSAGE_FMT = "%s Patient"
        private const val RISK_FREE_PRIMARY_MESSAGE = "Ready to Vaccinate"
    }

    val deletedItem = MutableLiveData(-1)

    private var cachedAppointment: Appointment? = null
    private var cachedAdmin: ShotAdministrator? = null
    private val appointmentIsMedDFlow = MutableStateFlow<Boolean>(false)

    var medDCheckStartedAt: LocalDateTime? = null
    private var aceTimeoutJob: Job? = null

    private val _uiFlow = MutableSharedFlow<State>(replay = 3, extraBufferCapacity = 3)
    val uiFlow = _uiFlow.asSharedFlow()

    private var aceWaitTimeMillisStart: Long = 0

    sealed class CheckoutPatientState : State {
        object ErrorGettingAppointment : CheckoutPatientState()

        data class AppointmentLoaded(
            val appointment: Appointment,
            val flags: List<FeatureFlag>,
            val shotAdmin: ShotAdministrator?,
            val availableCheckoutOptions: List<CheckoutOptions>?,
            val inventorySources: List<CheckoutStockOption>,
            val isForceRefresh: Boolean
        ) : CheckoutPatientState()

        data class ProductVerified(
            val appointment: Appointment,
            val vaccineWithIssues: VaccineWithIssues?
        ) : CheckoutPatientState()

        data class ProductHasIssues(
            val appointment: Appointment,
            val vaccineWithIssues: VaccineWithIssues,
            val issue: ProductIssue
        ) : CheckoutPatientState() {
            override fun equals(other: Any?): Boolean {
                if (other is ProductHasIssues) {
                    return this.issue == other.issue
                }
                return super.equals(other)
            }

            override fun hashCode(): Int = issue.hashCode()
        }

        object ProductAddCanceled : CheckoutPatientState()

        object PendingUserActionStock : CheckoutPatientState()

        data class AppointmentRefreshed(
            val appointment: Appointment,
            val shotAdmin: ShotAdministrator?,
            val availableCheckoutOptions: List<CheckoutOptions>?,
            val inventorySources: List<CheckoutStockOption>
        ) : CheckoutPatientState()

        data class AppointmentAbandoned(
            val success: Boolean
        ) : CheckoutPatientState()

        object AceSavingInfo : CheckoutPatientState()

        object AceTimingOut : CheckoutPatientState()

        /**
         * State to apply when there was an error with the patient patch request
         *
         * @property selectedStockId id of the stock selected
         * @property selectedFinancialClass class of vfc financial if applicable
         */
        data class StockError(
            val selectedStockId: Int,
            val selectedFinancialClass: String?
        ) : CheckoutPatientState()

        /**
         * State to apply when the 20 second timeout has elapsed waiting for ACE
         *
         * @property selectedStockId id of the stock selected
         * @property selectedFinancialClass class of vfc financial if applicable
         * @property riskFreePaymentMode
         * @property riskFreeAppointment new appointment to send that has been edited
         */
        data class StockTimeout(
            val selectedStockId: Int,
            val selectedFinancialClass: String?,
            val riskFreePaymentMode: PaymentMode,
            val riskFreeAppointment: Appointment
        ) : CheckoutPatientState()

        /**
         * State for updating the appointment after ACE is received
         */
        data class AppointmentUpdated(val appointment: Appointment) : CheckoutPatientState()
    }

    private val productDialogIssueMachine = DialogIssueMachine()
    private val routeOverrideAntigens = listOf(VARIVAX, PRO_QUAD, MMR_II)
    private var medDAutoRunStatus = MutableStateFlow<MedDAutoRunStatus>(MedDAutoRunStatus.INIT)
    var isMedDCheckRunStarted = false

    private enum class MedDAutoRunStatus {
        INIT,
        HAS_FLAG,
        IS_RUNNING,
        HAS_RAN
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun latestMedDInfo() =
        combine(
            flow = medDInfo,
            flow2 = appointmentIsMedDFlow,
            flow3 = medDAutoRunStatus,
            transform = ::Triple
        ).flatMapLatest { (medDInfo, isMedDAppt, autoRunStatus) ->
            if (isMedDAppt) {
                when (autoRunStatus) {
                    MedDAutoRunStatus.IS_RUNNING -> flowOf()
                    MedDAutoRunStatus.INIT,
                    MedDAutoRunStatus.HAS_RAN -> flowOf(medDInfo to true)

                    MedDAutoRunStatus.HAS_FLAG -> when {
                        medDInfo != null -> flowOf(medDInfo to true)
                        !isMedDCheckRunStarted -> {
                            cachedAppointment.startMedDAutoRun()
                            flowOf()
                        }

                        else -> flowOf()
                    }
                }
            } else {
                flowOf()
            }
        }

    fun fetchLocalAppointmentDataDueToRiskFree(appointmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val flags = locationRepository.getFeatureFlagsAsync()
            val inventorySources: List<InventorySource> =
                locationRepository.getInventorySourcesAsync()
                    ?: listOf(InventorySource.PRIVATE).also {
                        Timber.e("Inventory Sources table is empty! Returning Private as fallback")
                    }

            if (cachedAppointment == null) {
                val appointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)

                val administrator = shotAdministratorRepository.getAllAsync().firstOrNull {
                    it.id == appointment?.administeredBy
                }

                val state = appointment?.let {
                    cachedAppointment = it
                    appointmentIsMedDFlow.update { _ -> it.isMedDAndDateOfService() }
                    cachedAdmin = administrator
                    analytics.saveMetric(it.toCheckoutAppointmentOpenedMetric(isForceRiskFree = true))
                    CheckoutPatientState.AppointmentLoaded(
                        appointment = it,
                        flags = flags,
                        shotAdmin = administrator,
                        inventorySources = inventorySources.toCheckoutStockOptions().toList(),
                        availableCheckoutOptions = getAvailableCheckoutOptions(
                            inventorySources = inventorySources,
                            featureFlags = flags,
                            isAppointmentCheckedOut = it.checkedOut
                        ),
                        isForceRefresh = false
                    )
                } ?: CheckoutPatientState.ErrorGettingAppointment
                setState(state)
            } else {
                cachedAppointment?.let {
                    setUiState(
                        CheckoutPatientState.AppointmentRefreshed(
                            appointment = it,
                            shotAdmin = cachedAdmin,
                            inventorySources = inventorySources.toCheckoutStockOptions().toList(),
                            availableCheckoutOptions = getAvailableCheckoutOptions(
                                inventorySources = inventorySources,
                                featureFlags = flags,
                                isAppointmentCheckedOut = it.checkedOut
                            )
                        )
                    )
                } ?: run {
                    fetchLocalAppointmentDataDueToRiskFree(appointmentId)
                }
            }
        }
    }

    /**
     * Fetches the appointment from the DAO after the ACE is received and sets the state
     */
    fun fetchUpdatedAppointmentAfterAce(appointmentId: Int) =
        viewModelScope.launch {
            cancelPendingAceJob()
            cachedAppointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)
            appointmentIsMedDFlow.update { _ -> cachedAppointment?.isMedDAndDateOfService() == true }
            val state = cachedAppointment?.let { appointment ->
                CheckoutPatientState.AppointmentUpdated(appointment)
            } ?: Reset
            setUiState(state)
        }

    fun refreshAppointment(appointmentId: Int) {
        setUiState(LoadingState)
        viewModelScope.launch(dispatcherProvider.io) {
            val fakeLoadingDelayJob = launch {
                delay(2_000L)
            }

            try {
                orderRepository.syncOrdersChanges(syncContextFrom = REFRESH_APPOINTMENT)
                fetchUpdatedAppointment(appointmentId = appointmentId, forceFetch = true)

                fakeLoadingDelayJob.cancel()

                saveMetric(
                    CheckoutAppointmentRefreshAttemptMetric(
                        patientVisitId = appointmentId,
                        success = true
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed trying to sync order changes")
                fakeLoadingDelayJob.join()

                fetchUpdatedAppointment(appointmentId = appointmentId, forceFetch = false)
                saveMetric(
                    CheckoutAppointmentRefreshAttemptMetric(
                        patientVisitId = appointmentId,
                        success = false
                    )
                )
            }
        }
    }

    fun fetchUpdatedAppointment(
        appointmentId: Int,
        medDCoverageNotFoundId: Int? = null,
        medDNoCoverageId: Int? = null,
        forceFetch: Boolean = false
    ) {
        viewModelScope.launch(dispatcherProvider.io) {
            val flags = locationRepository.getFeatureFlagsAsync()
            val inventorySources = locationRepository.getInventorySourcesAsync() ?: listOf(
                InventorySource.PRIVATE
            ).also {
                Timber.e("Inventory Sources table is empty! Returning Private as fallback")
            }

            if (forceFetch || cachedAppointment == null) {
                setUiState(LoadingState)
                val shotAdmins = shotAdministratorRepository.getAllAsync()
                var appointmentDetails = appointmentRepository
                    .getAndInsertUpdatedAppointment(appointmentId)?.toAppointment()

                // Populate orders from room
                if (flags.isRightPatientRightDoseEnabled()) {
                    appointmentDetails?.patient?.id?.let { patientId ->
                        appointmentDetails?.orders = orderRepository.getOrdersByPatient(patientId)
                    }
                }

                if (appointmentDetails == null) {
                    appointmentDetails =
                        appointmentRepository.getAppointmentByIdAsync(appointmentId)
                }

                val administrator =
                    shotAdmins.firstOrNull { it.id == appointmentDetails?.administeredBy }

                val state = appointmentDetails?.let { appointment ->
                    cachedAppointment = appointment
                    cachedAdmin = administrator
                    analytics.saveMetric(
                        appointment.toCheckoutAppointmentOpenedMetric(
                            isForceRiskFree = false
                        )
                    )

                    if (appointment.isMedDAndDateOfService()) {
                        setMedDAppointmentIdAndGetInitialInfo(
                            appointmentId = appointmentId,
                            notCoveredResId = medDNoCoverageId,
                            notFoundResId = medDCoverageNotFoundId
                        )

                        if (flags.isMedDAutoRunEnabled()) {
                            medDAutoRunStatus.update { MedDAutoRunStatus.HAS_FLAG }
                        }

                        appointmentIsMedDFlow.update { _ -> true }
                    }

                    CheckoutPatientState.AppointmentLoaded(
                        appointment = appointment,
                        flags = flags,
                        shotAdmin = administrator,
                        availableCheckoutOptions = getAvailableCheckoutOptions(
                            inventorySources = inventorySources,
                            featureFlags = flags,
                            isAppointmentCheckedOut = appointment.checkedOut,
                        ),
                        inventorySources = inventorySources.toCheckoutStockOptions()
                            .toList(),
                        isForceRefresh = forceFetch,
                    )
                } ?: CheckoutPatientState.ErrorGettingAppointment
                setUiState(state)
            } else {
                cachedAppointment?.let {
                    setUiState(
                        CheckoutPatientState.AppointmentRefreshed(
                            appointment = it,
                            shotAdmin = cachedAdmin,
                            availableCheckoutOptions = getAvailableCheckoutOptions(
                                inventorySources = inventorySources,
                                featureFlags = flags,
                                isAppointmentCheckedOut = it.checkedOut
                            ),
                            inventorySources = inventorySources.toCheckoutStockOptions().toList()
                        )
                    )
                } ?: kotlin.run {
                    fetchUpdatedAppointment(
                        appointmentId = appointmentId,
                        forceFetch = true
                    )
                }
            }
        }
    }

    private fun Appointment?.startMedDAutoRun() =
        this?.let {
            viewModelScope.launch(dispatcherProvider.io) {
                try {
                    val listOfPatientValidMedDIDTypes: List<MedDIDType> =
                        getListOfPatientValidMedDIdTypes()

                    if (listOfPatientValidMedDIDTypes.isNotEmpty()) {
                        val medDIdTypeAndValue: Pair<MedDIDType, String> =
                            when (listOfPatientValidMedDIDTypes.minByOrNull { it.name } ?: MBI) {
                                MBI -> MBI to (patient.paymentInformation?.mbi ?: "")
                                SSN -> SSN to (patient.ssn ?: "")
                            }
                        isMedDCheckRunStarted = true
                        medDAutoRunStatus.update { MedDAutoRunStatus.IS_RUNNING }
                        medDCheckStartedAt = LocalDateTime.now()
                        runMedDCheck(id, medDIdTypeAndValue)

                        analytics.saveMetric(
                            MedDCheckTriggeredMetric(
                                patientVisitId = id,
                                medDIDType = medDIdTypeAndValue.first,
                                triggeredMode = AUTO_RUN
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Something went wrong trying to run Med-D check")
                    isMedDCheckRunStarted = false
                } finally {
                    medDAutoRunStatus.update { MedDAutoRunStatus.HAS_RAN }
                }
            }
        }

    private fun getAvailableCheckoutOptions(
        inventorySources: List<InventorySource>,
        featureFlags: List<FeatureFlag>,
        isAppointmentCheckedOut: Boolean
    ): List<CheckoutOptions>? =
        when {
            isAppointmentCheckedOut -> null
            inventorySources.size > 1 &&
                featureFlags.isMobileStockSelectorEnabled() &&
                featureFlags.isRightPatientRightDoseEnabled() -> listOf(
                CheckoutOptions.REFRESH,
                CheckoutOptions.CHANGE_STOCK
            )

            inventorySources.size > 1 &&
                featureFlags.isMobileStockSelectorEnabled() -> listOf(CheckoutOptions.CHANGE_STOCK)

            featureFlags.isRightPatientRightDoseEnabled() -> listOf(CheckoutOptions.REFRESH)
            else -> null
        }

    override fun resetState() {
        super.resetState()
        _uiFlow.tryEmit(Reset)
    }

    suspend fun findMapping(id: Int) = productRepository.findMapping(id)

    suspend fun findOneTouch(id: Int) = productRepository.findOneTouch(id)

    /**
     * returns LotNumberWithProduct by lotNumberName if it exists
     * if for some reason that lotNumberName is deleted, build the LotNumberWithProduct by looking
     * up product details and wrapping them with LotNumberName
     *
     * @param name
     * @param salesProductId
     * @return
     */
    suspend fun findLotNumberByNameAndProductId(name: String, salesProductId: Int): LotNumberWithProduct? {
        return productRepository.findLotNumberByNameAsync(name) ?: run {
            productRepository.findProductsBySalesProductIds(listOf(salesProductId))
                ?.let { products ->
                    if (products.size == 1) {
                        products[0].let { product ->
                            LotNumberWithProduct(
                                expirationDate = null,
                                id = 0,
                                name = name,
                                productId = product.id,
                                salesLotNumberId = 0,
                                salesProductId = salesProductId,
                                ageIndications = product.ageIndications,
                                cptCodes = product.cptCvxCodes,
                                product = product
                            )
                        }
                    } else {
                        Timber.e("Product id $salesProductId has more than 1 product associated")
                        null
                    }
                }
        }
    }

    suspend fun findProductsBySalesProductIds(ids: List<Int>): List<Product>? =
        productRepository.findProductsBySalesProductIds(ids)

    fun evaluateProductForIssues(
        appointment: Appointment,
        lotNumberWithProduct: LotNumberWithProduct,
        stagedProducts: List<VaccineAdapterDto>,
        doseSeries: Int?,
        manualDob: LocalDate? = null,
        isLocallyCreated: Boolean = false
    ) = viewModelScope.launch(Dispatchers.IO) {
        val location = locationRepository.getLocationAsync()
        val flags = location?.activeFeatureFlags ?: locationRepository.getFeatureFlagsAsync()
        val isRprdAndNotLocallyCreated = !isLocallyCreated && flags.isRightPatientRightDoseEnabled()
        val isDisableDuplicateRSV = flags.isDuplicateRSVDisabled()
        val isVaxCare3 = flags.isVaxCare3Enabled()

        val vaccineWithIssues = ProductVerifier(
            HubFeatures(
                isRprdAndNotLocallyCreated = isRprdAndNotLocallyCreated,
                isDisableDuplicateRSV = isDisableDuplicateRSV
            )
        ).getProductIssues(
            productToCheck = lotNumberWithProduct,
            appointment = appointment,
            stagedProducts = stagedProducts,
            doseSeries = doseSeries,
            manualDob = manualDob,
            simpleOnHandProductInventory = getSimpleOnHandInventoryByProductLotName(
                lotNumberWithProduct.name
            ),
            isVaxCare3 = isVaxCare3,
            medDInfo = runBlocking { medDInfo.firstOrNull() }
        )
        vaccineWithIssues?.let {
            if (productNeedsRouteOverride(it.lotNumberWithProduct.product.antigen)) {
                it.overrideSiteAndRoute(route = RouteCode.SC.name)
            }

            when {
                it.issues.isEmpty() -> {
                    val vaccineWithCopay = runBlocking {
                        vaccineWithIssues.apply {
                            copay = medDInfo
                                .firstOrNull()
                                .getValidCopay(lotNumberWithProduct.product.antigen)
                        }
                    }

                    setUiState(
                        CheckoutPatientState.ProductVerified(
                            appointment = appointment,
                            vaccineWithIssues = vaccineWithCopay
                        )
                    )
                }

                else -> registerIssues(
                    appointment = appointment,
                    stagedProducts = stagedProducts,
                    vaccineWithIssues = vaccineWithIssues,
                    originalParameters = OriginalEvaluateProductParameters(
                        manualDob = manualDob,
                        isLocallyCreated = isLocallyCreated,
                        locationSources = location?.inventorySources ?: emptyList(),
                    )
                )
            }
        } ?: run {
            setUiState(CheckoutPatientState.ErrorGettingAppointment)
        }
    }

    private fun productNeedsRouteOverride(antigen: String): Boolean = routeOverrideAntigens.contains(antigen)

    suspend fun getSimpleOnHandInventoryByProductLotName(productLotName: String): List<SimpleOnHandProduct> =
        viewModelScope.async(Dispatchers.IO) {
            simpleOnHandInventoryRepository.getSimpleOnHandProductsByLotName(productLotName)
        }.await()

    /**
     * Registers the issues in the state-machine with an inline listener to account for the incoming
     * appointment and vaccine
     *
     * @param appointment associated appointment
     * @param vaccineWithIssues associated vaccine we are attempting to add to the cart
     */
    private fun registerIssues(
        appointment: Appointment,
        stagedProducts: List<VaccineAdapterDto>,
        vaccineWithIssues: VaccineWithIssues,
        originalParameters: OriginalEvaluateProductParameters
    ) {
        val operator = ProductIssueOperator(
            appointment = appointment,
            stagedProducts = stagedProducts,
            vaccineWithIssues = vaccineWithIssues,
            analytics = analytics,
            locationSources = originalParameters.locationSources
        )
        productDialogIssueMachine.registerIssues(
            vaccineWithIssues.issues,
            object : DialogIssueListener {
                override fun handleIssue(issue: ProductIssue) {
                    setUiState(
                        CheckoutPatientState.ProductHasIssues(
                            appointment = appointment,
                            vaccineWithIssues = vaccineWithIssues,
                            issue = issue
                        )
                    )
                }

                override fun onDialogResponse(action: String, result: Bundle) {
                    val pendingAction = operator.processDialogResponseAndGetPendingAction(
                        action = action,
                        result = result,
                        issueMachine = productDialogIssueMachine
                    )

                    when (pendingAction) {
                        ProductPendingUserAction.SET_STOCK ->
                            setUiState(CheckoutPatientState.PendingUserActionStock)

                        ProductPendingUserAction.NONE -> getNextIssue()
                    }
                }

                override fun onIssuesEmpty() {
                    val vaccineWithCopay = runBlocking {
                        vaccineWithIssues.apply {
                            copay = medDInfo
                                .firstOrNull()
                                .getValidCopay(lotNumberWithProduct.product.antigen)
                        }
                    }

                    setUiState(
                        CheckoutPatientState.ProductVerified(
                            appointment = appointment,
                            vaccineWithIssues = vaccineWithCopay
                        )
                    )
                }

                override fun onCancelIssues() {
                    setUiState(CheckoutPatientState.ProductAddCanceled)
                }

                override fun onResetWithNewAppointment(appointment: Appointment?) {
                    (appointment ?: cachedAppointment)?.let {
                        evaluateProductForIssues(
                            appointment = it,
                            lotNumberWithProduct = vaccineWithIssues.lotNumberWithProduct,
                            stagedProducts = stagedProducts,
                            doseSeries = vaccineWithIssues.doseSeries,
                            manualDob = originalParameters.manualDob,
                            isLocallyCreated = originalParameters.isLocallyCreated
                        )
                    } ?: run {
                        Timber.e("ResetWithNewAppointment: Appointment was null!")
                        setUiState(CheckoutPatientState.ProductAddCanceled)
                    }
                }
            }
        )

        getNextIssue()
    }

    /**
     * Handle outstanding user actions needed to move forward with product issue resolution
     *
     * @param resolvedUserAction result from the user
     * @param appointment associated appointment if necessary
     */
    fun resolvePendingProductUserAction(resolvedUserAction: ProductPendingUserResult, appointment: Appointment?) {
        when (resolvedUserAction) {
            ProductPendingUserResult.SET_STOCK_COMPLETE ->
                productDialogIssueMachine.resetWithNewAppointment(
                    appointment = appointment
                )

            ProductPendingUserResult.SET_STOCK_CANCEL ->
                productDialogIssueMachine.notifyResultReceived(
                    action = ProductIssueOperator.ADD_ISSUE_TOKEN,
                    result = bundleOf(ProductIssueOperator.ADD_ISSUE_TOKEN to ProductIssue.WrongStock)
                )

            ProductPendingUserResult.MEDD_INFO_APPLIED ->
                productDialogIssueMachine.notifyResultReceived(
                    action = ProductIssueOperator.MEDD_INFO_UPDATED_TOKEN,
                    result = Bundle()
                )
        }
    }

    fun resolveDialogIssue(action: String, result: Bundle) {
        productDialogIssueMachine.notifyResultReceived(action, result)
    }

    fun getNextIssue() {
        productDialogIssueMachine.getNextDialogIssue()
    }

    fun cancelDialogIssues() {
        productDialogIssueMachine.cancelPendingIssues()
    }

    fun abandonAppointment(appointmentId: Int) =
        viewModelScope.launch {
            setUiState(LoadingState)
            val success = appointmentRepository.abandonAppointment(appointmentId)
            setUiState(CheckoutPatientState.AppointmentAbandoned(success))
        }

    private fun setUiState(state: State) {
        _uiFlow.tryEmit(state)
    }

    fun saveMetric(metric: BaseMetric) {
        analytics.saveMetric(metric)
    }

    fun saveCheckoutStockErrorOptionSelectedMetric(choice: String, appointmentId: Int) {
        saveMetric(
            StockChangeRetryPromptShownMetric(
                userChoice = choice,
                waitInMillis = aceWaitTimeMillisStart,
                visitId = appointmentId
            )
        )
    }

    private fun Appointment.getListOfPatientValidMedDIdTypes(): List<MedDIDType> {
        val listOfValidMedDIDType: MutableList<MedDIDType> = mutableListOf()

        if (this.patient.ssn?.isMaskedSsnFormatted() == true) {
            listOfValidMedDIDType.add(SSN)
        }

        if (this.patient.paymentInformation?.mbi?.isMbiFormatAndNotNull() == true) {
            listOfValidMedDIDType.add(MBI)
        }

        return listOfValidMedDIDType
    }

    private suspend fun runMedDCheck(appointmentId: Int, medDIdTypeAndValue: Pair<MedDIDType, String>) {
        val requestBody: MedDCheckRequestBody = when (medDIdTypeAndValue.first) {
            MBI -> MedDCheckRequestBody(
                ssn = null,
                mbi = medDIdTypeAndValue.second.trimDashes()
            )

            SSN -> MedDCheckRequestBody(
                ssn = medDIdTypeAndValue.second.trimDashes(),
                mbi = null
            )
        }

        appointmentRepository.doMedDCheck(appointmentId, requestBody)
    }

    fun onStockSelected(
        selectedStockId: Int,
        selectedFinancialClass: String?,
        appointmentId: Int
    ) {
        changeAppointmentStock(
            selectedStockId = selectedStockId,
            selectedFinancialClass = selectedFinancialClass,
            appointmentId = appointmentId
        )
    }

    /**
     * Copies the cachedAppointment, set it to "risk free" and return a state with the updated appt
     *
     * @param selectedStockId sourceId of the selected stock
     * @param selectedFinancialClass name of the selected vfcFinancialClass if applicable
     * @param appointmentId id of the visit
     */
    private fun getTimeoutStateWithRiskFreeAppointment(
        selectedStockId: Int,
        selectedFinancialClass: String?,
        appointmentId: Int
    ) = cachedAppointment?.let { cached ->
        val waitTime = System.currentTimeMillis() - aceWaitTimeMillisStart
        val chosenStock = InventorySource.valueOfSourceId(selectedStockId)
        val (riskFreePaymentMethod, riskFreeIcon, riskFreeSecondaryMessage) =
            when (chosenStock) {
                InventorySource.PRIVATE ->
                    (PaymentMode.InsurancePay.toPaymentMethod() to AppointmentIcon.STAR) to
                        PRIVATE_RISK_FREE_SECONDARY_MESSAGE

                else -> (PaymentMode.NoPay.toPaymentMethod() to AppointmentIcon.FULL_CIRCLE) to
                    PUBLIC_RISK_FREE_SECONDARY_MESSAGE_FMT.format(chosenStock.displayName)
            }

        val updatedEncounterState = cached.encounterState?.copy()?.apply {
            messages = cached.encounterState?.messages?.map { message ->
                if (message.serviceType == AppointmentServiceType.VACCINE) {
                    message.copy(
                        status = AppointmentStatus.RISK_FREE,
                        icon = riskFreeIcon,
                        primaryMessage = RISK_FREE_PRIMARY_MESSAGE,
                        secondaryMessage = riskFreeSecondaryMessage
                    )
                } else {
                    message
                }
            } ?: emptyList()
        }
        val updatedAppt = cached.copy(
            vaccineSupply = InventorySource.valueOfSourceId(selectedStockId)
                .getVaccineSupplyString(),
            paymentMethod = riskFreePaymentMethod,
            encounterState = updatedEncounterState,
            patient = cached.patient.copy(
                paymentInformation = cached.patient.paymentInformation?.copy(
                    vfcFinancialClass = selectedFinancialClass
                )
            )
        )

        saveMetric(
            StockChangeTimeOutMetric(
                originalStock = cached.vaccineSupply,
                originalPaymentMethod = cached.paymentMethod.name,
                presentedStock = chosenStock.displayName,
                presentedPaymentMethod = riskFreePaymentMethod.name,
                waitInMillis = waitTime,
                visitId = appointmentId
            )
        )

        viewModelScope.launch { appointmentRepository.upsertAppointments(listOf(updatedAppt)) }
        CheckoutPatientState.StockTimeout(
            selectedStockId = selectedStockId,
            selectedFinancialClass = selectedFinancialClass,
            riskFreePaymentMode = riskFreePaymentMethod.toPaymentMode(),
            riskFreeAppointment = updatedAppt
        )
    }

    /**
     * This is here because the only source's vaccineSupply that is different than its displayName
     * from the backend is 317 as "section317"
     */
    private fun InventorySource.getVaccineSupplyString() =
        when (this) {
            InventorySource.THREE_SEVENTEEN -> "section$displayName"
            else -> displayName
        }

    /**
     * Begins the patch for the appointment stock and wait for the ACE
     *
     * @param selectedStockId sourceId of the selected stock
     * @param selectedFinancialClass name of the selected vfcFinancialClass if applicable
     * @param appointmentId id of the visit
     */
    fun changeAppointmentStock(
        selectedStockId: Int,
        selectedFinancialClass: String?,
        appointmentId: Int
    ) {
        setUiState(CheckoutPatientState.AceSavingInfo)
        var patchSent = false
        val timeoutCallback: (Int) -> Unit = { timeoutIteration ->
            val state = when {
                timeoutIteration < ACE_TIMEOUT_ITERATION -> CheckoutPatientState.AceTimingOut
                else -> {
                    if (patchSent) {
                        val state = getTimeoutStateWithRiskFreeAppointment(
                            selectedStockId = selectedStockId,
                            selectedFinancialClass = selectedFinancialClass,
                            appointmentId = appointmentId
                        )

                        cachedAppointment = state?.riskFreeAppointment
                        appointmentIsMedDFlow.update { _ ->
                            cachedAppointment?.isMedDAndDateOfService() == true
                        }
                        state
                    } else {
                        null
                    }
                }
            } ?: Reset
            setUiState(state)
        }

        cancelPendingAceJob()
        aceWaitTimeMillisStart = System.currentTimeMillis()
        aceTimeoutJob = viewModelScope.launchWithTimeoutIterations(
            maxIterations = ACE_TIMEOUT_ITERATION,
            timeoutLengthMillis = ACE_TIMEOUT_MILLIS,
            dispatcher = Dispatchers.IO,
            timeoutCallback = timeoutCallback,
        ) {
            try {
                val appointment =
                    requireNotNull(appointmentRepository.getAppointmentByIdAsync(appointmentId)) {
                        "Appt $appointmentId not found!"
                    }
                val stocks = locationRepository.getInventorySourcesAsync()?.map { it.displayName }
                    ?: emptyList()
                analytics.saveMetric(
                    CheckoutStockOptionSelectedMetric(
                        selectedStock = InventorySource.valueOfSourceId(selectedStockId).displayName,
                        selectedFinancialClass = selectedFinancialClass,
                        appointmentStock = appointment.vaccineSupply,
                        patientInsurancePrimaryId = appointment.patient.paymentInformation?.primaryInsuranceId,
                        patientInsurancePlanId = appointment.patient.paymentInformation?.primaryInsurancePlanId,
                        activePublicStocks = stocks,
                        visitId = appointmentId
                    )
                )
                appointmentRepository.patchPatient(
                    patientId = appointment.patient.id,
                    fields = listOf(
                        PayerField.Stock(selectedStockId.toString()),
                        PayerField.VfcFinancialClass(selectedFinancialClass)
                    ),
                    appointmentId = appointment.id,
                    ignoreOfflineStorage = true
                )
                patchSent = true
            } catch (e: Exception) {
                Timber.e(e)
                aceWaitTimeMillisStart = System.currentTimeMillis() - aceWaitTimeMillisStart
                setUiState(
                    CheckoutPatientState.StockError(
                        selectedStockId = selectedStockId,
                        selectedFinancialClass = selectedFinancialClass
                    )
                )
                cancelPendingAceJob()
            }
        }
    }

    fun saveProductSelectionMetric(
        lotNumberWithProduct: LotNumberWithProduct,
        relativeDoS: RelativeDoS?,
        appointmentId: Int?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val copays =
                medDInfo.firstOrNull()?.copays?.associate { (it.antigen.value to it.copay) }

            val selfPayRate =
                productRepository.findOneTouch(lotNumberWithProduct.productId)?.selfPayRate

            saveProductSelectionMetric(
                ProductSelectionMetric(
                    screenSource = "Lot Selection",
                    scannerType = "Manual",
                    productSource = "Manual Lot Selection",
                    hubModel = "Mobile Hub",
                    salesProductId = lotNumberWithProduct.salesProductId,
                    productName = lotNumberWithProduct.product.displayName,
                    ndc = lotNumberWithProduct.product.productNdc ?: "",
                    lotNumber = lotNumberWithProduct.name,
                    expirationDate = lotNumberWithProduct.expirationDate?.toLocalDateString() ?: "",
                    relativeDoS = relativeDoS,
                    patientVisitId = appointmentId,
                    selfPayRate = selfPayRate,
                    copays = copays
                )
            )
        }
    }

    fun saveProductSelectionMetric(productSelectionMetric: ProductSelectionMetric) {
        val metric = runBlocking {
            val associatedCopays =
                medDInfo.firstOrNull()?.copays?.associate { (it.antigen.value to it.copay) }

            val associatedSelfPayRate =
                productRepository.findOneTouch(productSelectionMetric.salesProductId)?.selfPayRate

            productSelectionMetric.apply {
                copays = associatedCopays
                selfPayRate = associatedSelfPayRate
            }
        }
        analytics.saveMetric(metric)
    }

    override fun onCleared() {
        resetMedDInfo()
        super.onCleared()
    }

    private fun cancelPendingAceJob() {
        aceTimeoutJob?.cancel()
        aceTimeoutJob = null
    }

    private fun PaymentMode.toPaymentMethod(): PaymentMethod =
        PaymentMethod.values().associateBy(PaymentMethod::modeId)[id] ?: PaymentMethod.NoPay

    /**
     * Object to hold parameters passed in for evaluating
     *
     * @property manualDob
     * @property isLocallyCreated
     * @property locationSources
     */
    private data class OriginalEvaluateProductParameters(
        val manualDob: LocalDate? = null,
        val isLocallyCreated: Boolean = false,
        val locationSources: List<InventorySource>
    )
}
