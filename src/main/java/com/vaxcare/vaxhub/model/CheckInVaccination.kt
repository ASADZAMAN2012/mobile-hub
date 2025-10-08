/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckInVaccination(
    val id: Int,
    val productId: Int,
    val ageIndicated: Boolean,
    val lotNumber: String,
    val method: String?,
    val site: String?,
    val doseSeries: Int?,
    var paymentMode: PaymentMode?,
    var paymentModeReason: PaymentModeReason?,
    var reasonId: Int? = null
) {
    fun toAdministeredVaccine(appointmentId: Int): AdministeredVaccine {
        val administeredVaccine = AdministeredVaccine(
            id = 0,
            checkInVaccinationId = id,
            appointmentId = appointmentId,
            lotNumber = lotNumber,
            ageIndicated = ageIndicated,
            method = method,
            site = site,
            productId = productId,
            synced = null,
            deletedDate = null,
            isDeleted = false,
            doseSeries = doseSeries
        )

        administeredVaccine.paymentMode = paymentMode
        administeredVaccine.paymentModeReason = paymentModeReason
        return administeredVaccine
    }
}
