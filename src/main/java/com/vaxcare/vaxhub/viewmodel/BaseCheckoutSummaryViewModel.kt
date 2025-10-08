/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.extension.isCreditCardCaptureDisabled
import com.vaxcare.vaxhub.core.extension.isDuplicateRSVDisabled
import com.vaxcare.vaxhub.core.extension.isPayByPhoneDisabled
import com.vaxcare.vaxhub.core.extension.safeLet
import com.vaxcare.vaxhub.core.extension.toMillis
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.domain.UploadAppointmentMediaUseCase
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.AppointmentCheckout
import com.vaxcare.vaxhub.model.CheckInVaccination
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.appointment.PhoneContactConsentStatus
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus
import com.vaxcare.vaxhub.model.enums.ForcedRiskType
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.RiskFactor
import com.vaxcare.vaxhub.model.extension.isMultiplePaymentMode
import com.vaxcare.vaxhub.model.inventory.OrderDose
import com.vaxcare.vaxhub.model.legacy.NoCheckoutReason
import com.vaxcare.vaxhub.model.metric.CheckoutFinishMetric
import com.vaxcare.vaxhub.model.metric.CheckoutLoginMetric
import com.vaxcare.vaxhub.model.patient.AppointmentFlagsField
import com.vaxcare.vaxhub.model.patient.AppointmentMediaField
import com.vaxcare.vaxhub.model.patient.InfoField
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.ui.checkout.extensions.checkoutStatus
import com.vaxcare.vaxhub.web.PatientsApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

