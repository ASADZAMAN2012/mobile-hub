/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.data.dao.ClinicDao
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.web.PatientsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

interface ClinicRepository {
    /**
     * The checked Clinic Date
     */
    var checkedClinicDate: LocalDate

    /**
     * Get current partner id
     *
     * @return the current partner id
     */
    fun getPartnerId(): Long

    /**
     * Get parent clinic id
     * The parent clinic id is related the location selected on login use this value to ignore
     * the current clinic id that may be a temporary clinic id
     *
     * @return the current parent clinic id
     */
    fun getParentClinicId(): Long

    /**
     * Get current clinic id
     * This value may be the parent clinic id or a temporary clinic id
     *
     * @return the current clinic id
     */
    fun getCurrentClinicId(): Long

    /**
     * Get the current clinic assigned via ClinicId in SharedPrefs
     */
    suspend fun getParentClinic(): Clinic?

    /**
     * Get the current clinic assigned via ClinicId in SharedPrefs
     */
    suspend fun getCurrentClinic(): Clinic?

    /**
     * Sync clinics associated to the partner from upstream
     */
    suspend fun syncClinics(isCalledByJob: Boolean = false)

    /**
     * Get all clinics
     *
     * @return - LiveData of all clinics in dao
     */
    suspend fun getClinics(): List<Clinic>

    /**
     * Get clinic by id
     *
     * @param id - the Id of desired clinic
     * @return - LiveData of the associated clinic
     */
    fun getClinicById(id: Long): LiveData<Clinic>

    /**
     * Switch the current clinic id
     *
     * @param id the new clinic id
     */
    fun switchClinicId(id: Long)
}

class ClinicRepositoryImpl @Inject constructor(
    private val patientsApi: PatientsApi,
    private val clinicDao: ClinicDao,
    private val localStorage: LocalStorage
) : ClinicRepository {
    override var checkedClinicDate: LocalDate
        get() = localStorage.checkedClinicDate ?: LocalDate.now()
        set(value) {
            localStorage.checkedClinicDate = value
        }

    override fun getPartnerId(): Long = localStorage.partnerId

    override fun getParentClinicId(): Long = localStorage.clinicId

    override fun getCurrentClinicId(): Long = localStorage.currentClinicId

    override suspend fun getParentClinic(): Clinic? =
        withContext(Dispatchers.IO) {
            clinicDao.getByIdAsync(localStorage.clinicId)
        }

    override suspend fun getCurrentClinic(): Clinic? =
        withContext(Dispatchers.IO) {
            clinicDao.getByIdAsync(localStorage.currentClinicId)
        }

    override suspend fun syncClinics(isCalledByJob: Boolean) =
        withContext(Dispatchers.IO) {
            val clinics = patientsApi.getClinics(isCalledByJob)
            Timber.d("Insert clinic size: ${clinics.size}")
            clinicDao.insertAll(clinics)
        }

    override suspend fun getClinics(): List<Clinic> = withContext(Dispatchers.IO) { clinicDao.getClinics() }

    override fun getClinicById(id: Long): LiveData<Clinic> = clinicDao.getById(id)

    override fun switchClinicId(id: Long) {
        localStorage.switchClinicId(id)
    }
}
