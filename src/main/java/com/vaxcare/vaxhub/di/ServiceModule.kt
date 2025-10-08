/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import com.vaxcare.core.config.VaxCareConfig
import com.vaxcare.vaxhub.config.VaxCareConfigImpl
import com.vaxcare.vaxhub.service.PartDService
import com.vaxcare.vaxhub.service.PartDServiceImpl
import com.vaxcare.vaxhub.service.SessionCleanService
import com.vaxcare.vaxhub.service.SessionCleanServiceImpl
import com.vaxcare.vaxhub.service.UserSessionService
import com.vaxcare.vaxhub.service.UserSessionServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindVaxCareConfig(vaxCareConfigImpl: VaxCareConfigImpl): VaxCareConfig

    @Binds
    @Singleton
    abstract fun bindSessionCleanService(sessionCleanServiceImpl: SessionCleanServiceImpl): SessionCleanService

    @Binds
    @Singleton
    abstract fun bindUserSessionService(userSessionServiceImpl: UserSessionServiceImpl): UserSessionService

    @Binds
    @Singleton
    abstract fun bindPartDService(partDServiceImpl: PartDServiceImpl): PartDService
}
