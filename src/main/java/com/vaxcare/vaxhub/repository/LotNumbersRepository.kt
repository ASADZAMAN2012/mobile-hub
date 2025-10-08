/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import com.vaxcare.vaxhub.data.dao.LotNumberDao
import com.vaxcare.vaxhub.model.inventory.LotNumber
import com.vaxcare.vaxhub.model.inventory.LotNumberRequestBody
import com.vaxcare.vaxhub.web.InventoryApi
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

interface LotNumbersRepository {
    suspend fun syncLotNumbers(isCalledByJob: Boolean = false)

    suspend fun postNewLotNumber(
        expirationDate: LocalDate,
        lotNumberName: String,
        productId: Int,
        source: Int,
        isCalledByJob: Boolean = false
    )

    suspend fun insertAll(lotNumberList: List<LotNumber>)
}

class LotNumbersRepositoryImpl @Inject constructor(
    private val lotNumbersDao: LotNumberDao,
    private val inventoryApi: InventoryApi
) : LotNumbersRepository {
    override suspend fun syncLotNumbers(isCalledByJob: Boolean) {
        val lotNumbers = inventoryApi.getLotNumbers(isCalledByJob)
        lotNumbersDao.deleteAll()
        insertAll(lotNumbers)
    }

    override suspend fun postNewLotNumber(
        expirationDate: LocalDate,
        lotNumberName: String,
        productId: Int,
        source: Int,
        isCalledByJob: Boolean
    ) {
        val requestBody = LotNumberRequestBody(
            expirationDate = expirationDate,
            id = -1,
            name = lotNumberName,
            productId = productId,
            salesLotNumberId = -1,
            salesProductId = -1,
            source = source
        )
        Timber.d("Post LotNumber Request Body: $requestBody")
        val lotNumber = inventoryApi.postLotNumber(
            lotNumber = requestBody,
            isCalledByJob = isCalledByJob
        )
        Timber.d("LotNumber: $lotNumber")
        insertAll(lotNumber)
    }

    override suspend fun insertAll(lotNumberList: List<LotNumber>) {
        lotNumbersDao.insertAll(lotNumberList)
    }
}
