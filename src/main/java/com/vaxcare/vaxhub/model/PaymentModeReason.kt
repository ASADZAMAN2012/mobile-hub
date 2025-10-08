/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/
package com.vaxcare.vaxhub.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = false)
sealed class PaymentModeReason(val id: Int) : Parcelable {
    @Parcelize
    object Unknown : PaymentModeReason(0)

    @Parcelize
    object PartnerOverride : PaymentModeReason(1)

    @Parcelize
    object EmrRules : PaymentModeReason(2)

    @Parcelize
    object ManualOverride : PaymentModeReason(3)

    @Parcelize
    object GlobalInsuranceMapping : PaymentModeReason(4)

    @Parcelize
    object SchedulerCreated : PaymentModeReason(5)

    @Parcelize
    object OutOfAgeIndication : PaymentModeReason(6)

    @Parcelize
    object ImmunizationsNotCovered : PaymentModeReason(7)

    @Parcelize
    object SelfPayOptOut : PaymentModeReason(8)

    @Parcelize
    object RequestedMediaNotProvided : PaymentModeReason(9)

    @Parcelize
    object RiskFlip : PaymentModeReason(10)

    @Parcelize
    object InvalidStockSelected : PaymentModeReason(11)

    @Parcelize
    object DecisionatorFlip : PaymentModeReason(12)

    @Parcelize
    object RequestedPhoneNumberNotProvided : PaymentModeReason(13)

    @Parcelize
    object SourcedFromPreviousVisit : PaymentModeReason(14)
}
