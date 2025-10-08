/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.model.CurbsideInsuranceID
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.enums.PaymentMethod

object MockPaymentInformation {
    val withCurbsideInsurance = PaymentInformation(
        id = 1,
        insuranceName = "Cigna",
        primaryInsuranceId = CurbsideInsuranceID.INSURANCE_CARD_PROVIDED.value,
        primaryInsurancePlanId = null,
        primaryMemberId = "SDF",
        primaryGroupId = null,
        uninsured = false,
        paymentMode = PaymentMethod.InsurancePay,
        vfcFinancialClass = null,
        insuredFirstName = "MEDD",
        insuredLastName = "ELIGIBLE",
        insuredDob = "1940-03-07T00:00:00",
        insuredGender = "Male",
        appointmentId = 72600942,
        relationshipToInsured = RelationshipToInsured.Self,
        portalInsuranceMappingId = null,
        mbi = null
    )
}
