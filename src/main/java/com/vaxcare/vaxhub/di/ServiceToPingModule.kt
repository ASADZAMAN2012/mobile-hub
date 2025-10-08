/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.model.ServiceToPing.APP_CENTER
import com.vaxcare.vaxhub.model.ServiceToPing.AZURE
import com.vaxcare.vaxhub.model.ServiceToPing.CODE_CORP
import com.vaxcare.vaxhub.model.ServiceToPing.DATADOG
import com.vaxcare.vaxhub.model.ServiceToPing.GOOGLE
import com.vaxcare.vaxhub.model.ServiceToPing.MIXPANEL
import com.vaxcare.vaxhub.model.ServiceToPing.VAXCARE
import com.vaxcare.vaxhub.model.ServiceToPingWithUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ServiceToPingModule {
    @Provides
    @Singleton
    fun providesServicesToPing(): List<ServiceToPingWithUrl> =
        listOf(
            ServiceToPingWithUrl(VAXCARE, BuildConfig.VAX_VHAPI_URL.plus("api/ping")),
            ServiceToPingWithUrl(GOOGLE, "https://www.google.com/generate_204"),
            ServiceToPingWithUrl(CODE_CORP, "https://codecorp.com"),
            ServiceToPingWithUrl(
                AZURE,
                "https://management.azure.com/metadata/endpoints?api-version=2020-01-01"
            ),
            ServiceToPingWithUrl(
                APP_CENTER,
                "https://status.dev.azure.com/_apis/status/health"
            ),
            ServiceToPingWithUrl(MIXPANEL, "https://api.mixpanel.com/track/?ip=0"),
            ServiceToPingWithUrl(DATADOG, "https://api.datadoghq.com/")
        )
}
