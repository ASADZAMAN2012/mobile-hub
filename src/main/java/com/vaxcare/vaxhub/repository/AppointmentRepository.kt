/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.extension.to
import com.vaxcare.vaxhub.data.dao.AppointmentDao
import com.vaxcare.vaxhub.model.AdministeredVaccine
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.AppointmentCheckout
import com.vaxcare.vaxhub.model.AppointmentDetailDto
import com.vaxcare.vaxhub.model.AppointmentDto
import com.vaxcare.vaxhub.model.AppointmentEligibilityStatus
import com.vaxcare.vaxhub.model.AppointmentMedia
import com.vaxcare.vaxhub.model.BasePatchBody
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.MedDCheckRequestBody
import com.vaxcare.vaxhub.model.Operation
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentInformationResponse
import com.vaxcare.vaxhub.model.UpdateAppointmentRequest
import com.vaxcare.vaxhub.model.UpdatePatient
import com.vaxcare.vaxhub.model.appointment.AppointmentHubMetaData
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.EncounterMessageEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.enums.AppointmentHubContext
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.ShotStatus
import com.vaxcare.vaxhub.model.legacy.NoCheckoutReason
import com.vaxcare.vaxhub.model.legacy.generatePostDto
import com.vaxcare.vaxhub.model.partd.PartDResponse
import com.vaxcare.vaxhub.model.patient.AppointmentFlagsField
import com.vaxcare.vaxhub.model.patient.AppointmentMediaField
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.model.patient.InfoField
import com.vaxcare.vaxhub.model.patient.InfoType
import com.vaxcare.vaxhub.model.patient.PayerField
import com.vaxcare.vaxhub.util.AppointmentUtils.getNextAppointmentSlot
import com.vaxcare.vaxhub.web.PatientsApi
import retrofit2.Response
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

interface AppointmentRepository {
    /**
     * Syncs all appointments from backend and save the sync date
     *
     * @param date - sync date
     * @param removeCached - any cached appointments (currently unused in MH)
     */
    suspend fun syncAppointmentsAndSave(
        isCalledByJob: Boolean = false,
        date: LocalDate = LocalDate.now(),
        removeCached: Boolean = false
    )

    /**
     * Get AppointmentDetailDto from the backend for a given appointment Id
     *
     * @param appointmentId - appointment id for appt
     * @return - Appointment Details dto
     */
    suspend fun getAndInsertUpdatedAppointment(appointmentId: Int): AppointmentDetailDto?

    suspend fun fetchAndUpsertAppointmentAsRiskFree(appointmentId: Int)

    /**
     * Get PaymentInformation from the associated AppointmentId from the backend
     *
     * @param appointmentId - appointment id for appt
     * @return - PaymentInformation Response dto
     */
    suspend fun getPaymentInformation(appointmentId: Int): PaymentInformationResponse?

    /**
     * Get appointment from dao associated with given appointmentId
     *
     * @param appointmentId - appointment id for appt
     * @return - Appointment with associated appointmentId
     */
    suspend fun getAppointmentByIdAsync(appointmentId: Int): Appointment?

    /**
     * Get the status of the appointment to determine if risk has completed running
     *
     * @param appointmentId
     * @return
     */
    suspend fun getAppointmentEligibilityStatus(appointmentId: Int): AppointmentEligibilityStatus?

    /**
     * Gets a livedata of the associated appointmentId
     *
     * @param appointmentId - appointment id for appt
     * @return - Livedata of associated Appointment
     */
    fun getAppointmentLiveDataById(appointmentId: Int): LiveData<Appointment?>

    /**
     * Gets a livedata of the associated appointmentId
     *
     * @param appointmentId - appointment id for appt
     * @return - Livedata of associated Appointment
     */
    fun getEncounterMessagesByAppointmentId(appointmentId: Int): LiveData<List<EncounterMessageEntity>>

    /**
     * Get all appointments for a given date from the VaxCare3 backend api
     *
     * @param date - given date for return
     * @return - Appointments that all fall within the given date
     */
    suspend fun getAppointmentsByDate(date: LocalDate): List<Appointment>

