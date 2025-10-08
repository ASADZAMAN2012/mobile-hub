/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.vaxcare.core.api.model.ApiInfo
import com.vaxcare.core.api.model.ApiInfoImpl
import com.vaxcare.core.api.retrofit.LoginApi
import com.vaxcare.core.api.retrofit.WrongProductApi
import com.vaxcare.core.api.retrofit.interceptor.DnsSelector
import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.storage.preference.UserSessionManager
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.vaxhub.AppInfo
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.data.dao.OfflineRequestDao
import com.vaxcare.vaxhub.data.typeconverter.SerializeNulls
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.ProductPresentation
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LotNumbersRepository
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.web.InventoryApi
import com.vaxcare.vaxhub.web.OrmApi
import com.vaxcare.vaxhub.web.PatientsApi
import com.vaxcare.vaxhub.web.PingApi
import com.vaxcare.vaxhub.web.WebServer
import com.vaxcare.vaxhub.web.interceptor.MessageSourceInterceptor
import com.vaxcare.vaxhub.web.interceptor.StatusCodeHandler
import com.vaxcare.vaxhub.web.interceptor.VaxCareDataDogInterceptor
import com.vaxcare.vaxhub.web.interceptor.VaxHubIdentifier
import com.vaxcare.vaxhub.web.interceptor.WebLogger
import com.vaxcare.vaxhub.web.offline.OfflineRequestResponseHandler
import com.vaxcare.vaxhub.web.offline.OfflineRequestResponseHandlerImpl
import com.vaxcare.vaxhub.web.offline.OfflineRequestValidator
import com.vaxcare.vaxhub.web.offline.OfflineRequestValidatorImpl
import com.vaxcare.vaxhub.web.typeadapter.BooleanAdapters
import com.vaxcare.vaxhub.web.typeadapter.InventorySourceTypeAdapter
import com.vaxcare.vaxhub.web.typeadapter.TimeAdapter
import com.vaxcare.vaxhub.web.typeadapter.TypeAdapters
import com.vaxcare.vaxhub.web.typeadapter.legacy.LegacyTransactionTypeAdapter
import com.vaxcare.vaxhub.web.typeadapter.legacy.LegacyVaccineCountTypeAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileMoshi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileVaxHubWebServer

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileVaxHubLogin

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileOfflineValidator

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobileVaxHubWrongProductApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobilePicasso

@Module
@InstallIn(SingletonComponent::class)
object WebModule {
    @Provides
    @MobileMoshi
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(
                RouteCode::class.java,
                EnumJsonAdapter.create(RouteCode::class.java)
                    .withUnknownFallback(RouteCode.UNKNOWN)
            )
            .add(
                ProductPresentation::class.java,
                EnumJsonAdapter.create(ProductPresentation::class.java)
                    .withUnknownFallback(ProductPresentation.UNKNOWN)
            )
            .add(
                PaymentMethod::class.java,
                EnumJsonAdapter.create(PaymentMethod::class.java)
                    .withUnknownFallback(PaymentMethod.InsurancePay)
            ).add(
                UpdateSeverity::class.java,
                EnumJsonAdapter.create(UpdateSeverity::class.java)
                    .withUnknownFallback(UpdateSeverity.NoAction)
            ).add(LegacyVaccineCountTypeAdapter())
            .add(LegacyTransactionTypeAdapter())
            .add(InventorySourceTypeAdapter())
            .add(TimeAdapter())
            .add(BooleanAdapters())
            .add(TypeAdapters())
            .add(KotlinJsonAdapterFactory())
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(SerializeNulls.jsonAdapterFactory)
            .build()

    @Provides
    @Singleton
    fun provideOfflineRequestResponseHandler(
        @MobileMoshi moshi: Moshi,
        appointmentRepository: AppointmentRepository,
        lotNumbersRepository: LotNumbersRepository
    ): OfflineRequestResponseHandler =
        OfflineRequestResponseHandlerImpl(
            moshi = moshi,
            appointmentRepository = appointmentRepository,
            lotNumbersRepository = lotNumbersRepository
        )

