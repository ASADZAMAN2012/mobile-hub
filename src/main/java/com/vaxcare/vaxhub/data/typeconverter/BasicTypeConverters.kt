/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.typeconverter

import androidx.room.TypeConverter
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.ClinicType
import com.vaxcare.vaxhub.model.IntegrationType
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.enums.CompensationStatus
import com.vaxcare.vaxhub.model.enums.CompensationSubStatus
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.ShotStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Classes used for serializing and deserializing data from the SqLite database.
 *
 * BasicTypeConverters holds all the converters for basic types that are included in the core
 * libraries and are not custom classes written by us at VaxCare.
 *
 * The basic syntax for adding a converter is:
 *
 * ```
 * @TypeConverter
 * open fun toT(value: S): T {
 *     return T
 * }
 *
 * @TypeConverter
 * open fun fromT(value: T): S {
 *     return S
 * }
 * ```
 *
 * Where T and S are the classes to serialize to and from. Note: SqLite only supports primitive
 * types, so S should be a primitive and T should be the class you desire.
 */
class BasicTypeConverters {
    @TypeConverter
    fun fromRelationshipToInsured(value: RelationshipToInsured?): Int? {
        return value?.ordinal
    }

    @TypeConverter
    fun toRelationshipToInsured(value: Int?): RelationshipToInsured? {
        return value?.let {
            val map = RelationshipToInsured.values().associateBy(RelationshipToInsured::ordinal)
            map[value] ?: RelationshipToInsured.Self
        }
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): Int {
        return value.ordinal
    }

    @TypeConverter
    fun toPaymentMethod(value: Int): PaymentMethod {
        val map = PaymentMethod.values().associateBy(PaymentMethod::ordinal)
        return map[value] ?: PaymentMethod.InsurancePay
    }

    @TypeConverter
    fun toInstant(long: Long?) =
        if (long != null) {
            Instant.ofEpochMilli(long)
        } else {
            null
        }

    @TypeConverter
    fun fromInstant(instant: Instant?) = instant?.toEpochMilli()

    @TypeConverter
    fun toLocalDate(long: Long?) =
        if (long != null) {
            Instant.ofEpochMilli(long).atZone(ZoneId.of("UTC")).toLocalDate()
        } else {
            null
        }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?) = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(long: Long?) =
        if (long != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(long), ZoneId.systemDefault())
        } else {
            null
        }

    @TypeConverter
    fun fromLocalDateTime(date: LocalDateTime?) = date?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

    @TypeConverter
    fun fromClinicType(value: ClinicType): Int {
        return value.ordinal
    }

    @TypeConverter
    fun toClinicType(value: Int): ClinicType {
        val map = ClinicType.values().associateBy(ClinicType::ordinal)
        return map[value] ?: ClinicType.Permanent
    }

    @TypeConverter
    fun fromCallToAction(value: CallToAction?): Int? {
        return value?.id
    }

    @TypeConverter
    fun toCallToAction(value: Int?): CallToAction? {
        return value?.let { CallToAction.fromInt(it) }
    }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.toBigDecimalOrNull()
    }

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode?): Int? {
        return value?.id
    }

    @TypeConverter
    fun toPaymentMode(value: Int?): PaymentMode? {
        val map =
            PaymentMode::class.sealedSubclasses.map { it.objectInstance }.associateBy { it?.id }
        return map[value]
    }

    @TypeConverter
    fun fromPaymentModeReason(value: PaymentModeReason?): Int? {
        return value?.id
    }

    @TypeConverter
    fun toPaymentModeReason(value: Int?): PaymentModeReason? {
        val map = PaymentModeReason::class.sealedSubclasses.map { it.objectInstance }
            .associateBy { it?.id }
        return map[value]
    }

    @TypeConverter
    fun fromIntListToString(satisfyingProductIds: List<Int>): String {
        return satisfyingProductIds.joinToString(",")
    }

    @TypeConverter
    fun toIntListFromString(satisfyingProductIds: String): List<Int> {
        return satisfyingProductIds.split(',')
            .filter { it.isNotEmpty() }
            .map { it.toInt() }
    }

    @TypeConverter
    fun fromCompensationStatus(value: CompensationStatus): Int {
        return value.ordinal
    }

    @TypeConverter
    fun toCompensationStatus(value: Int): CompensationStatus {
        val map = CompensationStatus.values().associateBy(CompensationStatus::ordinal)
        return map[value] ?: CompensationStatus.Unspecified
    }

    @TypeConverter
    fun fromCompensationSubStatus(value: CompensationSubStatus?): Int {
        return value?.ordinal ?: CompensationStatus.Unspecified.ordinal
    }

    @TypeConverter
    fun toCompensationSubStatus(value: Int): CompensationSubStatus {
        val map = CompensationSubStatus.values().associateBy(CompensationSubStatus::ordinal)
        return map[value] ?: CompensationSubStatus.Unspecified
    }

    @TypeConverter
    fun fromShotStatus(status: ShotStatus): Int {
        return status.value
    }

    @TypeConverter
    fun toShotStatus(value: Int): ShotStatus = ShotStatus.fromValue(value)

    @TypeConverter
    fun fromIcon(value: AppointmentIcon?): String? = value?.value

    @TypeConverter
    fun toIcon(value: String?): AppointmentIcon? = value?.let { AppointmentIcon.fromString(value) }

    @TypeConverter
    fun fromServiceType(value: AppointmentServiceType): String = value.display

    @TypeConverter
    fun toServiceType(value: String): AppointmentServiceType = AppointmentServiceType.fromString(value)

    @TypeConverter
    fun fromAppointmentStatus(value: AppointmentStatus): String = value.display

    @TypeConverter
    fun toAppointmentStatus(value: String): AppointmentStatus = AppointmentStatus.fromString(value)

    @TypeConverter
    fun fromIntegrationType(value: IntegrationType?): String? = value?.name

    @TypeConverter
    fun toIntegrationType(value: String?): IntegrationType? = value?.uppercase()?.let { IntegrationType.valueOf(it) }
}
