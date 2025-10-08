/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

/**
 * InsuranceID enum specifically assigned for mobile created patients
 *
 * @property value - associated primaryInsuranceId values signify CURBSIDE
 */
enum class CurbsideInsuranceID(val value: Int) {
    INSURANCE_CARD_PROVIDED(1000023696),
    INSURANCE_CARD_NOT_PROVIDED(1000023697),
    OTHER_PAYER(1000023698),
    SELF_PAY(1000023699)
}
