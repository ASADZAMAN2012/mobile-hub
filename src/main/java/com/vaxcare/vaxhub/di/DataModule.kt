/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import android.content.Context
import com.vaxcare.core.secret.SecretProvider
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.data.AppDatabase
import com.vaxcare.vaxhub.data.dao.AppointmentDao
import com.vaxcare.vaxhub.data.dao.ClinicDao
import com.vaxcare.vaxhub.data.dao.FeatureFlagDao
import com.vaxcare.vaxhub.data.dao.LocationDao
import com.vaxcare.vaxhub.data.dao.LotInventoryDao
import com.vaxcare.vaxhub.data.dao.LotNumberDao
import com.vaxcare.vaxhub.data.dao.OfflineRequestDao
import com.vaxcare.vaxhub.data.dao.OrderDao
import com.vaxcare.vaxhub.data.dao.PayerDao
import com.vaxcare.vaxhub.data.dao.ProductDao
import com.vaxcare.vaxhub.data.dao.ProviderDao
import com.vaxcare.vaxhub.data.dao.SessionDao
import com.vaxcare.vaxhub.data.dao.ShotAdministratorDao
import com.vaxcare.vaxhub.data.dao.SimpleOnHandInventoryDao
import com.vaxcare.vaxhub.data.dao.UserDao
import com.vaxcare.vaxhub.data.dao.WrongProductNdcDao
import com.vaxcare.vaxhub.data.dao.legacy.LegacyCountDao
import com.vaxcare.vaxhub.data.dao.legacy.LegacyLotInventoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        secretProvider: SecretProvider,
        localStorage: LocalStorage
    ): AppDatabase =
        AppDatabase
            .initialize(
                context = context,
                secretProvider = secretProvider,
                localStorage = localStorage
            )

    @Provides
    @Singleton
    fun provideLotInventoryDao(appDatabase: AppDatabase): LotInventoryDao = appDatabase.lotInventoryDao()

    @Provides
    @Singleton
    fun provideAppointmentDao(appDatabase: AppDatabase): AppointmentDao = appDatabase.appointmentDao()

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao = appDatabase.userDao()

    @Provides
    @Singleton
    fun provideLocationDao(appDatabase: AppDatabase): LocationDao = appDatabase.locationDao()

    @Provides
    @Singleton
    fun provideFeatureFlagDao(appDatabase: AppDatabase): FeatureFlagDao = appDatabase.featureFlagDao()

    @Provides
    @Singleton
    fun provideProductDao(appDatabase: AppDatabase): ProductDao = appDatabase.productDao()

    @Provides
    @Singleton
    fun provideProviderDao(appDatabase: AppDatabase): ProviderDao = appDatabase.providerDao()

    @Provides
    @Singleton
    fun provideShotAdministratorDao(appDatabase: AppDatabase): ShotAdministratorDao = appDatabase.shotAdministratorDao()

    @Provides
    @Singleton
    fun provideLotNumberDao(appDatabase: AppDatabase): LotNumberDao = appDatabase.lotNumberDao()

    @Provides
    @Singleton
    fun provideSimpleOnHandInventoryDao(appDatabase: AppDatabase): SimpleOnHandInventoryDao =
        appDatabase.simpleOnHandInventoryDao()

    @Provides
    @Singleton
    fun provideLegacyLotInventoryDao(appDatabase: AppDatabase): LegacyLotInventoryDao =
        appDatabase.legacyLotInventoryDao()

    @Provides
    @Singleton
    fun provideLegacyCountDao(appDatabase: AppDatabase): LegacyCountDao = appDatabase.legacyCountDao()

    @Provides
    @Singleton
    fun provideOfflineRequestDao(appDatabase: AppDatabase): OfflineRequestDao = appDatabase.offlineRequestDao()

    @Provides
    @Singleton
    fun providePayerDao(appDatabase: AppDatabase): PayerDao = appDatabase.payerDao()

    @Provides
    @Singleton
    fun provideClinicDao(appDatabase: AppDatabase): ClinicDao = appDatabase.clinicDao()

    @Provides
    @Singleton
    fun provideOrderDao(appDatabase: AppDatabase): OrderDao = appDatabase.orderDao()

    @Provides
    @Singleton
    fun provideSessionDao(appDatabase: AppDatabase): SessionDao = appDatabase.sessionDao()

    @Provides
    @Singleton
    fun provideWrongProductNdcDao(appDatabase: AppDatabase): WrongProductNdcDao = appDatabase.wrongProductNdcDao()
}
