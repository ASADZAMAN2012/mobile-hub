/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/
package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
sealed class CallToAction(val id: Int) {
    object None : CallToAction(0)

    /*
     * MissingPatientData can mean either Missing Demographics OR Missing Payer info
     * if there is also missing demographics. This is why it is used for both cases
     */
    object MissingPatientData : CallToAction(1)

    object PatientResponsibility : CallToAction(2)

    object MissingOrInvalidSsn : CallToAction(3)

    object MissingOrInvalidGender : CallToAction(4)

    object MissingOrInvalidDoB : CallToAction(5)

    object MissingOrInvalidPayerName : CallToAction(6)

    object MissingOrInvalidGroupId : CallToAction(7)

    object MissingOrInvalidMemberId : CallToAction(8)

    object MedDCanRun : CallToAction(9)

    object MedDDidRun : CallToAction(10)

    object MissingOrInvalidMbi : CallToAction(11)

    object MedDCollectCreditCard : CallToAction(12)

    object MedDCollectSignature : CallToAction(13)

    object PatientAndInsuranceData : CallToAction(14)

    companion object {
        val ctaMedDCompleted = listOf(
            MedDDidRun,
            MedDCollectCreditCard,
            MedDCollectSignature,
            None
        )

        val ctaMissingInfo = listOf(
            MissingOrInvalidPayerName,
            MissingOrInvalidGroupId,
            MissingOrInvalidMemberId,
            MissingOrInvalidMbi,
            MissingOrInvalidSsn,
            MissingOrInvalidGender,
            MissingOrInvalidDoB,
            MissingPatientData,
            PatientAndInsuranceData
        )

        val ctaMissingDemoInfo = listOf(
            MissingOrInvalidSsn,
            MissingOrInvalidGender,
            MissingOrInvalidDoB,
            MissingPatientData,
            PatientAndInsuranceData
        )

        val ctaMissingPayerInfo = listOf(
            MissingOrInvalidPayerName,
            MissingOrInvalidGroupId,
            MissingOrInvalidMemberId,
            MissingOrInvalidMbi,
            PatientAndInsuranceData
        )

        fun fromInt(id: Int) =
            CallToAction::class.sealedSubclasses
                .mapNotNull { it.objectInstance }
                .firstOrNull { it.id == id }
                .let {
                    when (it) {
                        null -> None
                        else -> it
                    }
                }
    }
}
