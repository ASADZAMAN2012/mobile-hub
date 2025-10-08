/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout.mock.dispatcher

import android.util.Log
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.mock.model.MockRequest
import com.vaxcare.vaxhub.mock.util.MockMutator
import com.vaxcare.vaxhub.mock.util.MockRequestListener
import com.vaxcare.vaxhub.mock.util.usecase.checkout.CheckoutUseCases
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import java.time.LocalDateTime

abstract class CheckoutDispatcher(
    protected val useCases: CheckoutUseCases,
    protected val clinicId: Long
) : BaseMockDispatcher() {
    protected var checkoutSession = CheckoutSession()

    companion object {
        const val PATIENT_DETAIL_REGEX = "patients/patient/\\d+"
        const val PATIENT_SEARCH_ENDPOINT = "patient/search"
        const val APPOINTMENT_LIST_REGEX = "patients/appointment\\?clinicId.*"
        const val APPOINTMENT_DETAILS_REGEX = "patients/appointment/\\d+\\?version=2\\.0"
        const val MEDD_CHECK_REGEX = "patients/meddcopay/\\d+/check"
    }

    open fun requestListener(): MockRequestListener =
        MockRequestListener { request ->
            Log.d("MOCK", "RequestListener: Parsing request for: ${request.endpoint}")
            checkoutSession = when {
                isCreatePatient(request) -> useCases.getPatientNameAndAppointmentTime(
                    checkoutSession, request.requestBody
                )

                else -> checkoutSession
            }
        }

    open fun responseMutator(): MockMutator =
        MockMutator { request, responseBody ->
            Log.d("MOCK", "Mutator: Parsing response for request: ${request.endpoint}")
            when {
                request.endpoint.contains(PATIENT_DETAIL_REGEX.toRegex()) -> {
                    checkoutSession =
                        useCases.changePatientDetail(checkoutSession, responseBody)
                    checkoutSession.patientDetailPayload
                }

                request.endpoint.contains(PATIENT_SEARCH_ENDPOINT) -> {
                    checkoutSession =
                        useCases.changePatientSearch(checkoutSession, responseBody)
                    checkoutSession.patientSearchPayload
                }

                isPostAppointment(request) -> {
                    val appointmentId = responseBody?.toInt()
                    checkoutSession.appointmentId = appointmentId
                    useCases.sendACE(
                        clinicId = clinicId,
                        appointmentTime = checkoutSession.appointmentDateTime
                            ?: LocalDateTime.now(),
                        changeReason = AppointmentChangeReason.RiskUpdated,
                        appointmentId = appointmentId ?: 0
                    )
                    responseBody
                }

                request.endpoint.contains(APPOINTMENT_LIST_REGEX.toRegex()) -> {
                    checkoutSession =
                        useCases.changeAppointmentList(checkoutSession, responseBody)
                    checkoutSession.appointmentListPayload
                }

                request.endpoint.contains(APPOINTMENT_DETAILS_REGEX.toRegex()) &&
                    request.requestMethod.equals("GET", true) -> {
                    checkoutSession =
                        useCases.changeAppointment(checkoutSession, responseBody)
                    checkoutSession.appointmentDetailPayload
                }

                isPostMedDCheck(request) -> {
                    Log.d("MOCK", "sending ACE for request: ${request.endpoint}")
                    useCases.sendACE(
                        clinicId = clinicId,
                        appointmentTime = checkoutSession.appointmentDateTime
                            ?: LocalDateTime.now(),
                        changeReason = AppointmentChangeReason.MedDCompleted,
                        appointmentId = checkoutSession.appointmentId ?: 0
                    )
                    responseBody
                }

                else -> {
                    Log.d("MOCK", "returning unmutated response")
                    responseBody
                }
            }
        }

    protected fun isCreatePatient(request: MockRequest): Boolean =
        isPostAppointment(request) &&
            request.requestBody?.peek()?.readUtf8()?.contains("newPatient") == true

    protected fun isPostAppointment(request: MockRequest): Boolean =
        request.endpoint.contains("patients/appointment") &&
            !request.endpoint.contains("noCheckoutReason") &&
            request.requestMethod.equals("POST", true)

    protected fun isPostMedDCheck(request: MockRequest): Boolean =
        request.endpoint.contains(MEDD_CHECK_REGEX.toRegex()) &&
            request.requestMethod.equals("POST", true)
}
