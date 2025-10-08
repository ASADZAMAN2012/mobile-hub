/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.AppointmentRepositoryImpl
import com.vaxcare.vaxhub.repository.ClinicRepository
import com.vaxcare.vaxhub.repository.ClinicRepositoryImpl
import com.vaxcare.vaxhub.repository.FirewallCheckRepository
import com.vaxcare.vaxhub.repository.FirewallCheckRepositoryImpl
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.LocationRepositoryImpl
import com.vaxcare.vaxhub.repository.LotNumbersRepository
import com.vaxcare.vaxhub.repository.LotNumbersRepositoryImpl
import com.vaxcare.vaxhub.repository.OrdersRepository
import com.vaxcare.vaxhub.repository.OrdersRepositoryImpl
import com.vaxcare.vaxhub.repository.PatientRepository
import com.vaxcare.vaxhub.repository.PatientRepositoryImpl
import com.vaxcare.vaxhub.repository.PayerRepository
import com.vaxcare.vaxhub.repository.PayerRepositoryImpl
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.ProductRepositoryImpl
import com.vaxcare.vaxhub.repository.ProviderRepository
import com.vaxcare.vaxhub.repository.ProviderRepositoryImpl
import com.vaxcare.vaxhub.repository.SessionUserRepository
import com.vaxcare.vaxhub.repository.SessionUserRepositoryImpl
import com.vaxcare.vaxhub.repository.ShotAdministratorRepository
import com.vaxcare.vaxhub.repository.ShotAdministratorRepositoryImpl
import com.vaxcare.vaxhub.repository.SimpleOnHandInventoryRepository
import com.vaxcare.vaxhub.repository.SimpleOnHandInventoryRepositoryImpl
import com.vaxcare.vaxhub.repository.UpdateRepository
import com.vaxcare.vaxhub.repository.UpdateRepositoryImpl
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.repository.UserRepositoryImpl
import com.vaxcare.vaxhub.repository.WrongProductRepository
import com.vaxcare.vaxhub.repository.WrongProductRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(appointmentRepository: AppointmentRepositoryImpl): AppointmentRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(userRepository: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(locationRepository: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindClinicRepository(clinicRepository: ClinicRepositoryImpl): ClinicRepository

    @Binds
    @Singleton
    abstract fun bindPatientRepository(patientRepository: PatientRepositoryImpl): PatientRepository

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(ordersRepository: OrdersRepositoryImpl): OrdersRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(productRepository: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindPayerRepository(payerRepository: PayerRepositoryImpl): PayerRepository

    @Binds
    @Singleton
    abstract fun bindShotAdministratorRepository(
        shotAdministratorRepository: ShotAdministratorRepositoryImpl
    ): ShotAdministratorRepository

    @Binds
    @Singleton
    abstract fun bindLotNumbersRepository(lotNumbersRepository: LotNumbersRepositoryImpl): LotNumbersRepository

    @Binds
    @Singleton
    abstract fun bindSimpleOnHandInventoryRepository(
        simpleOnHandInventoryRepository: SimpleOnHandInventoryRepositoryImpl
    ): SimpleOnHandInventoryRepository

    @Binds
    @Singleton
    abstract fun bindProviderRepository(providerRepository: ProviderRepositoryImpl): ProviderRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(updateRepository: UpdateRepositoryImpl): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindSessionUserRepository(sessionUserRepositoryImpl: SessionUserRepositoryImpl): SessionUserRepository

    @Binds
    @Singleton
    abstract fun bindWrongProductRepository(wrongProductRepository: WrongProductRepositoryImpl): WrongProductRepository

    @Binds
    @Singleton
    abstract fun bindFirewallCheckRepository(
        firewallCheckRepository: FirewallCheckRepositoryImpl
    ): FirewallCheckRepository
}
