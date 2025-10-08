package com.vaxcare.vaxhub.repository

import com.vaxcare.core.api.retrofit.WrongProductApi
import com.vaxcare.core.model.inventory.WrongProductNdcDto
import com.vaxcare.vaxhub.data.dao.WrongProductNdcDao
import com.vaxcare.vaxhub.model.toWrongProductNdc
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class WrongProductRepositoryTest {
    private val wrongProductApi = mockk<WrongProductApi>()
    private val wrongProductDao = mockk<WrongProductNdcDao>()
    private val wrongProductRepository = WrongProductRepositoryImpl(wrongProductApi, wrongProductDao)

    @Test
    fun `getAndUpsertWrongProductNdcs should call API, delete all, insert all and return list of WrongProductNdc`() =
        runBlocking {
            val listOfWrongProductNdcDto = listOf(
                WrongProductNdcDto(
                    ndc = "12345678901",
                    errorMessage = "Some error message"
                )
            )

            coEvery { wrongProductApi.getListOfWrongProductNdc() } returns listOfWrongProductNdcDto
            coEvery { wrongProductDao.insertAll(any()) } just Runs

            val result = wrongProductRepository.getAndUpsertWrongProductNdcs(false)

            coVerify(exactly = 1) { wrongProductApi.getListOfWrongProductNdc() }
            coVerify(exactly = 1) { wrongProductDao.insertAll(any()) }

            assertEquals(listOfWrongProductNdcDto.map { it.toWrongProductNdc() }, result)
        }
}
