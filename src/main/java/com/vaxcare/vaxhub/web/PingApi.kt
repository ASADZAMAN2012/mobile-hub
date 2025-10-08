/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web

import com.vaxcare.vaxhub.web.constant.IS_CALLED_BY_JOB
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PingApi {
    @GET("api/ping/thirdparties")
    suspend fun pingThirdparties(
        @Query("Vaxcare") vaxcareResponseCode: Int,
        @Query("Google") googleResponseCode: Int,
        @Query("CodeCorp") codeCorpResponseCode: Int,
        @Query("Azure") azureResponseCode: Int,
        @Query("AppCenter") appCenterResponseCode: Int,
        @Query("Mixpanel") mixpanelResponseCode: Int,
        @Query("Datadog") datadogResponseCode: Int,
        @Header(value = IS_CALLED_BY_JOB) isCalledByJob: Boolean = true
    )
}
