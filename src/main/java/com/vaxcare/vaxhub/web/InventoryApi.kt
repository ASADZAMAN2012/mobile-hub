/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web

import com.vaxcare.vaxhub.model.inventory.LotNumber
import com.vaxcare.vaxhub.model.inventory.LotNumberRequestBody
import com.vaxcare.vaxhub.model.inventory.ProductDto
import com.vaxcare.vaxhub.model.inventory.ProductOneTouch
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProductDTO
import com.vaxcare.vaxhub.model.legacy.LegacyProductMapping
import com.vaxcare.vaxhub.web.constant.IS_CALLED_BY_JOB
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface InventoryApi {
    @GET("api/inventory/lotnumbers")
    suspend fun getLotNumbers(
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false,
        @Query("maximumExpirationAgeInDays") maximumExpirationAgeInDays: Int = 365,
    ): List<LotNumber>

    @GET("api/inventory/product/v2")
    suspend fun getProducts(
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<ProductDto>

    @POST("api/inventory/lotnumbers")
    suspend fun postLotNumber(
        @Body lotNumber: LotNumberRequestBody,
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<LotNumber>

    @GET("api/inventory/product/mappings")
    suspend fun getProductMappings(
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<LegacyProductMapping>

    @GET("api/inventory/product/onetouch")
    suspend fun getProductOneTouch(
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<ProductOneTouch>

    @GET("api/inventory/LotInventory/SimpleOnHand")
    suspend fun getSimpleOnHandInventory(
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<SimpleOnHandProductDTO>
}
