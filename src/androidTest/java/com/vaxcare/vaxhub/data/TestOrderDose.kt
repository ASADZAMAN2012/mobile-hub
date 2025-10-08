/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

sealed class TestOrderDose(
    val orderId: Int?,
    val patientVisitId: Int?,
    val isDeleted: Boolean?,
    val shortDescription: String,
    val orderNumber: String,
    val satisfyingProductIds: List<Int>,
    // This might need to be Date instead of LocalDate
    val serverSyncDateTimeUtc: Instant,
    val durationInDays: Int,
    val expirationDate: LocalDateTime,
    var orderDate: LocalDateTime,
    val icon: Int
) {
    object Varicella : TestOrderDose(
        (0 until 99999).random(),
        null,
        false,
        "Varicella",
        IntegrationUtil.getRandomString(11),
        listOf(13, 14),
        Instant.now().minus(1, ChronoUnit.DAYS),
        15,
        LocalDateTime.now().plusDays(1),
        LocalDateTime.now().minusDays(1),
        R.drawable.ic_presentation_single_dose_vial
    )

    object HibPrpT : TestOrderDose(
        (0 until 99999).random(),
        null,
        false,
        "Hib (PRP-T)",
        IntegrationUtil.getRandomString(11),
        listOf(3, 62, 159, 176, 183),
        Instant.now().minus(1, ChronoUnit.DAYS),
        15,
        LocalDateTime.now().plusDays(1),
        LocalDateTime.now().minusDays(1),
        R.drawable.ic_presentation_single_dose_vial
    )
}