    /**
     * Get all appointments
     *
     * @return - All appointments from the dao
     */
    suspend fun getAllAsync(): List<Appointment>?

    /**
     * Get all appointments for a given date from dao
     *
     * @param date - given date for return
     * @return - Livedata of all appointments from dao
     */
    suspend fun findAppointmentsByDate(date: LocalDate): List<Appointment>

    /**
     * Gets all the appointments as LiveData
     *
     * @return list of appointments LiveData
     */
    fun getAll(): LiveData<List<Appointment>>

    suspend fun deleteAndInsertByDate(date: LocalDate, newData: List<Appointment>)

    suspend fun upsertAppointments(appointments: List<Appointment>)

    suspend fun getAppointmentsByIdentifierAsync(
        identifier: String,
        startDate: Long,
        endDate: Long
    ): List<Appointment>

    suspend fun getAllByAppointmentAsync(startDate: Long, endDate: Long): List<Appointment>

    suspend fun upsertAdministeredVaccines(
        appointmentId: Int,
        vaccines: List<AdministeredVaccine>,
        appointmentCheckout: AppointmentCheckout
    )

    suspend fun updateAppointmentProcessingData(appointmentId: Int, processing: Boolean)

    /**
     * Deletes all the appointments stored
     */
    suspend fun deleteAll()

    suspend fun postAppointmentWithUTCZoneOffsetAndGetId(body: PatientPostBody): String

    suspend fun getPatientById(patientId: Int): Patient

    suspend fun getCovidSeries(patientId: Int, productId: Int): Int

    suspend fun getPartDCopays(appointmentId: Int): PartDResponse?

    suspend fun doMedDCheck(appointmentId: Int, body: MedDCheckRequestBody)

    suspend fun uploadAppointmentMedia(body: AppointmentMedia): Response<Unit>

    suspend fun updatePatient(patientId: Int, request: UpdatePatient)

    /**
     * Patch the given patientId with the given fields
     *
     * @param patientId id of patient ot patch
     * @param fields list of filled out fields
     * @param appointmentId optional appointmentId if the patient is being patched during checkout
     */
    suspend fun patchPatient(
        patientId: Int,
        fields: List<InfoField>,
        appointmentId: Int?,
        ignoreOfflineStorage: Boolean = false
    )

    /**
     * Updates the local DB/DAO and returns the new "updated" appointment.
     *
     * @param updatePatient - PatientUpdate object
     * @param patientId - patient Id of associated patient
     * @param appointmentId - appointment of associated patient
     * @return the new "updated" appointment.
     */
    suspend fun updatePatientLocally(
        updatePatient: UpdatePatient,
        patientId: Int,
        appointmentId: Int
    ): Appointment?

    /**
     * Abandon the appointment. If call fails, the appointment will be marked as abandoned.
     *
     * @param appointmentId
     * @return true if success
     */
    suspend fun abandonAppointment(appointmentId: Int): Boolean

    /**
     * Gets all AppointmentHubMetaData where context is ABANDONED
     *
     * @return list of appointment hub meta data
     */
    suspend fun getAllAbandonedAppointments(): List<AppointmentHubMetaData>

    suspend fun deleteAppointmentHubMetaDataByIds(appointmentIds: List<Int>)

    suspend fun deleteAppointmentByIds(appointmentIds: List<Int>)

    suspend fun updateAppointmentDetailsById(appointmentId: Int, isCalledByJob: Boolean = false)

    suspend fun postNoCheckoutReasons(reasons: List<NoCheckoutReason>)

    suspend fun getPotentialFamilyAppointments(appointment: Appointment): List<Appointment>

    suspend fun updateAppointment(
        appointmentId: Int,
        providerId: Int,
        date: LocalDateTime,
        visitType: String
    )
}

