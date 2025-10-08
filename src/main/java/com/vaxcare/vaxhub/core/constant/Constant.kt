/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.constant

object Constant {
    const val DELAY_MIDDLE = 500L
    const val COLLECT_PHONE_DATA_FRAGMENT_TAG = "CheckoutCollectPayerFragment"
}

enum class ResponseCodes(val value: Int) {
    OK(200),
    BAD_CREDENTIALS(401),
    NOT_FOUND(404),
    ACCOUNT_LOCKED(423)
}

object Receivers {
    const val RESCHEDULE_EXTRA = "RESCHEDULE"

    // Session Clean Receiver constants
    const val SESSION_CLEAN_REQUEST_CODE = 10002
    const val SESSION_CLEAN_ACTION = "SESSION_CLEAN_ACTION"

    // AppointmentChangedEvent Receiver
    const val ACE_ACTION = "com.vaxcare.mobilehub.ace"
    const val PART_D_ACTION = "com.vaxcare.mobilehub.partd"
    const val ACE_APPOINTMENT_ID = "appointmentId"
    const val ACE_CHANGE_REASON = "changeReason"
    const val ACE_CHANGE_TYPE = "changeType"
    const val PART_D_PATIENT_VISIT_ID = "patientVisitId"
    const val PART_D_COPAYS = "copays"

    // PlayCore request code
    const val UPDATE_REQUEST_CODE = 10003
    const val IN_APP_UPDATE_ACTION = "com.vaxcare.mobilehub.update"
}

/**
 * Events from the backend that will trigger VaxJobs to run from the JobSelector
 */
object FirebaseEventTypes {
    /**
     * Fires off the LocationJob
     */
    const val SYNC_LOCATION = "VaxHub.FirebaseEvents.SyncLocation"

    /**
     * Fires off the DiagnosticJob
     */
    const val SYNC_DIAGNOSTIC = "com.vaxcare.vaxhub.firebase.DIAGNOSTIC"

    /**
     * Fires off the OrderGroupChangedJob
     *
     * Requires a payload with OrderGroupChangedEvent json string
     */
    const val SYNC_GROUP_ORM = "VaxCare.Scheduler.Partner.Clinic.OrderGroupChangedEvent"

    /**
     * Fires off the AppointmentChangedJob
     *
     * Requires a payload with AppointmentChangedEvent json string
     */
    const val SYNC_APPOINTMENT =
        "VaxCare.Scheduler.Partner.Clinic.AppointmentChangedEvent"

    /**
     * Fires off the SimpleOnHandInventoryJob
     */
    const val SIMPLE_ON_HAND =
        "VaxApi.Cqrs.Command.CreateAdjustmentsCommandHandler+AdjustmentVaxHubEvent"

    const val COPAY_RESULTS =
        "VaxCare.PartD.Contracts.PartDCopayResultsEvent"
}

/**
 * Antigens that require special accommodations
 */
object AntigensWithSpecialAccommodations {
    // Seasonal Antigens
    const val FLU = "Influenza"
    const val COVID = "COVID"
    const val COVID_19 = "COVID-19"

    // Route Selection Antigens
    const val IPOL = "IPV"
    const val PNEUMOVAX23 = "PPSV23"
    const val VARIVAX = "Varicella"
    const val PRO_QUAD = "MMRV"
    const val MMR_II = "MMR"
}

object RejectCodes {
    /**
     * Risk Reject Codes specific for Med D or missing information for InsurancePay
     */
    val riskRejectCodesForMissingInfoOrMedD = listOf(
        "1019",
        "1113",
        "1114",
        "1001"
    )
}

object RegexPatterns {
    const val EMAIL_RFC_5322 = "^[A-Za-z0-9.'\\\" _%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\$"
}
