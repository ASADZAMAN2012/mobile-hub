/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.summary

import android.graphics.drawable.AnimatedImageDrawable
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.core.extensions.intToEnum
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.ui.extension.hide
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.safeContext
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentPatientCheckoutSummaryBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.extension.amount
import com.vaxcare.vaxhub.model.inventory.DoseReasonContext
import com.vaxcare.vaxhub.model.inventory.validator.HubFeatures
import com.vaxcare.vaxhub.model.inventory.validator.ProductVerifier
import com.vaxcare.vaxhub.model.metric.CheckoutCollectPaymentClick
import com.vaxcare.vaxhub.model.metric.CheckoutViewPastMetric
import com.vaxcare.vaxhub.model.metric.CheckoutViewPastMetric.CheckContext.COMPLETE_SCREEN
import com.vaxcare.vaxhub.model.metric.CheckoutViewPastMetric.CheckContext.PATIENT_SCHEDULE
import com.vaxcare.vaxhub.model.metric.CheckoutViewPastMetric.CheckoutResult
import com.vaxcare.vaxhub.model.metric.CheckoutViewPastMetric.RelativeTime
import com.vaxcare.vaxhub.model.metric.PaymentInformationPromptPresentedMetric
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.service.UserSessionService
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineSummaryItemAdapter
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineSummaryItemAdapterOptions
import com.vaxcare.vaxhub.ui.checkout.extensions.checkoutStatus
import com.vaxcare.vaxhub.ui.checkout.viewholder.SummaryItemListener
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutSummaryViewModel
import com.vaxcare.vaxhub.viewmodel.PhoneWorkflowData
import com.vaxcare.vaxhub.viewmodel.ProductViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutSummaryFragment :
    BaseSummaryFragment<FragmentPatientCheckoutSummaryBinding>(),
    SummaryItemListener {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var localStorage: LocalStorage

    @Inject
    override lateinit var sessionService: UserSessionService

    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CheckoutSummaryDestination

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private val args: CheckoutSummaryFragmentArgs by navArgs()
    private val productViewModel: ProductViewModel by activityViewModels()
    override val appointmentViewModel: AppointmentViewModel by activityViewModels()
    override val viewModel: CheckoutSummaryViewModel by viewModels()

    private lateinit var vaccineItemAdapter: VaccineSummaryItemAdapter
    private var isDisableDuplicateRSV = false
    private var isPhoneCollectingEnabled = true
    private var isCreditCardCaptureDisabled = false
    private val isEditCheckoutFromComplete by lazy {
        appointmentViewModel.currentCheckout.isEditCheckoutFromComplete
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_patient_checkout_summary,
        hasToolbar = false
    )

    private enum class LoginActionCorrelation {
        DEFAULT,
        GO_BACK,
        OPEN_ADMINISTERED,
        OPEN_PHYSICIAN
    }

    override fun canShowConnection(): Boolean = false

    override fun bindFragment(container: View): FragmentPatientCheckoutSummaryBinding =
        FragmentPatientCheckoutSummaryBinding.bind(container)

    override val appointmentId by lazy { args.appointmentId }

    private var checkoutStatus: EditCheckoutStatus = EditCheckoutStatus.ACTIVE_CHECKOUT

    override fun handleBack(): Boolean {
        return if (isSessionAuthenticated()) {
            appointmentViewModel.clearAllPhoneDeltas(COLLECT_PHONE_DATA_FRAGMENT_TAG)
            super.handleBack()
        } else {
            checkSessionNavigation(
                titleResId = R.string.loginPinFragment_reEnterPin,
                correlation = LoginActionCorrelation.GO_BACK.ordinal
            )
            true
        }
    }

    override fun setupUi(
        appointment: Appointment,
        user: User?,
        disableDuplicateRSV: Boolean,
        phoneCollectingEnabled: Boolean,
        disableCCCapture: Boolean
    ) {
        isDisableDuplicateRSV = disableDuplicateRSV
        isPhoneCollectingEnabled = phoneCollectingEnabled
        isCreditCardCaptureDisabled = disableCCCapture
        if (user != null) {
            checkoutStatus = appointment.checkoutStatus()
            binding?.topBar?.setTitle(checkoutStatus.summaryDisplay)
            val isViewOnly = checkoutStatus == EditCheckoutStatus.VIEW_CHECKOUT
            // here will save Checkout.ViewPast MixPanel event
            if (isViewOnly) {
                binding?.layoutPatientConsent?.hide()
                binding?.collectPaymentInfo?.apply {
                    text = getString(R.string.done)
                    setOnSingleClickListener { globalDestinations.goBack(this@CheckoutSummaryFragment) }
                    show()
                }

                analyticsCheckoutViewPastMetric(appointment)
            }

            vaccineItemAdapter = VaccineSummaryItemAdapter(
                items = appointmentViewModel.currentCheckout.stagedProducts,
                options = VaccineSummaryItemAdapterOptions(
                    appointment = appointment,
                    isEditable = checkoutStatus.isEditable(),
                    manualDob = appointmentViewModel.currentCheckout.manualDob,
                    updatedFirstName = appointmentViewModel.deltaFields
                        .firstOrNull { it is DemographicField.FirstName }?.currentValue,
                    updatedLastName = appointmentViewModel.deltaFields
                        .firstOrNull { it is DemographicField.LastName }?.currentValue,
                    isCheckedOut = checkoutStatus.isCheckedOut()
                ),
                listener = this
            )

            binding?.rvVaccines?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = vaccineItemAdapter
            }

            // Need appointment data to determine screenTitle
            when {
                isViewOnly -> logScreenNavigation("PastCheckoutSummary")
                vaccineItemAdapter.shouldDisplayCollectPaymentInfo(true) ->
                    logScreenNavigation("PaymentSummary")

                else -> logScreenNavigation("CheckoutSummary")
            }

            if (checkoutStatus == EditCheckoutStatus.VIEW_CHECKOUT &&
                appointmentViewModel.currentCheckout.stagedProducts.isEmpty() &&
                appointment.administeredVaccines.isNotEmpty()
            ) {
                // hardcode false for rprd here because at this point we dont care
                val verifier =
                    ProductVerifier(HubFeatures(false, isDisableDuplicateRSV))
                addAdministeredVaccinesToView(appointment, verifier)
            }

            showPatientConsent(appointment)

            if (shouldShowCollectPaymentInfo(appointment)) {
                binding?.layoutPatientConsent?.hide()
                binding?.collectPaymentInfo?.show()
                when {
                    vaccineItemAdapter.existCopay() -> {
                        binding?.collectPaymentInfo?.setOnClickListener {
                            analytics.saveMetric(
                                CheckoutCollectPaymentClick(
                                    totalToCollect = vaccineItemAdapter.items.amount(),
                                    patientVisitId = appointment.id
                                )
                            )

                            determinePaymentInfoOnFile {
                                destination.toMedDCopayDialog(
                                    this@CheckoutSummaryFragment,
                                    appointmentId = appointmentId,
                                    currentPhone = appointment.patient.phoneNumber,
                                    enablePhoneCollection = isPhoneCollectingEnabled
                                )
                            }
                        }
                    }

                    appointment.paymentMethod == PaymentMethod.SelfPay || vaccineItemAdapter.existSelfPay() -> {
                        binding?.collectPaymentInfo?.setOnClickListener {
                            analytics.saveMetric(
                                CheckoutCollectPaymentClick(
                                    totalToCollect = vaccineItemAdapter.items.amount(),
                                    patientVisitId = appointment.id
                                )
                            )

                            analytics.saveMetric(
                                PaymentInformationPromptPresentedMetric(
                                    appointmentId,
                                    PaymentInformationPromptPresentedMetric.PaymentInfoTrigger.SELF_PAY.displayName,
                                    ""
                                )
                            )

                            determinePaymentInfoOnFile {
                                destination.toSelfPayDialog(
                                    this@CheckoutSummaryFragment,
                                    appointmentId = appointmentId,
                                    currentPhone = appointment.patient.phoneNumber,
                                    enablePhoneCollection = isPhoneCollectingEnabled
                                )
                            }
                        }
                    }
                }
            } else {
                binding?.layoutPatientConsent?.isGone = isViewOnly
                binding?.collectPaymentInfo?.isVisible = isViewOnly
            }

            if (appointment.administeredVaccines.isNotEmpty()) {
                setupShotAdmin(appointment.administeredBy)
            } else {
                setupShotAdmin()
            }

            val forceMedDSignature = isForceMedDSignature(appointment)
            val hasMedDDoses = appointmentViewModel.currentCheckout.containsMedDDoses()

            if (forceMedDSignature || hasMedDDoses) {
                binding?.checkoutBtn?.setImageResource(R.drawable.ic_arrow_forward)
            }

            binding?.checkoutBtn?.setOnSingleClickListener {
                if (forceMedDSignature || hasMedDDoses) {
                    destination.toMedDSignatureCollect(this@CheckoutSummaryFragment)
                } else {
                    // completes checkout and sends to server
                    completeCheckout(appointment)
                }
            }
        } else {
            Timber.e("There was a problem: User is null.")
        }
    }

    private fun shouldShowCollectPaymentInfo(appointment: Appointment) =
        appointment.let {
            val hasAddedDoses = vaccineItemAdapter.hasAddedDoses()
            val isSelfPayAndSubtotalNotZero =
                it.paymentMethod == PaymentMethod.SelfPay && vaccineItemAdapter.checkSubTotalNotZero()
            val shouldDisplayBasedOnSubtotal =
                isSelfPayAndSubtotalNotZero || vaccineItemAdapter.shouldDisplayCollectPaymentInfo()
            val isEligibleForPayment =
                !isCreditCardCaptureDisabled && hasAddedDoses && isEditCheckoutDoseChanged()
            isEligibleForPayment && shouldDisplayBasedOnSubtotal
        }

    override fun onEditAdministeredByClick() {
        if (isSessionAuthenticated()) {
            openAdministeredBySelection()
        } else {
            checkSessionNavigation(
                titleResId = R.string.loginPinFragment_reEnterPin,
                correlation = LoginActionCorrelation.OPEN_ADMINISTERED.ordinal
            )
        }
    }

    override fun onEditPhysicianClick() {
        if (isSessionAuthenticated()) {
            openProviderSelection()
        } else {
            checkSessionNavigation(
                titleResId = R.string.loginPinFragment_reEnterPin,
                correlation = LoginActionCorrelation.OPEN_PHYSICIAN.ordinal
            )
        }
    }

    override fun showLoading() {
        binding?.apply {
            loadingContainer.show()
            (loading.drawable as? AnimatedImageDrawable)?.start()
        }
    }

    override fun hideLoading() {
        binding?.apply {
            loadingContainer.hide()
        }
    }

    /**
     * When forcing risk-free for locally created appointments, we must show the medD signature form
     * if a medD dose is scanned and the patient is 64 or older
     *
     * @param appointment
     * @return
     */
    private fun isForceMedDSignature(appointment: Appointment): Boolean =
        if (appointmentViewModel.currentCheckout.manualDob != null &&
            appointment.patient.dob != null
        ) {
            val isForceRiskFree =
                appointmentViewModel.currentCheckout.presentedRiskAssessmentId ==
                    AppointmentViewModel.FORCED_RISK_FREE_ASSESSMENT_ID

            val dob = appointmentViewModel.currentCheckout.manualDob ?: LocalDate.parse(
                appointment.patient.getDobString(),
                DateTimeFormatter.ofPattern("M/dd/yyyy")
            )

            val age = dob?.let {
                ChronoUnit.YEARS.between(dob, LocalDate.now()).toInt()
            } ?: 0

            val hasMedDDose = appointmentViewModel.currentCheckout.stagedProducts.any {
                MedDVaccines.isMedDVaccine(it.product.antigen)
            }

            isForceRiskFree && age >= 64 && hasMedDDose
        } else {
            false
        }

    /**
     *  If there is a valid card already on file, skip the “Credit Card” or “Cash/Check” popup,
     *
     * @param paymentDialogCallback goto the “Credit Card” or “Cash/Check” popup
     */
    private fun determinePaymentInfoOnFile(paymentDialogCallback: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.Main) {
            if (checkoutStatus != EditCheckoutStatus.VIEW_CHECKOUT) {
                startLoading()
                val paymentInfo = safeContext(Dispatchers.IO) {
                    appointmentViewModel.getPaymentInformation(appointmentId)
                }
                val paymentInfoRequestBody = paymentInfo?.let {
                    PaymentInformationRequestBody(
                        expirationDate = it.expirationDate ?: "",
                        cardNumber = it.cardNumber ?: "",
                        cardholderName = it.cardholderName ?: "",
                        email = it.email ?: "",
                        phoneNumber = it.phoneNumber ?: ""
                    ).apply {
                        isOnFile = true
                    }
                }
                endLoading()
                if (paymentInfoRequestBody != null && paymentInfoRequestBody.cardNumber.isNotEmpty()) {
                    destination.toMedDSummary(
                        fragment = this@CheckoutSummaryFragment,
                        appointmentId = appointmentId,
                        paymentInfo = paymentInfoRequestBody
                    )
                } else {
                    paymentDialogCallback.invoke()
                }
            } else {
                paymentDialogCallback.invoke()
            }
        }
    }

    override fun onAdminSelected(shotAdministrator: ShotAdministrator) {
        super.onAdminSelected(shotAdministrator)
        setupShotAdmin(shotAdministrator.id)
    }

    override fun handleProviderSelected(provider: Provider) {
        hideLoading()
        vaccineItemAdapter.updatePhysician(provider)
    }

    private fun setupShotAdmin(administratorById: Int? = null) =
        viewModel.getShotAdminsLiveData()
            .observe(viewLifecycleOwner) { shotAdministrators ->
                val selectedId = administratorById ?: localStorage.userId
                val administrator = shotAdministrators.find { it.id == selectedId }
                administrator?.let {
                    vaccineItemAdapter.updateShotAdministratorName(
                        context?.formatString(
                            R.string.provider_name_display,
                            it.firstName.captureName(),
                            it.lastName.captureName()
                        )
                    )
                }
            }

    private fun isExistDose() =
        appointmentViewModel.currentCheckout.stagedProducts.any {
            it.isRemoveDoseState().not()
        }

    private fun showPatientConsent(appointment: Appointment) {
        when (checkoutStatus) {
            EditCheckoutStatus.ACTIVE_CHECKOUT -> {
                binding?.layoutPatientBottom?.show()
                binding?.patientConsent?.text = getString(R.string.patient_consent)
                binding?.patientCheckoutAdvisory?.text = context?.formatString(
                    R.string.patient_checkout_advisory_display,
                    appointment.appointmentTime.toLocalDate()
                )
            }

            EditCheckoutStatus.PAST_CHECKOUT -> {
                binding?.layoutPatientBottom?.show()
                if (isExistDose()) {
                    binding?.patientConsent?.text = getString(R.string.patient_consent)
                    binding?.patientCheckoutAdvisory?.text = context?.formatString(
                        R.string.patient_checkout_advisory_display,
                        appointment.appointmentTime.toLocalDate()
                    )
                } else {
                    binding?.patientConsent?.text =
                        getString(R.string.patient_consent_checkout_empty)
                    binding?.patientCheckoutAdvisory?.text =
                        getString(R.string.patient_consent_checkout_empty_display)
                }
            }

            EditCheckoutStatus.VIEW_CHECKOUT -> Unit
        }
    }

    /*
     * Here is Ordered/ No Doses Scanned,but now not sure which is Ordered / Other Doses Scanned
     */
    private fun addAdministeredVaccinesToView(appointment: Appointment, verifier: ProductVerifier) {
        viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.Main) {
            val administeredVaccines = appointment.administeredVaccines
            var copays: List<ProductCopayInfo>? = null
            val vaccinesToAdd = safeContext(Dispatchers.IO) {
                administeredVaccines.mapNotNull { administeredVaccine ->
                    val lotNumberProduct =
                        productViewModel.findLotNumberByName(administeredVaccine.lotNumber)
                    lotNumberProduct?.let { prod ->
                        if (copays == null) {
                            copays = viewModel.retrieveMedDCopays(appointmentId)?.copays
                                ?: emptyList()
                        }
                        prod.oneTouch =
                            productViewModel.findOneTouch(lotNumberProduct.productId)
                        prod.copay = copays?.filter { it.isCovered() }
                            ?.find { it.antigen.value == prod.product.antigen }

                        val mapping = productViewModel.findMapping(lotNumberProduct.productId)
                        prod.dosesInSeries = mapping?.dosesInSeries ?: 1

                        val vaccineWithIssues = verifier.getProductIssues(
                            productToCheck = lotNumberProduct,
                            appointment = appointment,
                            stagedProducts = vaccineItemAdapter.items,
                            doseSeries = administeredVaccine.doseSeries,
                            manualDob = appointmentViewModel.currentCheckout.manualDob,
                            simpleOnHandProductInventory = productViewModel.getSimpleOnHandInventoryByProductLotName(
                                lotNumberProduct.name
                            ),
                            isVaxCare3 = appointmentViewModel.currentCheckout.isVaxCare3,
                            medDInfo = null
                        )
                        vaccineWithIssues?.overrideSiteAndRoute(
                            route = administeredVaccine.method
                        )
                        vaccineWithIssues?.toAdapterDto()
                            ?.apply {
                                doseState =
                                    if (administeredVaccine.isDeleted) {
                                        DoseState.ADMINISTERED_REMOVED
                                    } else {
                                        DoseState.ADMINISTERED
                                    }
                            }
                    }
                }
            }.sortedWith(
                compareBy(
                    { it.isDeleted },
                    { it.product.displayName.lowercase() }
                )
            )
            vaccineItemAdapter.setAllItems(vaccinesToAdd)
        }
    }

    // MixPanel for Checkout.ViewPast
    private fun analyticsCheckoutViewPastMetric(appointment: Appointment) {
        viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.IO) {
            val checkContext = if (isEditCheckoutFromComplete) {
                COMPLETE_SCREEN
            } else {
                PATIENT_SCHEDULE
            }
            val checkoutRelativeTime =
                RelativeTime.checkoutRelativeTime(appointment.appointmentTime.toLocalDate())
            val isAnyLotRemoved =
                appointmentViewModel.currentCheckout.stagedProducts.any { it.isAnyLotRemoved() }
            val isAnyLotAdded =
                appointmentViewModel.currentCheckout.stagedProducts.any { it.isAnyLotAdded() }
            Timber.d("isAnyLotRemoved=$isAnyLotRemoved / isAnyLotAdded=$isAnyLotAdded")
            val checkoutResult = when {
                appointment.isEditable == false -> CheckoutResult.VIEW_ONLY
                !isAnyLotRemoved && !isAnyLotAdded -> CheckoutResult.NO_EDITS
                isAnyLotRemoved && isAnyLotAdded -> CheckoutResult.BOTH_EDITS
                isAnyLotRemoved -> CheckoutResult.LOT_REMOVED
                else -> CheckoutResult.LOT_ADDED
            }
            val checkoutEvent = CheckoutViewPastMetric(
                appointment.id,
                checkContext.displayName,
                checkoutRelativeTime.displayName,
                checkoutResult.displayName
            )

            Timber.d("CheckoutViewPastMetric = $checkoutEvent")
            analytics.saveMetric(checkoutEvent)
        }
    }

    private fun isEditCheckoutDoseChanged(): Boolean {
        return checkoutStatus == EditCheckoutStatus.ACTIVE_CHECKOUT ||
            (
                checkoutStatus == EditCheckoutStatus.PAST_CHECKOUT &&
                    appointmentViewModel.currentCheckout.stagedProducts.any {
                        it.isAnyLotAdded() || it.isAnyLotRemoved()
                    }
            )
    }

    private fun completeCheckout(appointment: Appointment) {
        binding?.checkoutBtn?.isEnabled = false

        val addedDoseStates = listOf(DoseState.ADMINISTERED, DoseState.ADDED)
        val unOrdered =
            appointmentViewModel.currentCheckout.orderWrapper?.products
                ?.filter { it.reasonContext == DoseReasonContext.DOSES_NOT_ORDERED }
        val lotNumberWithProducts =
            appointmentViewModel.currentCheckout.stagedProducts
                .filter { addedDoseStates.contains(it.doseState) }
                .map { item ->
                    item.apply {
                        unorderReasonId = unOrdered
                            ?.firstOrNull { it.salesProductId == item.salesProductId }
                            ?.selectedReason?.value
                    }
                }
                .toMutableList()

        val phoneWorkflow = PhoneWorkflowData(
            phoneNumberFlowPresented = appointmentViewModel.currentCheckout.phoneNumberFlowPresented,
            phoneContactStatus = appointmentViewModel.currentCheckout.phoneContactStatus,
            phoneContactReasons = appointmentViewModel.currentCheckout.phoneContactReasons
        )

        viewModel.completeCheckout(
            selectedAppointment = appointment,
            stagedProducts = lotNumberWithProducts,
            shotAdminId = appointmentViewModel.currentCheckout.administrator?.id,
            deltas = appointmentViewModel.deltaFields,
            ordersUnadministered = appointmentViewModel.currentCheckout.orderWrapper?.products
                ?.filter { it.reasonContext == DoseReasonContext.ORDER_UNFILLED }
                ?: emptyList(),
            networkStatus = networkMonitor.networkStatus.value ?: NetworkStatus.DISCONNECTED,
            presentedRiskAssessmentId = appointmentViewModel.currentCheckout.presentedRiskAssessmentId,
            phoneWorkflowData = phoneWorkflow,
            paymentInformation = null,
            pregnancyPrompt = appointmentViewModel.currentCheckout.pregnancyPrompt,
            weeksPregnant = appointmentViewModel.currentCheckout.weeksPregnant,
            riskFactors = appointmentViewModel.currentCheckout.riskFactors
        )
    }

    override fun checkoutCompleted(appointment: Appointment, multiplePaymentMode: Boolean) {
        // Checkout.ViewPast
        analyticsCheckoutViewPastMetric(appointment)
        val addedDoseStates = listOf(DoseState.ADMINISTERED, DoseState.ADDED)
        val count =
            appointmentViewModel.currentCheckout.stagedProducts.filter { addedDoseStates.contains(it.doseState) }.size
        destination.toCheckoutComplete(
            fragment = this@CheckoutSummaryFragment,
            shotCount = count,
            multiplePaymentMode = multiplePaymentMode
        )

        appointmentViewModel.currentCheckout.selectedAppointment = appointment
    }

    override fun onLoginSuccess(data: Int) {
        super.onLoginSuccess(data)
        val loginCorrelation = intToEnum(data, LoginActionCorrelation.DEFAULT)
        reportLogin(true, loginCorrelation)
        when (loginCorrelation) {
            LoginActionCorrelation.GO_BACK -> globalDestinations.goBack(this)
            LoginActionCorrelation.OPEN_ADMINISTERED -> openAdministeredBySelection()
            LoginActionCorrelation.OPEN_PHYSICIAN -> openProviderSelection()
            else -> Timber.e("Error: LoginCorrelation for CheckoutSummary unknown")
        }
    }

    override fun onLoginAbort(data: Int) {
        super.onLoginAbort(data)
        reportLogin(false, intToEnum(data, LoginActionCorrelation.DEFAULT))
    }

    override fun onLoginFailure(data: Int) {
        reportLogin(false, intToEnum(data, LoginActionCorrelation.DEFAULT))
        appointmentViewModel.clearCurrentCheckout()
        super.onLoginFailure(data)
    }

    private fun reportLogin(success: Boolean, loginCorrelation: LoginActionCorrelation) {
        val attemptedNavigation = when (loginCorrelation) {
            LoginActionCorrelation.GO_BACK -> "EditCurrentCheckout"
            LoginActionCorrelation.OPEN_ADMINISTERED -> "EditAdministered"
            LoginActionCorrelation.OPEN_PHYSICIAN -> "EditPhysician"
            else -> "Unknown"
        }

        viewModel.reportLogin(success, attemptedNavigation, appointmentId)
    }

    override fun onDestroyView() {
        binding?.rvVaccines?.adapter = null
        super.onDestroyView()
    }
}