abstract class BaseCheckoutSummaryViewModel(
    protected val appointmentRepository: AppointmentRepository,
    protected val locationRepository: LocationRepository,
    protected val userRepository: UserRepository,
    protected val localStorage: LocalStorage,
    protected val patientsApi: PatientsApi,
    @MHAnalyticReport protected val analytics: AnalyticReport,
    private val uploadAppointmentMediaUseCase: UploadAppointmentMediaUseCase,
    protected val dispatcherProvider: DispatcherProvider
) : BaseViewModel() {
    sealed class CheckoutSummaryState : State {
        data class AppointmentLoaded(
            val appointment: Appointment,
            val isDisableDuplicateRSV: Boolean,
            val isPhoneCollectingEnabled: Boolean,
            val isDisableCCCapture: Boolean,
            val user: User?
        ) : CheckoutSummaryState()

        data class CheckoutSuccess(
            val appointment: Appointment,
            val multiplePaymentMode: Boolean
        ) : CheckoutSummaryState()

        data class AppointmentUpdateSuccess(
            val provider: Provider
        ) : CheckoutSummaryState()

        data class AppointmentUpdateFailure(
            val provider: Provider
        ) : CheckoutSummaryState()

        object AppointmentLoadFailure : CheckoutSummaryState()

        object CheckoutFailure : CheckoutSummaryState()

        object AppointmentEditPending : CheckoutSummaryState()
    }

    init {
        loadingStates.add(CheckoutSummaryState.AppointmentEditPending)
    }

    open fun onBeforeCheckout() {}

    fun loadAppointment(appointmentId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val flags = locationRepository.getFeatureFlagsAsync()
            var appt = appointmentRepository.getAppointmentByIdAsync(appointmentId)

            // Fetch appointment from API if we are viewing a past checkout
            if (appt?.checkoutStatus() == EditCheckoutStatus.VIEW_CHECKOUT) {
                appt = appointmentRepository.getAndInsertUpdatedAppointment(appointmentId)
                    ?.toAppointment()
            }
            appt?.let {
                val user = userRepository.getUserAsync(localStorage.userId)

                setState(
                    CheckoutSummaryState.AppointmentLoaded(
                        appointment = it,
                        isDisableDuplicateRSV = flags.isDuplicateRSVDisabled(),
                        isPhoneCollectingEnabled = !flags.isPayByPhoneDisabled(),
                        isDisableCCCapture = flags.isCreditCardCaptureDisabled(),
                        user = user,
                    )
                )
            } ?: kotlin.run { setState(CheckoutSummaryState.AppointmentLoadFailure) }
        }

    fun updateProvider(provider: Provider, appointment: Appointment?) {
        setState(CheckoutSummaryState.AppointmentEditPending)
        appointment?.let {
            viewModelScope.launch(dispatcherProvider.io) {
                val state = try {
                    appointmentRepository.updateAppointment(
                        appointmentId = appointment.id,
                        providerId = provider.id,
                        date = appointment.appointmentTime,
                        visitType = appointment.visitType
                    )
                    CheckoutSummaryState.AppointmentUpdateSuccess(provider)
                } catch (e: Exception) {
                    CheckoutSummaryState.AppointmentUpdateFailure(provider)
                }

                setState(state)
            }
        } ?: run {
            setState(CheckoutSummaryState.AppointmentUpdateFailure(provider))
        }
    }

    // TODO: Refactor this, most of this data should be retrieved form the ViewModel not from the Fragment
    fun completeCheckout(
        selectedAppointment: Appointment?,
        stagedProducts: MutableList<VaccineAdapterProductDto>,
        shotAdminId: Int?,
        updateData: PatientCollectData? = null,
        deltas: List<InfoField>,
        ordersUnadministered: List<OrderDose> = listOf(),
        networkStatus: NetworkStatus,
        presentedRiskAssessmentId: Int?,
        phoneWorkflowData: PhoneWorkflowData,
        paymentInformation: PaymentInformationRequestBody?,
        pregnancyPrompt: Boolean,
        weeksPregnant: Int?,
        riskFactors: List<RiskFactor>
    ) = viewModelScope.launch(Dispatchers.IO) {
        onBeforeCheckout()
        selectedAppointment?.let { appointment ->
            deltas.filterIsInstance<AppointmentMediaField>().let { mediaDeltas ->
                if (mediaDeltas.isNotEmpty()) {
                    val driversLicenseFront = mediaDeltas
                        .firstOrNull { it is AppointmentMediaField.DriversLicenseFront }
                    val insuranceFront = mediaDeltas
                        .firstOrNull { it is AppointmentMediaField.InsuranceCardFront }
                    val insuranceBack = mediaDeltas
                        .firstOrNull { it is AppointmentMediaField.InsuranceCardBack }
                    uploadPatientCards(
                        appointmentId = appointment.id,
                        driverLicenseFrontPath = driversLicenseFront?.currentValue,
                        insuranceCardFrontPath = insuranceFront?.currentValue,
                        insuranceCardBackPath = insuranceBack?.currentValue
                    )
                }
            }

            appointmentRepository.patchPatient(
                patientId = appointment.patient.id,
                fields = deltas.filter { it !is AppointmentMediaField },
                appointmentId = appointment.id
            )

            if (ordersUnadministered.isNotEmpty()) {
                val modified = LocalDateTime.now()
                val payload = ordersUnadministered
                    .mapNotNull {
                        safeLet(
                            it.orderNumber,
                            it.selectedReason
                        ) { orderNumber, reason -> orderNumber to reason }
                    }
                    .map {
                        NoCheckoutReason(
                            patientVisitId = appointment.id,
                            ormOrderNumber = it.first,
                            noCheckoutReason = it.second.value,
                            lastModified = modified,
                            versionNumber = BuildConfig.VERSION_CODE
                        )
                    }

                appointmentRepository.postNoCheckoutReasons(payload)
            }

            val checkInVaccinations =
                stagedProducts.mapIndexed { index, checkInVaccination ->
                    CheckInVaccination(
                        id = index,
                        productId = checkInVaccination.salesProductId,
                        ageIndicated = checkInVaccination.ageIndicated,
                        lotNumber = checkInVaccination.lotNumber,
                        method = checkInVaccination.product.routeCode.name,
                        site = checkInVaccination.site?.abbreviation,
                        doseSeries = checkInVaccination.doseSeries,
                        paymentMode = checkInVaccination.paymentMode,
                        paymentModeReason = checkInVaccination.paymentModeReason,
                        reasonId = checkInVaccination.unorderReasonId
                    )
                }

            val appointmentFlags = deltas
                .filterIsInstance<AppointmentFlagsField.PhoneOptIn>()
                .flatMap { it.flags }

            val checkoutSuccess = doCheckout(
                deviceGuid = localStorage.tabletId,
                checkInVaccinations = checkInVaccinations,
                shotAdministratorId = shotAdminId ?: localStorage.userId,
                appointmentId = appointment.id,
                presentedRiskAssessmentId = presentedRiskAssessmentId,
                paymentMode = appointment.paymentMethod.toPaymentMode(),
                phoneWorkflowData = phoneWorkflowData,
                appointmentFlags = appointmentFlags,
                pregnancyPrompt = pregnancyPrompt,
                weeksPregnant = weeksPregnant,
                paymentInformation = paymentInformation,
                riskFactors = riskFactors
            )

            saveCheckoutFinishMetric(
                appointment,
                checkInVaccinations,
                checkoutSuccess,
                updateData,
                networkStatus,
                paymentInformation,
                presentedRiskAssessmentId,
                riskFactors
            )

            val state = if (paymentInformation == null || checkoutSuccess) {
                CheckoutSummaryState.CheckoutSuccess(
                    appointment,
                    stagedProducts.isMultiplePaymentMode()
                )
            } else {
                CheckoutSummaryState.CheckoutFailure
            }

            setState(state)
        }
    }

    private fun saveCheckoutFinishMetric(
        appointment: Appointment,
        checkInVaccinations: List<CheckInVaccination>,
        checkoutSuccess: Boolean,
        updateData: PatientCollectData?,
        networkStatus: NetworkStatus,
        paymentInformation: PaymentInformationRequestBody?,
        presentedRiskAssessmentId: Int?,
        riskFactors: List<RiskFactor>
    ) {
        analytics.saveMetric(
            CheckoutFinishMetric(
                visitId = appointment.id,
                doseCount = checkInVaccinations.size,
                isCheckedOut = appointment.checkedOut,
                riskFactors = riskFactors,
                paymentMethod = appointment.paymentMethod,
                duration = LocalDateTime.now()
                    .toMillis() - appointment.appointmentTime.toMillis(),
                result = if (checkoutSuccess) {
                    CheckoutFinishMetric.CheckoutResult.SUBMITTED
                } else {
                    CheckoutFinishMetric.CheckoutResult.ERROR
                },
                missingInfoCaptured = updateData != null,
                networkStatus = networkStatus,
                relativeDoS = appointment.getRelativeDoS(),
                paymentType = when {
                    paymentInformation?.cardNumber?.isNotBlank() == true -> "Credit Debit"
                    appointment.paymentMethod == PaymentMethod.PartnerBill ||
                        checkInVaccinations.any {
                            it.paymentMode == PaymentMode.PartnerBill
                        } -> "Cash Check / No Charge"

                    else -> "N/A"
                },
                showedRiskFree = presentedRiskAssessmentId == AppointmentViewModel.FORCED_RISK_FREE_ASSESSMENT_ID
            )
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun uploadPatientCards(
        appointmentId: Int,
        driverLicenseFrontPath: String?,
        insuranceCardFrontPath: String?,
        insuranceCardBackPath: String?
    ) {
        GlobalScope.launch {
            // Driver License Front
            driverLicenseFrontPath?.let { mediaPath ->
                uploadAppointmentMediaUseCase.uploadDriverLicenseFront(mediaPath, appointmentId)
            }

            // Insurance Card Front
            insuranceCardFrontPath?.let { mediaPath ->
                uploadAppointmentMediaUseCase.uploadInsuranceCardFront(mediaPath, appointmentId)
            }

            // Insurance Card Back
            insuranceCardBackPath?.let { mediaPath ->
                uploadAppointmentMediaUseCase.uploadInsuranceCardBack(mediaPath, appointmentId)
            }
        }
    }

    private suspend fun doCheckout(
        deviceGuid: String,
        checkInVaccinations: List<CheckInVaccination>,
        shotAdministratorId: Int,
        appointmentId: Int,
        presentedRiskAssessmentId: Int?,
        paymentMode: PaymentMode,
        phoneWorkflowData: PhoneWorkflowData,
        appointmentFlags: List<String>,
        pregnancyPrompt: Boolean,
        weeksPregnant: Int?,
        paymentInformation: PaymentInformationRequestBody?,
        riskFactors: List<RiskFactor>
    ): Boolean {
        val forcedRiskType =
            if (presentedRiskAssessmentId == AppointmentViewModel.FORCED_RISK_FREE_ASSESSMENT_ID) {
                ForcedRiskType.RiskFree.value
            } else {
                ForcedRiskType.Undefined.value
            }

        val activeFeatureFlagsNames = locationRepository.getFeatureFlagsAsync()
            .map { it.featureFlagName }

        val appointmentCheckout = AppointmentCheckout(
            tabletId = deviceGuid,
            administeredVaccines = checkInVaccinations,
            administered = LocalDateTime.now(ZoneOffset.UTC),
            administeredBy = shotAdministratorId,
            presentedRiskAssessmentId = presentedRiskAssessmentId,
            forcedRiskType = forcedRiskType,
            postShotVisitPaymentModeDisplayed = paymentMode,
            phoneNumberFlowPresented = phoneWorkflowData.phoneNumberFlowPresented,
            phoneContactConsentStatus = phoneWorkflowData.phoneContactStatus,
            phoneContactReasons = phoneWorkflowData.phoneContactReasons
                .joinToString(",") { it.value },
            flags = appointmentFlags,
            pregnancyPrompt = pregnancyPrompt,
            weeksPregnant = weeksPregnant,
            activeFeatureFlags = activeFeatureFlagsNames,
            creditCardInformation = paymentInformation,
            attestHighRisk = riskFactors.isNotEmpty(),
            riskFactors = riskFactors
        )

        try {
            if (checkInVaccinations.groupBy { it.paymentMode }.size > 1) {
                appointmentRepository.updateAppointmentProcessingData(appointmentId, true)
            }

            val success = patientsApi.checkoutAppointment(
                appointmentId = appointmentId,
                appointmentCheckout = appointmentCheckout,
                ignoreOfflineStorage = paymentInformation != null
            ).isSuccessful || paymentInformation == null

            if (success) {
                appointmentRepository.upsertAdministeredVaccines(
                    appointmentId = appointmentId,
                    vaccines = checkInVaccinations.map {
                        it.toAdministeredVaccine(
                            appointmentId
                        )
                    },
                    appointmentCheckout = appointmentCheckout
                )
            }
            return success
        } catch (e: IOException) {
            Timber.e(
                e,
                "A network exception occurred when checking out AppointmentId: $appointmentId"
            )
            return false
        } catch (e: Exception) {
            Timber.e(e, "An error occurred when checking out AppointmentId: $appointmentId")
            return false
        }
    }

    fun reportLogin(
        success: Boolean,
        targetDestination: String,
        appointmentId: Int?
    ) {
        analytics.saveMetric(
            CheckoutLoginMetric(
                isSuccess = success,
                targetNavigation = targetDestination,
                appointmentId = appointmentId
            )
        )
    }
}

/**
 * Class for holding meta data for PhoneWorkFlow collection during checkout
 */
data class PhoneWorkflowData(
    var phoneNumberFlowPresented: Boolean = false,
    var phoneContactStatus: PhoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
    var phoneContactReasons: MutableSet<PhoneContactReasons> = mutableSetOf()
)
