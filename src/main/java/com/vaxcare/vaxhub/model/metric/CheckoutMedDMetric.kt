/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

abstract class CheckoutMedDMetric(
    override var patientVisitId: Int?,
    medDEventName: String
) : CheckoutMetric(patientVisitId = patientVisitId, checkoutEventName = "MedD.$medDEventName")
