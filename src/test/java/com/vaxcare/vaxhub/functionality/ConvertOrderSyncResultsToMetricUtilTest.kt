/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.core.model.Orders
import com.vaxcare.core.report.model.OrderAckMetric
import com.vaxcare.core.report.model.OrderDeleteMetric
import com.vaxcare.vaxhub.data.MockOrders
import com.vaxcare.vaxhub.repository.util.ConvertOrderSyncResultsToMetricsUtil
import io.mockk.mockk
import org.junit.Test

class ConvertOrderSyncResultsToMetricUtilTest {
    private val util = mockk<ConvertOrderSyncResultsToMetricsUtil>(relaxed = true)

    @Test
    fun `Order Sync Result Conversion Full Test`() {
        val context = "unit test"
        val metrics = util.convertOrderSyncResults(MockOrders.results, context)
        metrics.forEachScoped {
            assert(this is OrderAckMetric || this is OrderDeleteMetric)
            when (this) {
                is OrderAckMetric -> checkWithOrder(MockOrders.upsertedOrders.first(), context)
                is OrderDeleteMetric -> checkWithOrder(MockOrders.deletedOrders.first(), context)
            }
        }
    }

    private fun OrderDeleteMetric.checkWithOrder(associatedOrder: Orders, metricContext: String) {
        assert(patientId == associatedOrder.patientId)
        assert(shortDescription == associatedOrder.shortDescription)
        assert(productIds == associatedOrder.satisfyingProductIds.joinToString(","))
        assert(placerOrderNumber == associatedOrder.orderNumber)
        assert(patientVisitId == associatedOrder.patientVisitId)
        assert(context == metricContext)
    }

    private fun OrderAckMetric.checkWithOrder(order: Orders, metricContext: String) {
        assert(patientId == order.patientId)
        assert(shortDescription == order.shortDescription)
        assert(productIds == order.satisfyingProductIds.joinToString(","))
        assert(placerOrderNumber == order.orderNumber)
        assert(patientVisitId == order.patientVisitId)
        assert(patientOrderCountTotal == MockOrders.totalOrders)
        assert(patientOrderCountOutstanding == MockOrders.outstandingOrdersCount)
        assert(context == metricContext)
        assert(!deleted)
    }

    private inline fun <T> Iterable<T>.forEachScoped(action: T.() -> Unit) {
        for (element in this) element.action()
    }
}
