/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util.usecase.checkout

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckoutUseCases @Inject constructor(
    val getPatientNameAndAppointmentTime: ExtractPatientNameAndAppointmentTimeUseCase,
    val changePatientSearch: MutatePatientSearchUseCase,
    val changePatientDetail: MutatePatientDetailUseCase,
    val changeAppointmentList: MutateFirstAppointmentListUseCase,
    val changeAppointment: MutateAppointmentUseCase,
    val sendACE: SendAppointmentChangedEventUseCase
)
