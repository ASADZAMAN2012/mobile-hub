/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.core.model.OrderSyncResults
import com.vaxcare.core.model.Orders
import java.time.Instant
import java.time.LocalDateTime

object MockOrders {
    val upsertedOrders = listOf(
        Orders(
            orderId = 1,
            partnerId = 1,
            clinicId = 1,
            patientVisitId = null,
            patientId = 123,
            isDeleted = null,
            shortDescription = "test-updated-order",
            orderNumber = "order-update123",
            satisfyingProductIds = listOf(1, 2, 3),
            serverSyncDateTimeUtc = Instant.now(),
            durationInDays = 1,
            expirationDate = LocalDateTime.now(),
            orderDate = LocalDateTime.now()
        )
    )
    val deletedOrders = listOf(
        Orders(
            orderId = 2,
            partnerId = 1,
            clinicId = 1,
            patientVisitId = 123,
            patientId = 123,
            isDeleted = true,
            shortDescription = "test-deleted-order",
            orderNumber = "order-deleted123",
            satisfyingProductIds = listOf(4, 5, 6),
            serverSyncDateTimeUtc = Instant.now(),
            durationInDays = 1,
            expirationDate = LocalDateTime.now(),
            orderDate = LocalDateTime.now()
        )
    )
    val results = OrderSyncResults(
        updatedOrders = listOf(),
        upsertedOrders = upsertedOrders,
        deletedOrders = deletedOrders
    )

    val totalOrders = upsertedOrders.count()
    val outstandingOrdersCount =
        (upsertedOrders + deletedOrders).count { it.patientVisitId == null }
}
