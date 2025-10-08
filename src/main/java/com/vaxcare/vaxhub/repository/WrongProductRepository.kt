/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import com.vaxcare.core.api.retrofit.WrongProductApi
import com.vaxcare.vaxhub.data.dao.WrongProductNdcDao
import com.vaxcare.vaxhub.di.MobileVaxHubWrongProductApi
import com.vaxcare.vaxhub.model.WrongProductNdc
import com.vaxcare.vaxhub.model.toWrongProductEntity
import com.vaxcare.vaxhub.model.toWrongProductNdc
import javax.inject.Inject

interface WrongProductRepository {
    suspend fun getAndUpsertWrongProductNdcs(isCalledByJob: Boolean): List<WrongProductNdc>

    suspend fun findProductByNdc(ndc: String): WrongProductNdc?
}

/**
 * As of Aug 26th, 2024, @MobileVaxHubWrongProductApi and @MobileWrongProductNdcDao are required
 * since, without them, Hilt will grab instances from MPs,
 * provoking a compilation issue or a crash correspondingly.
 *
 * @property wrongProductApi
 * @property wrongProductDao
 */
class WrongProductRepositoryImpl @Inject constructor(
    @MobileVaxHubWrongProductApi private val wrongProductApi: WrongProductApi,
    private val wrongProductDao: WrongProductNdcDao,
) : WrongProductRepository {
    override suspend fun getAndUpsertWrongProductNdcs(isCalledByJob: Boolean): List<WrongProductNdc> {
        val listOfWrongProductNdcDto = wrongProductApi.getListOfWrongProductNdc(isCalledByJob)

        wrongProductDao.insertAll(listOfWrongProductNdcDto.map { it.toWrongProductEntity() })

        return listOfWrongProductNdcDto.map { it.toWrongProductNdc() }
    }

    override suspend fun findProductByNdc(ndc: String): WrongProductNdc? =
        wrongProductDao.findNDC(ndc)?.toWrongProductNdc()
}
