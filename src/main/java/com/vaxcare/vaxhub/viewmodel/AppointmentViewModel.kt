/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.model.checkout.CheckoutInsuranceCardCollectionFlow.InsuranceCardCollectionMethod
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.data.dao.AppointmentDao
import com.vaxcare.vaxhub.domain.UploadAppointmentMediaUseCase
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CurbsideInsuranceID
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.model.PaymentInformationResponse
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.VaccineWithIssues
import com.vaxcare.vaxhub.model.appointment.PhoneContactConsentStatus
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.RiskFactor
import com.vaxcare.vaxhub.model.inventory.OrderProductWrapper
import com.vaxcare.vaxhub.model.patient.AppointmentFlagsField
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.model.patient.InfoField
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.ui.patient.edit.CheckoutCollectDemographicInfoFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val appointmentDao: AppointmentDao,
    private val localStorage: LocalStorage,
    private val appointmentRepository: AppointmentRepository,
    private val appointmentMediaUseCase: UploadAppointmentMediaUseCase
) : ViewModel() {
    companion object {
        const val APPOINTMENT_ID = "appointment_id"
        const val ADD_PATIENT_SOURCE = "add_patient_source"
        const val PAYMENT_INFORMATION = "payment_information"

        const val PHONE_FLOW = "phone_flow"
        const val FORCED_RISK_FREE_ASSESSMENT_ID = -3
    }

    var appointmentCreation: AppointmentCreation = AppointmentCreation()
    var currentCheckout = CurrentCheckout()
    private var familyMemberInfo = FamilyMemberInfo()

    fun getCurrentShotAdministratorId() = currentCheckout.administrator?.id ?: localStorage.userId

    /**
     * List of delta fields - eventually gets boiled down to the patch call
     */
    private val patientEditedFields = mutableSetOf<FieldWrapper>()
    val deltaFields: List<InfoField>
        get() = patientEditedFields.map { it.infoField }

    // Get the current child's appointment id
    fun getChildAppointmentId(): Int? {
        familyMemberInfo.currentChildAppointmentId?.let { childApptId ->
            if (familyMemberInfo.countOfCreatedParentAppointments in 0..1) {
                return childApptId
            }
        }
        return null
    }

    // After successfully creating a parent, increase the parent count
    private fun increaseParentAppointmentCount() {
        familyMemberInfo.currentChildAppointmentId?.let {
            familyMemberInfo.countOfCreatedParentAppointments += 1
        }
    }

    fun clearAppointmentCreation() {
        appointmentCreation = AppointmentCreation()
    }

    // gets
    fun getById(appointmentId: Int): LiveData<Appointment?> = appointmentDao.getById(appointmentId)

    suspend fun getByIdAsync(id: Int) =
        appointmentDao.getByIdAsync(id)?.apply {
            encounterState?.messages = appointmentDao.getMessagesByAppointmentIdAsync(id)
        }

    fun getAll() = appointmentDao.getAll()

    // deletes
    fun deleteAll() =
        viewModelScope.safeLaunch {
            try {
                appointmentDao.deleteAll()
            } catch (e: Exception) {
                Timber.e(e, "Error clearing appointments")
            }
        }

    // api
    suspend fun getUpdatedAppointment(appointmentId: Int): Appointment? {
        return appointmentRepository.getAndInsertUpdatedAppointment(appointmentId)?.toAppointment()
            ?.apply {
                encounterState?.messages =
                    appointmentDao.getMessagesByAppointmentIdAsync(appointmentId)
            }
    }

    suspend fun getPaymentInformation(appointmentId: Int): PaymentInformationResponse? =
        appointmentRepository.getPaymentInformation(appointmentId)

    suspend fun createAppointmentWithPatientId(
        patientId: Int,
        providerId: Int?,
        patientCollectData: PatientCollectData?,
        count: Int
    ): Pair<String?, Int> {
        appointmentCreation = AppointmentCreation()
        appointmentCreation.patientId = patientId
        appointmentCreation.providerId = providerId
        return createAppointment(count, patientCollectData)
    }

    suspend fun createAppointment(count: Int, patientCollectData: PatientCollectData?): Pair<String?, Int> =
        try {
            val patientPostBody = if (appointmentCreation.patientId != null) {
                PatientPostBody(
                    patientId = appointmentCreation.patientId,
                    clinicId = localStorage.currentClinicId,
                    providerId = appointmentCreation.providerId,
                    visitType = "Well"
                )
            } else {
                PatientPostBody(
                    newPatient = appointmentCreation.newPatient,
                    clinicId = localStorage.currentClinicId,
                    providerId = appointmentCreation.providerId,
                    initialPaymentMode = appointmentCreation.newPatient?.paymentInformation?.paymentMode,
                    visitType = "Well"
                )
            }.apply {
                val data = patientCollectData ?: PatientCollectData(
                    frontInsurancePath = appointmentCreation.insuranceCardFrontPath,
                    backInsurancePath = appointmentCreation.insuranceCardBackPath,
                    driverLicenseFrontPath = appointmentCreation.driverLicenseFrontPath
                )
                data.let {
                    when {
                        it.phoneContactAgreement -> {
                            this.metaData =
                                PatientPostBody.AppointmentMetaData(
                                    flags = listOf(
                                        PatientPostBody
                                            .AppointmentFlagType.PatientContactPhoneOptIn.value
                                    )
                                )
                        }

                        it.hasUploadedCard() -> {
                            this.metaData =
                                PatientPostBody.AppointmentMetaData(
                                    mediaProvided = this.getMediaProvidedFromCollection(
                                        it
                                    )
                                )
                        }
                    }
                }
            }

            if (appointmentCreation.newPatient?.paymentInformation == null) {
                appointmentCreation.payer?.let {
                    patientPostBody.newPatient?.paymentInformation = when (it.id) {
                        Payer.PayerType.OTHER.id -> {
                            PatientPostBody.PaymentInformation(
                                primaryInsuranceId = CurbsideInsuranceID.OTHER_PAYER.value
                            )
                        }

                        Payer.PayerType.UNINSURED.id -> {
                            PatientPostBody.PaymentInformation(
                                primaryInsuranceId = CurbsideInsuranceID.SELF_PAY.value,
                                uninsured = true
                            )
                        }

                        Payer.PayerType.SELF.id -> {
                            PatientPostBody.PaymentInformation(
                                primaryInsuranceId = CurbsideInsuranceID.SELF_PAY.value,
                                uninsured = false
                            )
                        }

                        else -> PatientPostBody.PaymentInformation(
                            primaryInsuranceId = it.insuranceId,
                            primaryInsurancePlanId = it.insurancePlanId
                        )
                    }
                }
            }

            val appointmentId =
                appointmentRepository.postAppointmentWithUTCZoneOffsetAndGetId(patientPostBody)

            // Need to support create parent for patient
            increaseParentAppointmentCount()
            Pair(appointmentId, count + 1)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, count + 1)
        }

    suspend fun uploadPatientPhotos(appointmentId: Int) {
        uploadPatientInsurance(
            appointmentId,
            appointmentCreation.driverLicenseFrontPath,
            appointmentCreation.insuranceCardFrontPath,
            appointmentCreation.insuranceCardBackPath
        )
    }

    private suspend fun uploadPatientInsurance(
        appointmentId: Int,
        driverLicenseFrontPath: String?,
        insuranceCardFrontPath: String?,
        insuranceCardBackPath: String?
    ) {
        // Driver License Front
        driverLicenseFrontPath?.let { mediaPath ->
            appointmentMediaUseCase.uploadDriverLicenseFront(mediaPath, appointmentId)
        }

        // Insurance Card Front
        insuranceCardFrontPath?.let { mediaPath ->
            appointmentMediaUseCase.uploadInsuranceCardFront(mediaPath, appointmentId)
        }

        // Insurance Card Back
        insuranceCardBackPath?.let { mediaPath ->
            appointmentMediaUseCase.uploadInsuranceCardBack(mediaPath, appointmentId)
        }
    }

    fun getParentPaymentInfoSameAsChild(payment: PaymentInformation): PatientPostBody.PaymentInformation {
        return PatientPostBody.PaymentInformation().apply {
            if (payment.primaryInsuranceId != null && payment.primaryInsuranceId > 0) {
                primaryInsuranceId = payment.primaryInsuranceId
            }
            if (payment.primaryInsurancePlanId != null && payment.primaryInsurancePlanId > 0) {
                primaryInsurancePlanId = payment.primaryInsurancePlanId
            }
            primaryMemberId = payment.primaryMemberId
            primaryGroupId = payment.primaryGroupId
            relationshipToInsured = RelationshipToInsured.Self

            when (payment.paymentMode) {
                PaymentMethod.SelfPay,
                PaymentMethod.InsurancePay -> {
                    paymentMode = payment.paymentMode.name
                }

                PaymentMethod.PartnerBill -> {
                    paymentMode = payment.paymentMode.name
                    insuranceName = payment.insuranceName
                }

                else -> {
                    paymentMode = PaymentMethod.SelfPay.name
                }
            }
        }
    }

    suspend fun getCovidSeries(productId: Int): Int {
        try {
            // If no shot history, auto-select 1st Dose
            // If one shot in history, auto-select 2nd Dose
            // If two (or more) shots in history, auto-select 3rd Dose
            val series =
                appointmentRepository.getCovidSeries(
                    currentCheckout.selectedAppointment!!.patient.id,
                    productId
                )
            return if (series <= 3) series else 3
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1
    }

    /**
     * Add edited fields from Info Collection flow replacing existing values
     *
     * @param tagSet tag from associated screen
     * @param fields delta field(s) to add
     */
    fun addEditedFields(tagSet: String, vararg fields: InfoField) {
        patientEditedFields.removeIf { patientEditedField ->
            patientEditedField.tagSet == tagSet &&
                patientEditedField.infoField::class.java in fields.map { field ->
                    field::class.java
                }
        }
        patientEditedFields += fields.map { FieldWrapper(tagSet, it) }.toHashSet()
    }

    fun clearAllPhoneDeltas(tagSet: String) =
        patientEditedFields.removeAll { wrapper ->
            wrapper.tagSet == tagSet && (
                wrapper.infoField is AppointmentFlagsField ||
                    wrapper.infoField is DemographicField
            )
        }

    fun clearEditedFields(tagSet: String) = patientEditedFields.removeAll { it.tagSet == tagSet }

    private fun clearAllEditedFields() = patientEditedFields.clear()

    /**
     * Reset all data in the current checkout
     */
    fun clearCurrentCheckout() {
        currentCheckout = CurrentCheckout()
        clearAllEditedFields()
        clearFamilyMemberInfo()
    }

    /**
     * Reset related child appointment data for curbside family flow
     *
     */
    private fun clearFamilyMemberInfo() {
        familyMemberInfo = FamilyMemberInfo()
    }

    /**
     * Add PhoneOptIn flag when phoneContactAgreement is true.
     * Remove any previous if existing - there are two flows that can have this, but we only want to
     * send the most recent change.
     *
     * @param tagSet tag from associated screen
     * @param data PatientCollectData to check
     * @param reasons reasons enum(s) to apply to currentCheckout if agreement was reached
     * @return the incoming phoneContactAgreement
     */
    fun addPhoneCollectField(
        tagSet: String,
        data: PatientCollectData,
        vararg reasons: PhoneContactReasons
    ): Boolean {
        clearAllPhoneDeltas(tagSet)
        currentCheckout.phoneNumberFlowPresented = true
        currentCheckout.phoneContactStatus =
            PhoneContactConsentStatus.fromBoolean(data.phoneContactAgreement)
        patientEditedFields.removeAll {
            when (it.infoField) {
                is AppointmentFlagsField.PhoneOptIn -> true
                is DemographicField.Phone ->
                    it.tagSet != CheckoutCollectDemographicInfoFragment.FRAGMENT_TAG

                else -> false
            }
        }

        val fieldsToAdd = if (data.phoneContactAgreement) {
            currentCheckout.phoneContactReasons.clear()
            currentCheckout.phoneContactReasons.addAll(reasons.toSet())
            listOf(
                DemographicField.Phone(data.currentPhone),
                AppointmentFlagsField.PhoneOptIn()
            )
        } else {
            currentCheckout.phoneContactReasons.clear()
            currentCheckout.phoneContactReasons.add(PhoneContactReasons.DO_NOT_CONTACT)
            listOf(AppointmentFlagsField.PhoneOptOut())
        }

        addEditedFields(tagSet, *fieldsToAdd.toTypedArray())
        return data.phoneContactAgreement
    }

    fun deltaPhoneOptInPresent() =
        patientEditedFields
            .filter { it.tagSet != "CollectPaymentFragment" }
            .any { it.infoField is AppointmentFlagsField.PhoneOptIn }

    fun deltaPhoneOptOutPresent() =
        patientEditedFields
            .filter { it.tagSet != "CollectPaymentFragment" }
            .any { it.infoField is AppointmentFlagsField.PhoneOptOut }
}