class AppointmentRepositoryImpl @Inject constructor(
    private val appointmentDao: AppointmentDao,
    private val patientsApi: PatientsApi,
    private val localStorage: LocalStorage
) : AppointmentRepository {
    override suspend fun syncAppointmentsAndSave(
        isCalledByJob: Boolean,
        date: LocalDate,
        removeCached: Boolean
    ) {
        val today = LocalDate.now()
        val appointments = syncAppointments(date)

        if (appointments.isNotEmpty()) {
            // insert Appointments
            Timber.d("Inserting new Appointments...")
            appointmentDao.upsert(appointments)
        }

        // set lastSyncDate to today
        localStorage.lastAppointmentSyncDate = today.toString()
    }

    private suspend fun syncAppointments(
        date: LocalDate = LocalDate.now(),
        filterIds: List<Int> = listOf(),
        isCalledByJob: Boolean = false
    ): List<Appointment> {
        val appointmentDtos = mutableListOf<AppointmentDto>()
        try {
            if (filterIds.isNotEmpty()) {
                filterIds.forEach { id ->
                    patientsApi.getAppointmentById(
                        appointmentId = id,
                        isCalledByJob = isCalledByJob
                    ).body()
                        ?.toAppointmentDto()
                        ?.let { dto -> appointmentDtos.add(dto) }
                }
            }

            Timber.i("Calling Appointment sync...")
            appointmentDtos.addAll(
                patientsApi.syncAppointments(
                    clinicId = localStorage.currentClinicId.toInt(),
                    date = date,
                    isCalledByJob = isCalledByJob
                )
                    .filter { filterIds.isEmpty() || it.id in filterIds }
            )
            appointmentDao.deleteAll()
        } catch (e: Exception) {
            Timber.d("Sync failed for: ${e.message}")
            Timber.d("Getting Appointments for today...")
            try {
                appointmentDtos.addAll(
                    patientsApi.getClinicAppointmentsByDate(
                        clinicId = localStorage.currentClinicId.toInt(),
                        isCalledByJob = isCalledByJob
                    )
                )
                appointmentDao.deleteAll()
            } catch (e: Exception) {
                Timber.e(e, "getClinicAppointmentsByDate failed")
            }
        }

        return appointmentDtos.map { it.toAppointment() }
    }

    override suspend fun getAndInsertUpdatedAppointment(appointmentId: Int): AppointmentDetailDto? {
        return try {
            patientsApi.getAppointmentById(appointmentId = appointmentId).body()
                ?.let { appointmentDetailDto ->
                    appointmentDao.upsert(listOf(appointmentDetailDto.toAppointment()))
                    appointmentDetailDto
                }
        } catch (e: Exception) {
            Timber.e(e, "getAppointmentById failed")
            null
        }
    }

    override suspend fun fetchAndUpsertAppointmentAsRiskFree(appointmentId: Int) {
        patientsApi.getAppointmentById(appointmentId = appointmentId).body()
            ?.toAppointment()?.setRiskFreeValues()?.let { appointmentWithRiskFreeValues ->
                appointmentDao.upsert(listOf(appointmentWithRiskFreeValues))
            }
    }

    private fun Appointment.setRiskFreeValues(): Appointment {
        val isEmployerPay = paymentMethod == PaymentMethod.EmployerPay
        encounterState = EncounterStateEntity(
            appointmentId = this.id,
            shotStatus = ShotStatus.PreShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        )

        encounterState?.messages = listOf(
            EncounterMessageEntity(
                // Unique value for composite key (negative appointmentId)
                riskAssessmentId = (this.id * -1),
                appointmentId = this.id,
                status = AppointmentStatus.RISK_FREE,
                icon = if (isEmployerPay) {
                    AppointmentIcon.FULL_CIRCLE
                } else {
                    AppointmentIcon.STAR
                },
                primaryMessage = "Ready to Vaccinate (Risk Free)",
                secondaryMessage = null,
                callToAction = CallToAction.None,
                topRejectCode = null,
                serviceType = AppointmentServiceType.VACCINE
            )
        )

        return this
    }

    override suspend fun getPaymentInformation(appointmentId: Int): PaymentInformationResponse? {
        return try {
            val paymentInformationResponse = patientsApi.getPaymentInformation(appointmentId)
            paymentInformationResponse
        } catch (e: Exception) {
            Timber.e(e, "getPaymentInformation failed")
            null
        }
    }

    override suspend fun getAppointmentByIdAsync(appointmentId: Int): Appointment? =
        appointmentDao.getByIdAsync(appointmentId)?.apply {
            encounterState?.messages = appointmentDao.getMessagesByAppointmentIdAsync(id)
        }

    override suspend fun getAppointmentEligibilityStatus(appointmentId: Int) =
        try {
            patientsApi.getAppointmentEligibilityStatus(appointmentId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get AppointmentStatus for AppointmentId $appointmentId")
            null
        }

    override fun getAppointmentLiveDataById(appointmentId: Int): LiveData<Appointment?> =
        appointmentDao.getById(appointmentId)

    override fun getEncounterMessagesByAppointmentId(appointmentId: Int) =
        appointmentDao.getMessagesByAppointmentId(appointmentId)

    override suspend fun getAppointmentsByDate(date: LocalDate): List<Appointment> {
        val appts = patientsApi.getClinicAppointmentsByDate(
            clinicId = localStorage.currentClinicId.toInt(),
            date = date
        )
            .map { it.toAppointment() }.sortedBy { it.appointmentTime }
        insertAndUpdateAppointments(appts, date)
        return appts
    }

    override suspend fun findAppointmentsByDate(date: LocalDate): List<Appointment> {
        val startOfDate = date.atStartOfDay()
        val endDate = startOfDate.plus(1, ChronoUnit.DAYS)
        val appts = appointmentDao.getAllByAppointmentAsync(
            startOfDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        alignEncounterStates(appts)
        return appts
    }

    private suspend fun alignEncounterStates(appointments: List<Appointment>) {
        val ids = appointments.filter { it.encounterState != null }.map { it.id }
        val messageMap =
            appointmentDao.getMessagesByAppointmentIds(ids).groupBy { it.appointmentId }
        ids.forEach { apptId ->
            messageMap[apptId]?.let { messages ->
                appointments.firstOrNull { it.id == apptId }?.encounterState?.messages = messages
            }
        }
    }

    override fun getAll(): LiveData<List<Appointment>> = appointmentDao.getAll()

    override suspend fun getAllAsync(): List<Appointment>? = appointmentDao.getAllAsync()

    override suspend fun deleteAndInsertByDate(date: LocalDate, newData: List<Appointment>) {
        val oldData = getAllAsync()?.filter { it.appointmentTime.toLocalDate() == date }
            ?: listOf()
        appointmentDao.deleteAndUpsert(oldData, newData)
    }

    override suspend fun upsertAppointments(appointments: List<Appointment>) = appointmentDao.upsert(appointments)

    /**
     * Function copied from AppointmentViewModel. Essentially "updates" given appointment ids and
     * inserts the rest
     * The appointments response (fetch by clinic & day) currently doesn't return the full patient object
     * (missing paymentInfo) So do not wipe the paymentInfo when updating
     *
     * @param appointments - list of appointments to upsert
     * @param date - date of appointments to remove
     */
    private suspend fun insertAndUpdateAppointments(appointments: List<Appointment>, date: LocalDate) {
        val currentAppointments =
            appointmentDao.getAllAsync()?.filter { it.appointmentTime.toLocalDate() == date }
                ?: listOf()
        appointmentDao.deleteAndUpsert(currentAppointments, appointments)
    }

    override suspend fun getAppointmentsByIdentifierAsync(
        identifier: String,
        startDate: Long,
        endDate: Long
    ): List<Appointment> {
        val matchText = identifier.trim().replace(",", " ")
        val firstPart = matchText.substringBefore(" ")
        val lastPart = matchText.substringAfter(" ").trim()
        val appointments = appointmentDao.getAppointmentsByIdentifierAsync(
            matchText,
            firstPart,
            lastPart,
            startDate,
            endDate
        )
        alignEncounterStates(appointments)
        return appointments
    }

    override suspend fun getAllByAppointmentAsync(startDate: Long, endDate: Long) =
        appointmentDao.getAllByAppointmentAsync(startDate, endDate)

    override suspend fun upsertAdministeredVaccines(
        appointmentId: Int,
        vaccines: List<AdministeredVaccine>,
        appointmentCheckout: AppointmentCheckout
    ) = appointmentDao.upsertAdministeredVaccines(
        appointmentId,
        vaccines,
        appointmentCheckout
    )

    override suspend fun updateAppointmentProcessingData(appointmentId: Int, processing: Boolean) =
        appointmentDao.updateAppointmentProcessingData(appointmentId, processing)

    override suspend fun deleteAll() = appointmentDao.deleteAll()

    override suspend fun postAppointmentWithUTCZoneOffsetAndGetId(body: PatientPostBody): String {
        body.date = LocalDateTime.now(ZoneOffset.UTC).getNextAppointmentSlot()

        // Specific band-aid for the backend to simulate behavior with portal as of 07/03/24
        val (paymentMode, insuranceName, primaryInsuranceId) =
            when (body.newPatient?.paymentInformation?.primaryInsuranceId) {
                Payer.PayerType.EMPLOYER.id -> "EmployerPay" to "Employer Covered" to null
                Payer.PayerType.SELF.id -> "SelfPay" to "" to null
                Payer.PayerType.OTHER.id,
                Payer.PayerType.UNINSURED.id -> "PartnerBill" to "Uninsured" to null

                else ->
                    "InsurancePay" to body.newPatient?.paymentInformation?.insuranceName to
                        body.newPatient?.paymentInformation?.primaryInsuranceId
            }

        body.newPatient?.paymentInformation?.apply {
            this.paymentMode = paymentMode
            this.insuranceName = insuranceName
            this.primaryInsuranceId = primaryInsuranceId
        }

        return patientsApi.postAppointment(body)
    }

    override suspend fun getPatientById(patientId: Int) = patientsApi.getPatientById(patientId)

    override suspend fun getCovidSeries(patientId: Int, productId: Int) =
        patientsApi.getCovidSeries(patientId, productId)

    override suspend fun getPartDCopays(appointmentId: Int) =
        try {
            patientsApi.getPartDEligibilityStatus(appointmentId.toLong()).body()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    override suspend fun doMedDCheck(appointmentId: Int, body: MedDCheckRequestBody) =
        patientsApi.doMedDCheck(appointmentId, body)

    override suspend fun uploadAppointmentMedia(body: AppointmentMedia) = patientsApi.uploadAppointmentMedia(body)

    override suspend fun updatePatient(patientId: Int, request: UpdatePatient) =
        patientsApi.updatePatient(patientId, request)

    override suspend fun patchPatient(
        patientId: Int,
        fields: List<InfoField>,
        appointmentId: Int?,
        ignoreOfflineStorage: Boolean
    ) {
        // Populate mediaTags to pass with the metadata path
        val (mediaTags, mediaPath) = fields.filterIsInstance<AppointmentMediaField>()
            .map { it.tag } to InfoType.MEDIA_PATH
        val mediaPatchBody = listOf(
            if (mediaTags.isNotEmpty()) {
                BasePatchBody(
                    op = Operation.ADD,
                    path = mediaPath,
                    value = mediaTags
                )
            } else {
                null
            }
        )

        /*
            Populate payer patch bodies. Currently the only one supported is PayerName - as the
            Mobile Hub has no feature for editing MemberId or GroupId
         */
        val payerPatchBody = fields.filterIsInstance<PayerField>().map {
            when (it) {
                is PayerField.PayerName -> {
                    BasePatchBody(
                        op = Operation.REPLACE,
                        path = it.getPatchPath(),
                        value = it.selectedInsuranceId
                    )
                }

                else -> {
                    it.currentValue?.let { value ->
                        BasePatchBody(
                            op = Operation.REPLACE,
                            path = it.getPatchPath(),
                            value = value
                        )
                    } ?: BasePatchBody(
                        op = Operation.REMOVE,
                        path = it.getPatchPath()
                    )
                }
            }
        }

        // Populate flags to pass into the metadata path
        val flagsPatchBody = fields.filterIsInstance<AppointmentFlagsField>().map {
            if (it.flags.isNotEmpty()) {
                BasePatchBody(
                    op = Operation.ADD,
                    path = it.getPatchPath(),
                    value = it.flags
                )
            } else {
                null
            }
        }

        // Populate demographic patch bodies
        val demographicPatchBody = fields.filterIsInstance<DemographicField>()
            .mapNotNull {
                it.currentValue?.let { value ->
                    BasePatchBody(
                        op = Operation.REPLACE,
                        path = it.getPatchPath(),
                        value = value
                    )
                }
            }

        val finalPatchList =
            (mediaPatchBody + payerPatchBody + flagsPatchBody + demographicPatchBody).mapNotNull { it }
        Timber.i("Patching patient with ${finalPatchList.size} changes")
        if (finalPatchList.isNotEmpty()) {
            try {
                patientsApi.patchPatient(
                    patientId,
                    appointmentId,
                    finalPatchList,
                    ignoreOfflineStorage
                )
            } catch (e: Exception) {
                Timber.e(e, "Problem PATCHING patient: $patientId from appointment: $appointmentId")
                if (ignoreOfflineStorage) {
                    throw e
                }
            }
        }
    }

    override suspend fun updatePatientLocally(
        updatePatient: UpdatePatient,
        patientId: Int,
        appointmentId: Int
    ): Appointment? {
        return with(appointmentDao.getByIdAsync(appointmentId)) {
            this?.copy(patient = updatePatient.toPatient(patientId))
        }
    }

    override suspend fun abandonAppointment(appointmentId: Int): Boolean {
        val result = try {
            patientsApi.abandonAppointment(appointmentId).isSuccessful
        } catch (e: Exception) {
            Timber.e(e, "Error abandoning appointment")
            false
        }

        if (!result) {
            appointmentDao.insertAppointmentHubMetaData(
                listOf(
                    AppointmentHubMetaData(
                        appointmentId = appointmentId,
                        updatedTime = LocalDateTime.now(),
                        context = AppointmentHubContext.ABANDONED
                    )
                )
            )
        }

        appointmentDao.deleteAllByAppointmentIds(listOf(appointmentId))
        return result
    }

    override suspend fun getAllAbandonedAppointments(): List<AppointmentHubMetaData> =
        appointmentDao.getAbandonedAppointments()

    override suspend fun deleteAppointmentHubMetaDataByIds(appointmentIds: List<Int>) =
        appointmentDao.deleteAppointmentHubMetaDataByIds(appointmentIds)

    override suspend fun deleteAppointmentByIds(appointmentIds: List<Int>) {
        appointmentDao.deleteAllByAppointmentIds(appointmentIds)
    }

    /**
     * Gets the latest appointment details from the API and insert it into the DAO
     *
     * @param appointmentId
     */
    override suspend fun updateAppointmentDetailsById(appointmentId: Int, isCalledByJob: Boolean) {
        patientsApi.getAppointmentById(isCalledByJob = true, appointmentId = appointmentId).body()
            ?.toAppointment()?.let { appointment ->
                appointmentDao.upsert(listOf(appointment))
            }
    }

    override suspend fun postNoCheckoutReasons(reasons: List<NoCheckoutReason>) {
        val body = generatePostDto(reasons.toTypedArray(), localStorage)
        try {
            patientsApi.postNoCheckoutReason(body)
        } catch (e: Exception) {
            val appointmentId = reasons.firstOrNull()?.patientVisitId
            Timber.e(e, "Problem posting noCheckoutReasons for id: $appointmentId")
        }
    }

    override suspend fun getPotentialFamilyAppointments(appointment: Appointment): List<Appointment> =
        appointmentDao.getFamilyAppointments(
            appointment.id,
            appointment.appointmentTime.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            appointment.appointmentTime.plusHours(1).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            appointment.patient.lastName,
            appointment.paymentMethod,
            appointment.paymentType
        )

    override suspend fun updateAppointment(
        appointmentId: Int,
        providerId: Int,
        date: LocalDateTime,
        visitType: String
    ) {
        val request = UpdateAppointmentRequest(
            clinicId = localStorage.currentClinicId,
            date = date,
            providerId = providerId,
            visitType = visitType
        )

        patientsApi.updateAppointment(appointmentId, request)
        appointmentDao.updateAppointmentProvider(
            appointmentId = appointmentId,
            providerId = providerId
        )
    }
}
