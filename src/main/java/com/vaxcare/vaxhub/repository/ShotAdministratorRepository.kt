/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.vaxcare.vaxhub.data.dao.ShotAdministratorDao
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.web.PatientsApi
import javax.inject.Inject

interface ShotAdministratorRepository {
    /**
     * Get all ShotAdministrators as livedata
     */
    fun getAll(): LiveData<List<ShotAdministrator>>

    /**
     * Get shotAdmin by Id
     */
    fun getById(id: Int): LiveData<ShotAdministrator?>

    /**
     * Insert list of shot administrators
     */
    suspend fun insert(shotAdministrators: List<ShotAdministrator>)

    /**
     * Delete all shot administrators
     *
     */
    suspend fun deleteAll()

    /**
     * Get all as list
     */
    suspend fun getAllAsync(): List<ShotAdministrator>

    /**
     * Get all from backend and insert
     */
    suspend fun refreshShotAdministrators(isCalledByJob: Boolean = false)
}

class ShotAdministratorRepositoryImpl @Inject constructor(
    private val patientsApi: PatientsApi,
    private val shotAdministratorDao: ShotAdministratorDao
) : ShotAdministratorRepository {
    override fun getAll(): LiveData<List<ShotAdministrator>> = shotAdministratorDao.getAll()

    override fun getById(id: Int): LiveData<ShotAdministrator?> = shotAdministratorDao.getById(id)

    override suspend fun insert(shotAdministrators: List<ShotAdministrator>) =
        shotAdministratorDao.insert(shotAdministrators)

    override suspend fun deleteAll() = shotAdministratorDao.deleteAll()

    override suspend fun getAllAsync(): List<ShotAdministrator> = shotAdministratorDao.getAllAsync()

    override suspend fun refreshShotAdministrators(isCalledByJob: Boolean) {
        val admins = patientsApi.getShotAdministrators(isCalledByJob)
        deleteAll()
        insert(admins)
    }
}
