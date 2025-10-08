/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class MedDRunCopayCheckClick(patientVisitId: Int) : CheckoutMedDMetric(
    patientVisitId = patientVisitId,
    medDEventName = "RunCopayCheck.Click"
)
