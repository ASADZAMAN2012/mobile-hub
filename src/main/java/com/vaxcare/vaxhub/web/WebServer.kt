/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web

import com.vaxcare.core.model.SetupConfig
import com.vaxcare.vaxhub.model.CheckData
import com.vaxcare.vaxhub.model.LocationData
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.web.constant.IS_CALLED_BY_JOB
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Streaming

interface WebServer {
    @GET("api/setup/ValidatePassword")
    suspend fun validatePassword(
        @Query("password") password: String
    ): Boolean

    @GET("api/setup/checkData")
    suspend fun getCheckPartnerAndClinic(
        @Query("partnerId") partnerId: String,
        @Query("clinicId") clinicId: String
    ): CheckData

    @GET("api/setup/usersPartnerLevel")
    suspend fun getUsersForPartner(
        @Query("partnerId") partnerId: String,
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): List<User>

    @GET("api/setup/LocationData")
    suspend fun getLocationData(
        @Query("clinicId") clinicId: String,
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): LocationData?

    @Streaming()
    @GET("api/setup/downloadUpdate")
    fun downloadUpdate(
        @Query("apkUri") fileName: String,
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): Call<ResponseBody>

    @GET("api/setup/config")
    suspend fun getSetupConfig(
        @Query("isOffline") isOffline: String,
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = false
    ): SetupConfig
}
