/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.vaxcare.vaxhub.data.dao.ProviderDao
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.web.PatientsApi
import timber.log.Timber
import javax.inject.Inject

interface ProviderRepository {
    fun getAll(): LiveData<List<Provider>>

    suspend fun getAllAsync(): List<Provider>

    fun getById(id: Int): LiveData<Provider>

    suspend fun getByIdAsync(id: Int): Provider?

    suspend fun insert(providers: List<Provider>)

    suspend fun deleteAll()

    suspend fun syncProviders(isCalledByJob: Boolean = false)
}

class ProviderRepositoryImpl @Inject constructor(
    private val patientsApi: PatientsApi,
    private val providerDao: ProviderDao
) : ProviderRepository {
    override fun getAll(): LiveData<List<Provider>> = providerDao.getAll()

    override suspend fun getAllAsync(): List<Provider> = providerDao.getAllAsync()

    override fun getById(id: Int): LiveData<Provider> = providerDao.getById(id)

    override suspend fun getByIdAsync(id: Int): Provider? = providerDao.getByIdAsync(id)

    override suspend fun insert(providers: List<Provider>) = providerDao.insert(providers)

    override suspend fun deleteAll() = providerDao.deleteAll()

    override suspend fun syncProviders(isCalledByJob: Boolean) {
        try {
            val providers = patientsApi.getProviders(isCalledByJob = isCalledByJob)
            providerDao.insert(providers)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
