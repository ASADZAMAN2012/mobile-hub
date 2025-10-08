/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.vaxcare.core.model.OrderSyncResults
import com.vaxcare.core.model.Orders
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.extension.toUtc
import com.vaxcare.vaxhub.data.dao.OrderDao
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.order.OrderDto
import com.vaxcare.vaxhub.model.order.OrderEntity
import com.vaxcare.vaxhub.model.order.OrderJson
import com.vaxcare.vaxhub.repository.OrdersRepository.Companion.SyncContextFrom
import com.vaxcare.vaxhub.repository.util.ConvertOrderSyncResultsToMetricsUtil
import com.vaxcare.vaxhub.web.OrmApi
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Order Repository to handle the interaction with sources of data like storage and api
 */
interface OrdersRepository {
    /**
     * Get orders from the last Sync Date
     *
     */
    suspend fun syncOrdersChanges(syncContextFrom: SyncContextFrom)

    /**
     * Get orders by group number
     *
     * @param orderGroupNumber orders group number
     */
    suspend fun syncOrdersByGroup(
        orderGroupNumber: String,
        isCalledByJob: Boolean = false,
        syncContextFrom: SyncContextFrom
    )

    /**
     * Gets the orders only related to patients with appointments
     *
     * @return list of orders
     */
    suspend fun getOrdersByActivePatients(): List<OrderDto>

    /**
     * Remove the expired doses
     *
     */
    suspend fun removeExpiredOrders()

    /**
     * Fetch all orders for a patient
     *
     */
    suspend fun getOrdersByPatient(patientId: Int): List<OrderEntity>

    companion object {
        enum class SyncContextFrom(val prettyName: String) {
            FCM("FCM Event"),
            SCHEDULE("Schedule Refresh"),
            REFRESH_APPOINTMENT("Appointment Refresh")
        }
    }
}

class OrdersRepositoryImpl @Inject constructor(
    private val ordersApi: OrmApi,
    private val orderDao: OrderDao,
    private val localStorage: LocalStorage,
    @MHAnalyticReport private val analytics: AnalyticReport,
    private val orderMetricUtil: ConvertOrderSyncResultsToMetricsUtil
) : OrdersRepository {
    companion object {
        private const val DAY_IN_PAST: Long = 15L

        private const val RAW_QUERY =
            """SELECT *, ROW_NUMBER () OVER (PARTITION BY patientId, shortDescription ORDER BY orderDate DESC) RowNumber 
        FROM OrdersData o 
        INNER JOIN Patients p ON o.patientId == p.id 
        WHERE RowNumber == 1"""
    }

    override suspend fun syncOrdersChanges(syncContextFrom: SyncContextFrom) {
        val lastSyncDate =
            localStorage.lastOrdersSyncDate?.toString() ?: LocalDateTime.now()
                .minusDays(DAY_IN_PAST).toUtc()
                .toString()

        val orders = ordersApi.getOrdersChanges(localStorage.clinicId.toInt(), lastSyncDate)
        insertOrders(orders, syncContextFrom.prettyName)

        localStorage.lastOrdersSyncDate = LocalDateTime.now().toUtc()
    }

    private suspend fun insertOrders(orders: List<OrderJson>, syncContext: String) {
        val updatedOrders = orders.flatMap {
            orderDao.getOrdersByPatientIdAndShortDescription(
                it.patientId,
                it.shortDescription
            )
        }
        val deletedOrders = orders.filter { it.isDeleted == true }.map { it.toOrders() }
        orderDao.upsert(orders.map { OrderEntity.fromOrder(it) })
        val metrics = orderMetricUtil.convertOrderSyncResults(
            results = OrderSyncResults(
                orders.map { it.toOrders() },
                updatedOrders,
                deletedOrders
            ),
            context = syncContext
        )
        analytics.saveMetric(*metrics.toTypedArray())
    }

    override suspend fun syncOrdersByGroup(
        orderGroupNumber: String,
        isCalledByJob: Boolean,
        syncContextFrom: SyncContextFrom
    ) {
        val orders = ordersApi.getOrdersByGroup(orderGroupNumber, isCalledByJob)
        insertOrders(orders, syncContextFrom.prettyName)
    }

    override suspend fun getOrdersByActivePatients(): List<OrderDto> {
        return orderDao.getOrdersByActivePatients(SimpleSQLiteQuery(RAW_QUERY)).map {
            OrderDto.fromOrder(it)
        }
    }

    override suspend fun removeExpiredOrders() {
        orderDao.deleteExpiredOrdersAsync(LocalDateTime.now())
    }

    private fun OrderJson.toOrders() =
        Orders(
            orderId = orderId,
            partnerId = partnerId,
            clinicId = clinicId,
            patientVisitId = patientVisitId,
            patientId = patientId,
            isDeleted = isDeleted,
            shortDescription = shortDescription,
            orderNumber = orderNumber,
            satisfyingProductIds = satisfyingProductIds,
            serverSyncDateTimeUtc = serverSyncDateTimeUtc,
            durationInDays = durationInDays,
            expirationDate = expirationDate,
            orderDate = orderDate
        )

    override suspend fun getOrdersByPatient(patientId: Int): List<OrderEntity> {
        return orderDao.getOrdersByPatientId(patientId)
    }
}
