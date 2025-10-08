/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.model.appointment.PhoneContactConsentStatus
import com.vaxcare.vaxhub.model.enums.RiskFactor
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class AppointmentCheckout(
    val tabletId: String,
    val administeredVaccines: List<CheckInVaccination>,
    val administered: LocalDateTime,
    val administeredBy: Int,
    val presentedRiskAssessmentId: Int?,
    val forcedRiskType: Int,
    val postShotVisitPaymentModeDisplayed: PaymentMode,
    val phoneNumberFlowPresented: Boolean,
    val phoneContactConsentStatus: PhoneContactConsentStatus,
    val phoneContactReasons: String,
    val flags: List<String>,
    val pregnancyPrompt: Boolean,
    val weeksPregnant: Int?,
    val creditCardInformation: PaymentInformationRequestBody?,
    val activeFeatureFlags: List<String>,
    val attestHighRisk: Boolean,
    val riskFactors: List<RiskFactor>
)