    @Provides
    @MobileOfflineValidator
    @Singleton
    fun provideOfflineRequestValidator(
        @MobileMoshi moshi: Moshi,
        fileStorage: FileStorage,
        appInfo: AppInfo
    ): OfflineRequestValidator =
        OfflineRequestValidatorImpl(
            moshi = moshi,
            fileStorage = fileStorage,
            appInfo = appInfo
        )

    @Provides
    @MobileOkHttpClient
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        localStorage: LocalStorage,
        sessionManager: UserSessionManager,
        networkMonitor: NetworkMonitor,
        offlineRequestDao: OfflineRequestDao,
        @MobileOfflineValidator offlineRequestValidator: OfflineRequestValidator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(BuildConfig.SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .dns(DnsSelector())
            .addInterceptor(VaxCareDataDogInterceptor().createDataDogInterceptor())
            .addInterceptor(VaxHubIdentifier(localStorage, sessionManager, networkMonitor))
            .addInterceptor(MessageSourceInterceptor())
            .addInterceptor(WebLogger(context))
            .addInterceptor(
                StatusCodeHandler(
                    networkMonitor,
                    offlineRequestDao,
                    offlineRequestValidator
                )
            )
            .cache(Cache(context.cacheDir, Long.MAX_VALUE))
            .build()

    @Provides
    @MobileConverterFactory
    @Singleton
    fun providesConverterFactory(
        @MobileMoshi moshi: Moshi
    ): MoshiConverterFactory = MoshiConverterFactory.create(moshi)

    @Provides
    @MobileVaxHubWebServer
    @Singleton
    fun provideRetrofit(
        @MobileConverterFactory moshiConverterFactory: MoshiConverterFactory,
        @MobileOkHttpClient okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.VAX_VHAPI_URL)
            .addConverterFactory(moshiConverterFactory)
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    fun provideWebServer(
        @MobileVaxHubWebServer retrofit: Retrofit
    ): WebServer = retrofit.create(WebServer::class.java)

    @Provides
    @Singleton
    fun providePatientsApi(
        @MobileVaxHubWebServer retrofit: Retrofit
    ): PatientsApi = retrofit.create(PatientsApi::class.java)

    @Provides
    @Singleton
    fun provideInventoryApi(
        @MobileVaxHubWebServer retrofit: Retrofit
    ): InventoryApi = retrofit.create(InventoryApi::class.java)

    @Provides
    @Singleton
    fun providePingApi(
        @MobileVaxHubWebServer retrofit: Retrofit
    ): PingApi = retrofit.create(PingApi::class.java)

    @Provides
    @Singleton
    fun provideOrmApi(
        @MobileVaxHubWebServer retrofit: Retrofit
    ): OrmApi = retrofit.create(OrmApi::class.java)

    @Provides
    @MobileVaxHubLogin
    @Singleton
    fun provideLoginApi(
        @MobileVaxHubWebServer retrofit: Retrofit
    ): LoginApi = retrofit.create(LoginApi::class.java)

    @Provides
    @Singleton
    fun provideApiInfo(): ApiInfo =
        ApiInfoImpl(
            BuildConfig.VAX_VHAPI_URL
        )

    @Provides
    @MobileVaxHubWrongProductApi
    @Singleton
    fun provideWrongProductApi(
        @MobileVaxHubWebServer retrofit: Retrofit
    ): WrongProductApi = retrofit.create(WrongProductApi::class.java)

    @Provides
    @MobilePicasso
    @Singleton
    fun providePicasso(
        @ApplicationContext context: Context,
        @MobileOkHttpClient okHttpClient: OkHttpClient
    ): Picasso =
        Picasso.Builder(context)
            .downloader(OkHttp3Downloader(okHttpClient))
            .build()
}
