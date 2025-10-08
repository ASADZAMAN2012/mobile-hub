/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Entity(tableName = "PaymentInformation")
@JsonClass(generateAdapter = true)
data class PaymentInformation(
    @PrimaryKey val id: Int,
    val insuranceName: String? = null,
    val primaryInsuranceId: Int? = null,
    val primaryInsurancePlanId: Int? = null,
    val primaryMemberId: String? = null,
    val primaryGroupId: String? = null,
    val uninsured: Boolean,
    val paymentMode: PaymentMethod,
    val vfcFinancialClass: String? = null,
    val insuredFirstName: String? = null,
    val insuredLastName: String? = null,
    val insuredDob: String? = null,
    val insuredGender: String? = null,
    val appointmentId: Int? = null,
    val relationshipToInsured: RelationshipToInsured? = null,
    val portalInsuranceMappingId: Int? = null,
    val mbi: String? = null,
    val stock: String? = null
) {
    fun getDob(): LocalDate? {
        if (insuredDob == null) {
            return null
        }
        val epoch =
            Instant.ofEpochSecond(LocalDateTime.parse(insuredDob).toEpochSecond(ZoneOffset.UTC))
                .toEpochMilli()
        return Instant.ofEpochMilli(epoch).atZone(ZoneId.of("UTC")).toLocalDate()
    }

    fun getDobString(): String? {
        return getDob()?.toLocalDateString()
    }

    fun isMissingInsuranceData(): Boolean =
        insuranceName?.let {
            val missingIds = listOf(null, 0, -1)
            it.isBlank() ||
                primaryInsuranceId in missingIds ||
                primaryInsurancePlanId in missingIds
        } ?: true
}
