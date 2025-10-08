/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.core.annotation.LocalTime
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@JsonClass(generateAdapter = true)
data class AdministeredVaccine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Json(name = "checkinVaccinationId") val checkInVaccinationId: Int,
    val appointmentId: Int,
    val lotNumber: String,
    val ageIndicated: Boolean,
    val method: String?,
    val site: String?,
    val productId: Int,
    @LocalTime val synced: LocalDate?,
    val doseSeries: Int? = null,
    @LocalTime val deletedDate: LocalDateTime?,
    val isDeleted: Boolean
) {
    @Ignore
    var paymentMode: PaymentMode? = null

    @Ignore
    var paymentModeReason: PaymentModeReason? = null
}
