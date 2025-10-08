/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web

import com.vaxcare.vaxhub.model.order.OrderJson
import com.vaxcare.vaxhub.web.constant.IS_CALLED_BY_JOB
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface OrmApi {
    @GET("api/interfaces/orm/orders")
    suspend fun getOrdersChanges(
        @Query("clinicId") clinicId: Int,
        @Query("lastSyncDateUtc", encoded = true) date: String
    ): List<OrderJson>

    @GET("api/interfaces/orm/orders/search")
    suspend fun getOrdersByGroup(
        @Query("placerGroupNumber") orderGroupNumber: String,
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<OrderJson>
}
