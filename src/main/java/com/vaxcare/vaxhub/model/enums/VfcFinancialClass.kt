/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

enum class VfcFinancialClass(val description: String) {
    V02(
        description = "Enrolled in Medicaid"
    ),
    V03(
        description = "Uninsured"
    ),
    V04(
        description = "American Indian or Alaska Native"
    ),
    V05(
        description = "Underinsured"
    ),
    V08(
        description = "Private Underinsured"
    )
}
