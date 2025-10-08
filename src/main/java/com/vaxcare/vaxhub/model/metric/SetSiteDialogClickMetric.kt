/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.inventory.Site

class SetSiteDialogClickMetric(
    patientVisitId: Int,
    private val productName: String,
    private val lotNumber: String,
    private val siteValue: Site.SiteValue,
) : CheckoutMetric(patientVisitId, "SetSiteDialog.Click") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("productName", productName)
            put("lotNumber", lotNumber)
            put("siteSelectedAbbreviation", siteValue.abbreviation)
        }
    }
}
