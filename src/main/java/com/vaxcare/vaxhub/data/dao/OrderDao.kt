/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.vaxcare.core.model.Orders
import com.vaxcare.vaxhub.model.order.OrderEntity
import java.time.LocalDateTime

@Dao
abstract class OrderDao {
    // insert transactions
    @Transaction
    @Insert
    suspend fun upsert(orders: List<OrderEntity>): Int {
        val staleOrderNumbers = orders.filter { it.satisfyingProductIds.isNotEmpty() }
            .let { validOrders ->
                val orderNumbersToRemove = validOrders.mapNotNull {
                    if (it.isDeleted != true) {
                        getLocalOrderIdByPatientIdAndShortDescription(
                            it.patientId,
                            it.shortDescription
                        )
                    } else {
                        it.orderNumber
                    }
                }

                deleteByOrderNumbers(orderNumbersToRemove)
                insertOrders(validOrders.filter { it.isDeleted != true })
                orderNumbersToRemove
            }

        return staleOrderNumbers.size
    }

    @RawQuery
    abstract fun getOrdersByActivePatients(query: SupportSQLiteQuery): List<OrderEntity>

    @Transaction
    @Insert
    fun insertAll(orders: List<OrderEntity>) = insertOrders(orders)

    @Transaction
    @Query("DELETE FROM OrdersData WHERE expirationDate < :date")
    abstract suspend fun deleteExpiredOrdersAsync(date: LocalDateTime)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertOrders(offlineRequest: List<OrderEntity>)

    @Query("SELECT orderNumber FROM OrdersData WHERE patientId = :patientId AND shortDescription = :shortDescription")
    abstract suspend fun getLocalOrderIdByPatientIdAndShortDescription(
        patientId: Int,
        shortDescription: String
    ): String?

    @Query("SELECT * FROM OrdersData WHERE patientId = :patientId AND shortDescription = :shortDescription")
    abstract suspend fun getOrdersByPatientIdAndShortDescription(patientId: Int, shortDescription: String): List<Orders>

    @Query("SELECT * FROM OrdersData WHERE patientId = :patientId")
    abstract suspend fun getOrdersByPatientId(patientId: Int): List<OrderEntity>

    @Transaction
    @Query("DELETE FROM OrdersData WHERE orderNumber in (:localIds)")
    abstract suspend fun deleteByOrderNumbers(localIds: List<String>)
}
