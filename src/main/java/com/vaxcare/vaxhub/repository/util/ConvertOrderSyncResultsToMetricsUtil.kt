/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository.util

import com.microsoft.appcenter.crashes.utils.ErrorLogHelper
import com.vaxcare.core.model.OrderSyncResults
import com.vaxcare.core.model.Orders
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.OrderAckMetric
import com.vaxcare.core.report.model.OrderDeleteMetric
import javax.inject.Inject

/**
 * UseCase for extracting OrderAckMetrics and OrderDeleteMetrics from an OrderSyncResults
 */
class ConvertOrderSyncResultsToMetricsUtil @Inject constructor() {
    /**
     * @param results The given OrderSyncResults object
     * @param context The context to send up with each metric
     * @return Extracted combined list of OrderAckMetrics and OrderDeleteMetrics
     */
    fun convertOrderSyncResults(results: OrderSyncResults, context: String): MutableList<BaseMetric> {
        val upsertedOrders = extractOrderAcks(results.upsertedOrders, context, false)
        val updatedOrders = extractOrderAcks(results.updatedOrders, context, true)
        val deletedOrders = extractDeletes(results.deletedOrders, context)
        return (upsertedOrders + updatedOrders + deletedOrders).toMutableList()
    }

    private fun extractOrderAcks(
        orders: List<Orders>,
        context: String,
        deleted: Boolean
    ) = orders.map { order ->
        val totalOrders = orders.count { it.patientId == order.patientId }
        val outstandingOrders =
            orders.count { it.patientId == order.patientId && it.patientVisitId == null }
        val products = order.satisfyingProductIds.joinToString(",")
        OrderAckMetric(
            patientId = order.patientId,
            shortDescription = order.shortDescription,
            productIds = products.substring(
                0..Integer.min(
                    products.length - 1,
                    ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH
                )
            ),
            placerOrderNumber = order.orderNumber,
            patientVisitId = order.patientVisitId,
            patientOrderCountTotal = totalOrders,
            patientOrderCountOutstanding = outstandingOrders,
            context = context,
            deleted = deleted
        )
    }

    private fun extractDeletes(orders: List<Orders>, context: String) =
        orders.map { order ->
            val products = order.satisfyingProductIds.joinToString(",")
            OrderDeleteMetric(
                patientId = order.patientId,
                shortDescription = order.shortDescription,
                productIds = products.substring(
                    0..Integer.min(
                        products.length - 1,
                        ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH
                    )
                ),
                placerOrderNumber = order.orderNumber,
                patientVisitId = order.patientVisitId,
                context = context
            )
        }
}
