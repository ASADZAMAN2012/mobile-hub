/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import com.vaxcare.vaxhub.model.AppointmentDto
import com.vaxcare.vaxhub.model.SearchPatient
import com.vaxcare.vaxhub.model.partd.PartDResponse
import com.vaxcare.vaxhub.web.PatientsApi
import retrofit2.Response
import java.time.LocalDate
import javax.inject.Inject

interface PatientRepository {
    suspend fun searchPatients(queryString: String): List<SearchPatient>

    suspend fun getClinicAppointmentsByDate(clinicId: Int, date: LocalDate = LocalDate.now()): List<AppointmentDto>

    suspend fun getMedDCopays(appointmentId: Int): Response<PartDResponse?>
}

class PatientRepositoryImpl @Inject constructor(private val patientsApi: PatientsApi) : PatientRepository {
    override suspend fun searchPatients(queryString: String): List<SearchPatient> {
        return patientsApi.searchPatients(queryString)
    }

    override suspend fun getClinicAppointmentsByDate(clinicId: Int, date: LocalDate): List<AppointmentDto> {
        return patientsApi.getClinicAppointmentsByDate(clinicId, date)
    }

    override suspend fun getMedDCopays(appointmentId: Int): Response<PartDResponse?> =
        patientsApi.getPartDEligibilityStatus(appointmentId.toLong())
}
