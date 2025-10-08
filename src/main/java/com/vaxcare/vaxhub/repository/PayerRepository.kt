/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.vaxcare.vaxhub.data.dao.PayerDao
import com.vaxcare.vaxhub.model.Payer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface PayerRepository {
    fun searchPayers(identifier: String): LiveData<List<Payer>>

    suspend fun insertPayers(data: List<Payer>)

    fun getTwoMostRecentPayers(): LiveData<List<Payer>>

    suspend fun getTwoMostRecentPayersAsync(): List<Payer>?

    suspend fun updatePayer(payer: Payer)

    suspend fun deleteAll()

    suspend fun searchPayersAsync(identifier: String): List<Payer>

    suspend fun getPayerByInsuranceId(id: Int): Payer?

    fun getThreeMostRecentPayers(): Flow<List<Payer>>

    suspend fun getThreeMostRecentPayersAsync(): List<Payer>
}

class PayerRepositoryImpl @Inject constructor(
    private val payerDao: PayerDao
) : PayerRepository {
    override fun searchPayers(identifier: String): LiveData<List<Payer>> = payerDao.getPayersByIdentifier(identifier)

    override suspend fun insertPayers(data: List<Payer>) = payerDao.insert(data)

    override fun getTwoMostRecentPayers(): LiveData<List<Payer>> = payerDao.getLastTwoRecentPayers()

    override suspend fun getTwoMostRecentPayersAsync(): List<Payer>? = payerDao.getLastTwoRecentPayersAsync()

    override suspend fun updatePayer(payer: Payer) = payerDao.update(payer)

    override suspend fun deleteAll() = payerDao.deleteAll()

    override suspend fun searchPayersAsync(identifier: String): List<Payer> =
        payerDao.getPayersByIdentifierAsync(identifier)

    override suspend fun getPayerByInsuranceId(id: Int): Payer? = payerDao.getPayerByInsuranceId(id)

    override fun getThreeMostRecentPayers(): Flow<List<Payer>> = payerDao.getLastThreeRecentPayers()

    override suspend fun getThreeMostRecentPayersAsync(): List<Payer> = payerDao.getLastThreeRecentPayersAsync()
}
