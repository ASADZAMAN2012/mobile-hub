/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.content.Context
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

@Entity(tableName = "Patients")
@JsonClass(generateAdapter = true)
data class Patient(
    @PrimaryKey val id: Int,
    val originatorPatientId: String?,
    val firstName: String,
    val lastName: String,
    val dob: String?,
    val middleInitial: String?,
    val race: String?,
    val ethnicity: String?,
    var gender: String?,
    val ssn: String?,
    val address1: String?,
    val address2: String?,
    val city: String?,
    val state: String?,
    val zip: String?,
    var phoneNumber: String?,
    val email: String?,
    @Embedded(prefix = "payment_")
    var paymentInformation: PaymentInformation?
) {
    @Ignore
    var selfPayType: SelfPayType = SelfPayType.NONE

    fun getDobString(): String? =
        if (dob != null) {
            try {
                val epoch =
                    Instant.ofEpochSecond(LocalDateTime.parse(dob).toEpochSecond(ZoneOffset.UTC))
                        .toEpochMilli()
                Instant.ofEpochMilli(epoch).atZone(ZoneId.of("UTC")).toLocalDate()
                    .toLocalDateString()
            } catch (ex: DateTimeParseException) {
                Timber.e(ex, "dob: $dob is malformed. Exception: ")
                null
            }
        } else {
            null
        }

    fun getFormatAddress(context: Context): String {
        val builder = StringBuilder()
        if (!address1.isNullOrEmpty()) {
            builder.append("${address1}\n")
        }
        if (!address2.isNullOrEmpty()) {
            builder.append("${address2}\n")
        }
        if (!city.isNullOrEmpty()) {
            builder.append(city)
            if (state.isNullOrEmpty()) {
                builder.append("\n")
            } else {
                builder.append(", ${state}\n")
            }
        } else {
            if (!state.isNullOrEmpty()) {
                builder.append("${state}\n")
            }
        }
        if (!zip.isNullOrEmpty()) {
            builder.append(zip)
        }

        var address = builder.toString().trim()
        if (address.isEmpty()) {
            address = context.getString(R.string.patient_confirm_no_address)
        }
        return address
    }

    enum class PatientGender(val value: String) {
        MALE("Male"),
        FEMALE("Female");

        companion object {
            private val map = values().associateBy(PatientGender::value)

            fun fromInt(value: Int) =
                when (value) {
                    0 -> MALE
                    else -> FEMALE
                }

            fun fromString(type: String) =
                when (type) {
                    "Male" -> MALE
                    else -> FEMALE
                }
        }
    }

    enum class SelfPayType(displayText: String) {
        NONE("None"),
        CASH_CHECK("Cash or Check"),
        CREDIT("Credit Card")
    }
}
