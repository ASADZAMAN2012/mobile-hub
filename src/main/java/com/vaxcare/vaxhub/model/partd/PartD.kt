/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.partd

import com.vaxcare.vaxhub.model.enums.PartDEligibilityStatusCode
import java.math.BigDecimal

interface PartD {
    val patientVisitId: Int?
    val copays: List<PartDCopay>
    val systemHealthStatus: Int?
}

interface PartDCopay {
    val ndc: String?
    val productId: Int?
    val copay: BigDecimal?
    val eligibilityStatusCode: PartDEligibilityStatusCode?
    val requestStatus: Int?
}
