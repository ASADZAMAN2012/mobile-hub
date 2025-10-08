/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web

import com.vaxcare.vaxhub.model.AppointmentCheckout
import com.vaxcare.vaxhub.model.AppointmentDetailDto
import com.vaxcare.vaxhub.model.AppointmentDto
import com.vaxcare.vaxhub.model.AppointmentEligibilityStatus
import com.vaxcare.vaxhub.model.AppointmentMedia
import com.vaxcare.vaxhub.model.BasePatchBody
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.model.MedDCheckRequestBody
import com.vaxcare.vaxhub.model.MedDCheckResponse
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentInformationResponse
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.SearchPatient
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.model.UpdateAppointmentRequest
import com.vaxcare.vaxhub.model.UpdatePatient
import com.vaxcare.vaxhub.model.legacy.LegacyPostDto
import com.vaxcare.vaxhub.model.legacy.NoCheckoutReason
import com.vaxcare.vaxhub.model.partd.PartDResponse
import com.vaxcare.vaxhub.web.constant.IGNORE_OFFLINE_STORAGE
import com.vaxcare.vaxhub.web.constant.IS_CALLED_BY_JOB
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate

interface PatientsApi {
    @GET("api/patients/appointment")
    suspend fun getClinicAppointmentsByDate(
        @Query("clinicId") clinicId: Int,
        @Query("date") date: LocalDate = LocalDate.now(),
        @Query("version") version: String = "2.0",
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<AppointmentDto>

    @GET("api/patients/appointment/sync")
    suspend fun syncAppointments(
        @Query("clinicId") clinicId: Int,
        @Query("date") date: LocalDate = LocalDate.now(),
        @Query("version") version: String = "2.0",
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<AppointmentDto>

    @GET("api/patients/appointment/{appointmentId}")
    suspend fun getAppointmentById(
        @Path("appointmentId") appointmentId: Int,
        @Query("version") version: String = "2.0",
        @Header(IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): Response<AppointmentDetailDto?>

    @GET("api/patients/appointment/{appointmentId}/appointmentStatus")
    suspend fun getAppointmentEligibilityStatus(
        @Path("appointmentId") appointmentId: Int
    ): AppointmentEligibilityStatus?

    @GET("api/patients/staffer/providers")
    suspend fun getProviders(
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<Provider>

    @GET("api/patients/staffer/shotadministrators")
    suspend fun getShotAdministrators(
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<ShotAdministrator>

    @PUT("api/patients/appointment/{appointmentId}/checkout")
    suspend fun checkoutAppointment(
        @Path("appointmentId") appointmentId: Int,
        @Body appointmentCheckout: AppointmentCheckout,
        @Header(IGNORE_OFFLINE_STORAGE) ignoreOfflineStorage: Boolean
    ): Response<Unit>

    @POST("api/patients/appointment")
    suspend fun postAppointment(
        @Body requestBody: PatientPostBody
    ): String

    @PUT("api/patients/appointment/{appointmentId}")
    suspend fun updateAppointment(
        @Path("appointmentId") appointmentId: Int,
        @Body request: UpdateAppointmentRequest
    )

    @PUT("api/patients/appointment/media")
    suspend fun uploadAppointmentMedia(
        @Body appointmentMedia: AppointmentMedia
    ): Response<Unit>

    @GET("api/patients/insurance/bystate/{state}")
    suspend fun getPayers(
        @Header("IsCalledByJob") isCalledByJob: Boolean = false,
        @Path("state") state: String,
        @Query("contractedOnly") contractedOnly: Boolean
    ): List<Payer>

    @GET("/api/patients/patient/search")
    suspend fun searchPatients(
        @Query("queryString") queryString: String
    ): List<SearchPatient>

    @GET("api/patients/patient/{patientId}")
    suspend fun getPatientById(
        @Path("patientId") patientId: Int
    ): Patient

    @GET("api/patients/clinic")
    suspend fun getClinics(
        @Header(IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<Clinic>

    @GET("api/covid/seriessuggestion")
    suspend fun getCovidSeries(
        @Query("PatientId") patientId: Int,
        @Query("ProductId") productId: Int
    ): Int

    @POST("api/patients/meddcopay/{appointmentId}/check")
    suspend fun doMedDCheck(
        @Path("appointmentId") appointmentId: Int,
        @Body requestBody: MedDCheckRequestBody
    )

    @Deprecated("Do not use this. It is only here for old automated tests")
    @GET("api/patients/meddcopay/{appointmentId}")
    suspend fun getMedDCopays(
        @Path("appointmentId") appointmentId: Int
    ): MedDCheckResponse

    @GET("api/partD/GetPatientEligibilityStatus")
    suspend fun getPartDEligibilityStatus(
        @Query("patientVisitID") patientVisitId: Long
    ): Response<PartDResponse?>

    @GET("api/patients/paymentinformation/{appointmentId}")
    suspend fun getPaymentInformation(
        @Path("appointmentId") appointmentId: Int
    ): PaymentInformationResponse

    @PUT("api/patients/patient/{patientId}")
    suspend fun updatePatient(
        @Path("patientId") patientId: Int,
        @Body request: UpdatePatient
    )

    @PATCH("api/patients/patient/{patientId}")
    suspend fun patchPatient(
        @Path("patientId") patientId: Int,
        @Query("appointmentId") appointmentId: Int?,
        @Body request: List<BasePatchBody>,
        @Header(IGNORE_OFFLINE_STORAGE) ignoreOfflineStorage: Boolean
    )

    @POST("api/patients/appointment/noCheckoutReason")
    suspend fun postNoCheckoutReason(
        @Body checkinNoCheckoutReason: LegacyPostDto<Array<NoCheckoutReason>>
    )

    @PUT("api/patients/appointment/{appointmentId}/abandon")
    suspend fun abandonAppointment(
        @Path("appointmentId") appointmentId: Int
    ): Response<Unit>
}
