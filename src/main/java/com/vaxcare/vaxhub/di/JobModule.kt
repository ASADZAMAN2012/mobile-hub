/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import com.vaxcare.core.worker.JobExecutor
import com.vaxcare.core.worker.JobExecutorImpl
import com.vaxcare.vaxhub.worker.jobs.FireWallCheckJob
import com.vaxcare.vaxhub.worker.jobs.VaxJobCallback
import com.vaxcare.vaxhub.worker.jobs.VaxJobCallbackImpl
import com.vaxcare.vaxhub.worker.jobs.appointment.AppointmentJob
import com.vaxcare.vaxhub.worker.jobs.appointment.PayerJob
import com.vaxcare.vaxhub.worker.jobs.appointment.ShotAdministratorJob
import com.vaxcare.vaxhub.worker.jobs.hub.ClinicSyncJob
import com.vaxcare.vaxhub.worker.jobs.hub.ConfigJob
import com.vaxcare.vaxhub.worker.jobs.hub.LocationJob
import com.vaxcare.vaxhub.worker.jobs.hub.OfflineRequestJob
import com.vaxcare.vaxhub.worker.jobs.hub.ProviderJob
import com.vaxcare.vaxhub.worker.jobs.hub.SessionCacheCleanupJob
import com.vaxcare.vaxhub.worker.jobs.hub.UserJob
import com.vaxcare.vaxhub.worker.jobs.inventory.LotNumbersJob
import com.vaxcare.vaxhub.worker.jobs.inventory.ProductJob
import com.vaxcare.vaxhub.worker.jobs.inventory.SimpleOnHandInventoryJob
import com.vaxcare.vaxhub.worker.jobs.ndc.WrongProductNdcJob
import com.vaxcare.vaxhub.worker.jobs.order.OrderCleanupJob
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DailyJobs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ThreeHourJobs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HalfHourJobs

@Module
@InstallIn(SingletonComponent::class)
object JobModule {
    @Provides
    @Singleton
    fun provideVaxJobCallback(): VaxJobCallback = VaxJobCallbackImpl()

    // VaxJob Executors
    @Provides
    @DailyJobs
    @Singleton
    fun provideDailyJobs(
        clinicSyncJob: ClinicSyncJob,
        configJob: ConfigJob,
        fireWallCheckJob: FireWallCheckJob,
        locationJob: LocationJob,
        orderCleanupJob: OrderCleanupJob,
        payerJob: PayerJob,
        productJob: ProductJob,
        providerJob: ProviderJob,
        sessionCacheCleanupJob: SessionCacheCleanupJob,
        shotAdministratorJob: ShotAdministratorJob,
        simpleOnHandInventoryJob: SimpleOnHandInventoryJob,
        userJob: UserJob,
        wrongProductNdcJob: WrongProductNdcJob
    ): JobExecutor =
        JobExecutorImpl(
            listOf(
                clinicSyncJob,
                configJob,
                fireWallCheckJob,
                locationJob,
                orderCleanupJob,
                payerJob,
                productJob,
                providerJob,
                sessionCacheCleanupJob,
                shotAdministratorJob,
                simpleOnHandInventoryJob,
                userJob,
                wrongProductNdcJob
            )
        )

    @Provides
    @ThreeHourJobs
    @Singleton
    fun provideThreeHourJobs(lotNumbersJob: LotNumbersJob, appointmentJob: AppointmentJob): JobExecutor =
        JobExecutorImpl(
            listOf(
                lotNumbersJob,
                appointmentJob
            )
        )

    @Provides
    @HalfHourJobs
    @Singleton
    fun provideHalfHourJobs(offlineRequestJob: OfflineRequestJob): JobExecutor =
        JobExecutorImpl(
            listOf(
                offlineRequestJob
            )
        )
}
