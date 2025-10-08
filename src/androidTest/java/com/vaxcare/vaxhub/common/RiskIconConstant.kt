/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import com.vaxcare.vaxhub.R

sealed class RiskIconConstant(
    val resourceId: Int,
    val isInstrumentation: Boolean,
    val tintColorResId: Int? = null
) {
    object RiskFreeIcon : RiskIconConstant(
        resourceId = R.drawable.ic_vax3_eligibility_guaranteed_ic,
        isInstrumentation = false,
        tintColorResId = R.color.primary_purple
    )

    object SelfPayIcon : RiskIconConstant(
        resourceId = R.drawable.ic_vax3_eligibility_self_pay,
        isInstrumentation = false,
        tintColorResId = R.color.primary_coral
    )

    data class PartnerBillIcon(private val instanceTintId: Int = R.color.primary_purple) :
        RiskIconConstant(R.drawable.ic_oval_blue, false, instanceTintId)

    object VFCPartnerBillIcon : RiskIconConstant(
        resourceId = R.drawable.ic_vax3_eligibility_partner_resp_ic,
        isInstrumentation = false,
        tintColorResId = R.color.button_primary_green
    )

    object PrefilledSyringe : RiskIconConstant(
        resourceId = R.drawable.ic_presentation_prefilled_syringe,
        isInstrumentation = false
    )

    object InReviewIcon : RiskIconConstant(
        resourceId = R.drawable.ic_compensation_inreview,
        isInstrumentation = false,
        tintColorResId = R.color.primary_purple
    )

    object MissingInfoIcon : RiskIconConstant(
        resourceId = R.drawable.ic_vax3_eligibility_issue_ic,
        isInstrumentation = false,
        tintColorResId = R.color.primary_yellow
    )
}
