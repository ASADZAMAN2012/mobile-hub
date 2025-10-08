/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.model

import java.time.LocalDateTime

data class CheckoutSession(
    var patientFirstName: String? = null,
    var patientLastName: String? = null,
    var appointmentId: Int? = 0,
    var appointmentDateTime: LocalDateTime? = LocalDateTime.now(),
    var appointmentDetailPayload: String? = null,
    var appointmentListPayload: String? = null,
    var patientDetailPayload: String? = null,
    var patientSearchPayload: String? = null
)