/**
 * Wrapper for InfoField to associate with a unique "tagset" per screen instance
 */
private data class FieldWrapper(
    val tagSet: String,
    val infoField: InfoField
) {
    override fun equals(other: Any?): Boolean =
        when (other) {
            is FieldWrapper -> {
                this.infoField::class.java == other.infoField::class.java &&
                    this.infoField.currentValue == other.infoField.currentValue
            }

            else -> false
        }

    override fun hashCode(): Int {
        return tagSet.hashCode()
    }
}

data class AppointmentCreation(
    var patientId: Int? = null,
    var newPatient: PatientPostBody.NewPatient? = null,
    var driverLicenseFrontPath: String? = null,
    var insuranceCardFrontPath: String? = null,
    var insuranceCardBackPath: String? = null,
    var payer: Payer? = null,
    var providerId: Int? = null
)

data class FamilyMemberInfo(
    var currentChildAppointmentId: Int? = null,
    var countOfCreatedParentAppointments: Int = 0
)

/**
 * Represents the entire checkout with all information accessible on demand
 */
data class CurrentCheckout(
    // Checkout flow
    var selectedAppointment: Appointment? = null,
    var manualDob: LocalDate? = null,
    var pendingProduct: VaccineWithIssues? = null,
    var isEditCheckoutFromComplete: Boolean = false,
    var stagedProducts: MutableList<VaccineAdapterProductDto> = mutableListOf(),
    var patientId: Int = -1,
    var administrator: ShotAdministrator? = null,
    var orderWrapper: OrderProductWrapper? = null,
    var presentedRiskAssessmentId: Int? = null,
    // Insurance card collection metrics
    var insuranceCardFlowPresented: Boolean = false,
    var insuranceCollectionMethod: InsuranceCardCollectionMethod = InsuranceCardCollectionMethod.NONE,
    // Phone collection metrics
    var phoneNumberFlowPresented: Boolean = false,
    var phoneContactStatus: PhoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
    var phoneContactReasons: MutableSet<PhoneContactReasons> = mutableSetOf(),
    var isLocallyCreated: Boolean = false,
    // Age Warning data (Maternal RSV, COVID Under 65)
    var riskFactors: List<RiskFactor> = listOf(),
    var pregnancyPrompt: Boolean = false,
    var weeksPregnant: Int? = null,
    // Enterprise - used to determine if flip
    var isVaxCare3: Boolean = false,
) {
    fun containsMedDDoses(): Boolean {
        val removedStates = listOf(
            DoseState.ADMINISTERED_REMOVED,
            DoseState.REMOVED
        )
        return stagedProducts.filter { it.doseState !in removedStates }
            .filter { ProductIssue.ProductNotCovered !in it.vaccineIssues }
            .any { it.hasCopay(true) }
    }

    fun flipPartnerBill(reason: PaymentModeReason) {
        stagedProducts
            .forEach {
                when (it.paymentMode) {
                    PaymentMode.SelfPay -> {
                        it.originalPaymentModeReason = it.paymentModeReason
                        it.paymentModeReason = PaymentModeReason.SelfPayOptOut
                    }

                    PaymentMode.PartnerBill -> Unit
                    else -> it.paymentModeReason = reason
                }

                it.flipPartnerBill(true)
            }
    }

    /**
     * Reverts payment flips that may have occurred during the phone collection flow
     * but navigating back from summary screen
     */
    fun revertPaymentFlips() {
        stagedProducts
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
}
