/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di

import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.ui.navigation.AddAppointmentDestination
import com.vaxcare.vaxhub.ui.navigation.AddAppointmentDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.AddPatientsDestination
import com.vaxcare.vaxhub.ui.navigation.AddPatientsDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.AdminDestination
import com.vaxcare.vaxhub.ui.navigation.AdminDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.AppointmentDestination
import com.vaxcare.vaxhub.ui.navigation.AppointmentDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.AppointmentSearchDestination
import com.vaxcare.vaxhub.ui.navigation.AppointmentSearchDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestination
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.CheckoutCollectInfoDestination
import com.vaxcare.vaxhub.ui.navigation.CheckoutCollectInfoDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.CheckoutPatientDestination
import com.vaxcare.vaxhub.ui.navigation.CheckoutPatientDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestination
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.CreateAppointmentDestination
import com.vaxcare.vaxhub.ui.navigation.CreateAppointmentDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.CurbsideConfirmPatientInfoDestination
import com.vaxcare.vaxhub.ui.navigation.CurbsideConfirmPatientInfoDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.DoseReasonDestination
import com.vaxcare.vaxhub.ui.navigation.DoseReasonDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinationsImpl
import com.vaxcare.vaxhub.ui.navigation.LoginDestination
import com.vaxcare.vaxhub.ui.navigation.LoginDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.LotDestination
import com.vaxcare.vaxhub.ui.navigation.LotDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.MainDestination
import com.vaxcare.vaxhub.ui.navigation.MainDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.MedDCheckoutDestination
import com.vaxcare.vaxhub.ui.navigation.MedDCheckoutDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.NavCommons
import com.vaxcare.vaxhub.ui.navigation.NavCommonsImpl
import com.vaxcare.vaxhub.ui.navigation.OutOfDateDestination
import com.vaxcare.vaxhub.ui.navigation.OutOfDateDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.PatientDestination
import com.vaxcare.vaxhub.ui.navigation.PatientDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.PatientEditDestination
import com.vaxcare.vaxhub.ui.navigation.PatientEditDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.PhoneCollectDestination
import com.vaxcare.vaxhub.ui.navigation.PhoneCollectDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.PinLockDestination
import com.vaxcare.vaxhub.ui.navigation.PinLockDestinationImpl
import com.vaxcare.vaxhub.ui.navigation.SplashDestination
import com.vaxcare.vaxhub.ui.navigation.SplashDestinationImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {
    @Provides
    @Singleton
    fun providesNavCommons(): NavCommons = NavCommonsImpl(R.id.nav_host)

    @Provides
    @Singleton
    fun providesGlobalDestinations(navCommons: NavCommons): GlobalDestinations = GlobalDestinationsImpl(navCommons)

    @Provides
    @Singleton
    fun providesPhoneCollectDestination(navCommons: NavCommons): PhoneCollectDestination =
        PhoneCollectDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesMedDCheckoutDestination(navCommons: NavCommons): MedDCheckoutDestination =
        MedDCheckoutDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesCheckoutPatientDestination(navCommons: NavCommons): CheckoutPatientDestination =
        CheckoutPatientDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesDoseReasonDestination(navCommons: NavCommons): DoseReasonDestination =
        DoseReasonDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesAdminDestination(navCommons: NavCommons): AdminDestination = AdminDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesMainDestination(navCommons: NavCommons): MainDestination = MainDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesAppointmentDestination(navCommons: NavCommons): AppointmentDestination =
        AppointmentDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesAppointmentSearchDestination(navCommons: NavCommons): AppointmentSearchDestination =
        AppointmentSearchDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesAddAppointmentDestination(navCommons: NavCommons): AddAppointmentDestination =
        AddAppointmentDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesConfirmPatientInfoDestination(navCommons: NavCommons): CurbsideConfirmPatientInfoDestination =
        CurbsideConfirmPatientInfoDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesCreateAppointmentDestination(navCommons: NavCommons): CreateAppointmentDestination =
        CreateAppointmentDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesPinLockDestination(navCommons: NavCommons): PinLockDestination = PinLockDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesPatientDestination(navCommons: NavCommons): PatientDestination = PatientDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesSplashDestination(navCommons: NavCommons): SplashDestination = SplashDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesLotDestination(navCommons: NavCommons): LotDestination = LotDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesCheckoutSummaryDestination(navCommons: NavCommons): CheckoutSummaryDestination =
        CheckoutSummaryDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesCaptureFlowDestination(navCommons: NavCommons): CaptureFlowDestination =
        CaptureFlowDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesCheckoutCollectInfoDestination(navCommons: NavCommons): CheckoutCollectInfoDestination =
        CheckoutCollectInfoDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesPatientEditDestination(navCommons: NavCommons): PatientEditDestination =
        PatientEditDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesLoginDestination(navCommons: NavCommons): LoginDestination = LoginDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesAddPatientsDestination(navCommons: NavCommons): AddPatientsDestination =
        AddPatientsDestinationImpl(navCommons)

    @Provides
    @Singleton
    fun providesOutOfDateDestination(navCommons: NavCommons): OutOfDateDestination =
        OutOfDateDestinationImpl(navCommons)
}
