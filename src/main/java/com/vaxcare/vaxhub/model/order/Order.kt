/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.order

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.core.annotation.LocalTime
import java.time.Instant
import java.time.LocalDateTime

interface Order {
    val orderId: Int?

    val partnerId: Int

    val clinicId: Int

    val patientVisitId: Int?

    val patientId: Int

    val isDeleted: Boolean?

    val shortDescription: String

    val orderNumber: String

    val satisfyingProductIds: List<Int>

    val serverSyncDateTimeUtc: Instant

    val durationInDays: Int

    val expirationDate: LocalDateTime

    val orderDate: LocalDateTime
}

// DTO

data class OrderDto(
    override val orderId: Int?,
    override val partnerId: Int,
    override val clinicId: Int,
    override val patientVisitId: Int?,
    override val patientId: Int,
    override val isDeleted: Boolean?,
    override val shortDescription: String,
    override val orderNumber: String,
    override val satisfyingProductIds: List<Int>,
    override val serverSyncDateTimeUtc: Instant,
    override val durationInDays: Int,
    override val expirationDate: LocalDateTime,
    override val orderDate: LocalDateTime
) : Order {
    companion object {
        fun fromOrder(order: Order): OrderDto =
            OrderDto(
                orderId = order.orderId,
                partnerId = order.partnerId,
                clinicId = order.clinicId,
                patientId = order.patientId,
                isDeleted = order.isDeleted,
                patientVisitId = order.patientVisitId,
                shortDescription = order.shortDescription,
                orderNumber = order.orderNumber,
                satisfyingProductIds = order.satisfyingProductIds,
                serverSyncDateTimeUtc = order.serverSyncDateTimeUtc,
                durationInDays = order.durationInDays,
                orderDate = order.orderDate,
                expirationDate = order.expirationDate
            )
    }
}

// Json Mapper

@JsonClass(generateAdapter = true)
data class OrderJson(
    @Json(name = "Id") override val orderId: Int?,
    @Json(name = "PartnerId") override val partnerId: Int,
    @Json(name = "ClinicId") override val clinicId: Int,
    @Json(name = "PatientVisitId") override val patientVisitId: Int?,
    @Json(name = "PatientId") override val patientId: Int,
    @Json(name = "IsDeleted") override val isDeleted: Boolean?,
    @Json(name = "ShortDescription") override val shortDescription: String,
    @Json(name = "OrderNumber") override val orderNumber: String,
    @Json(name = "SatisfyingProductIds") override val satisfyingProductIds: List<Int>,
    // This might need to be Date instead of LocalDate
    @Json(name = "ServerSyncDateTimeUtc") override val serverSyncDateTimeUtc: Instant,
    @Json(name = "DurationInDays") override val durationInDays: Int,
    @LocalTime @Json(name = "ExpirationDate") override val expirationDate: LocalDateTime,
    @LocalTime @Json(name = "OrderDateUtc") override val orderDate: LocalDateTime
) : Order

// Database

@Entity(tableName = "OrdersData")
data class OrderEntity(
    override val orderId: Int?,
    override val partnerId: Int,
    override val clinicId: Int,
    override val patientVisitId: Int?,
    override val patientId: Int,
    override val isDeleted: Boolean?,
    override val shortDescription: String,
    @PrimaryKey override val orderNumber: String,
    override val satisfyingProductIds: List<Int>,
    override val serverSyncDateTimeUtc: Instant,
    override val durationInDays: Int,
    override val expirationDate: LocalDateTime,
    override val orderDate: LocalDateTime
) : Order {
    companion object {
        fun fromOrder(order: Order): OrderEntity =
            OrderEntity(
                orderId = order.orderId,
                partnerId = order.partnerId,
                clinicId = order.clinicId,
                patientId = order.patientId,
                isDeleted = order.isDeleted,
                patientVisitId = order.patientVisitId,
                shortDescription = order.shortDescription,
                orderNumber = order.orderNumber,
                satisfyingProductIds = order.satisfyingProductIds,
                serverSyncDateTimeUtc = order.serverSyncDateTimeUtc,
                durationInDays = order.durationInDays,
                orderDate = order.orderDate,
                expirationDate = order.expirationDate
            )
    }
}
