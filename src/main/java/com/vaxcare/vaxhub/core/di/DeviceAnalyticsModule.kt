/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.di

import com.vaxcare.core.report.analytics.AnalyticsTarget
import com.vaxcare.core.report.di.DataDog
import com.vaxcare.vaxhub.core.AnalyticsTargetDataDog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceAnalyticsModule {
    @Binds
    @DataDog
    abstract fun providesDataDogTarget(analyticsTargetDataDog: AnalyticsTargetDataDog): AnalyticsTarget
}
