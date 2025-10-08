/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import java.time.LocalDateTime

data class AppointmentEligibilityStatus(
    val patientVisitId: Int,
    val partnerId: Int,
    val clinicId: Int,
    val patientDataChangedUtc: LocalDateTime?,
    val appointmentDataChangedUtc: LocalDateTime?,
    val riskInvokedUtc: LocalDateTime?,
    val riskUpdatedUtc: LocalDateTime?,
    val checkoutCompletedUtc: LocalDateTime?,
    val undoCheckoutCompletedUtc: LocalDateTime?,
    val discoveryInvokedUtc: LocalDateTime?,
    val discoveryCompletedUtc: LocalDateTime?,
    val medDInvokedUtc: LocalDateTime?,
    val medDCompletedUtc: LocalDateTime?,
    val medDErrorUtc: LocalDateTime?,
)
