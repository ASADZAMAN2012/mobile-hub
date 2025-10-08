/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.data.typeconverter.BasicTypeConverters
import com.vaxcare.vaxhub.model.RelationshipToInsured
import org.junit.Test

/**
 * Test for BasicTypeConverters
 *
 *  TODO: Outstanding tests to write from BasicTypeConverters:
 *
 *  fromPaymentMethod(value: PaymentMethod): Int
 *  toPaymentMethod(value: Int): PaymentMethod
 *  toInstant(long: Long?): Instant
 *  fromInstant(instant: Instant?): Long
 *  toLocalDate(long: Long?): LocalDate
 *  fromLocalDate(date: LocalDate?): Long
 *  toLocalDateTime(long: Long?): LocalDateTime
 *  fromLocalDateTime(date: LocalDateTime?): Long
 *  fromClinicType(value: ClinicType): Int
 *  toClinicType(value: Int): ClinicType
 *  fromCallToAction(value: CallToAction?): Int?
 *  toCallToAction(value: Int?): CallToAction?
 *  fromBigDecimal(value: BigDecimal?): String?
 *  toBigDecimal(value: String?): BigDecimal?
 *  fromPaymentMode(value: PaymentMode?): Int?
 *  toPaymentMode(value: Int?): PaymentMode?
 *  fromPaymentModeReason(value: PaymentModeReason?): Int?
 *  toPaymentModeReason(value: Int?): PaymentModeReason?
 *  fromCompensationStatus(value: CompensationStatus): Int
 *  toCompensationStatus(value: Int): CompensationStatus
 *  fromCompensationSubStatus(value: CompensationSubStatus): Int
 *  toCompensationSubStatus(value: Int): CompensationSubStatus
 *  fromShotStatus(status: ShotStatus): Int
 *  toShotStatus(value: Int): ShotStatus
 */
class TypeConverterTest {
    private val typeConverter = BasicTypeConverters()

    @Test
    fun relationshipTest() {
        assertRelation(RelationshipToInsured.Self)
        assertRelation(RelationshipToInsured.Dependent)
        assertRelation(RelationshipToInsured.Spouse)
    }

    @Test
    fun satisfyingProductIdsTest() {
        assert(typeConverter.fromIntListToString(listOf(1, 2, 3, 4, 5)).isNotEmpty())
        assert(typeConverter.fromIntListToString(listOf()).isEmpty())
        assert(typeConverter.toIntListFromString("1,2,3,4").isNotEmpty())
        assert(typeConverter.toIntListFromString("").isEmpty())
    }

    private fun assertRelation(relation: RelationshipToInsured) {
        val resultInt = typeConverter.fromRelationshipToInsured(relation)
        assert(resultInt == relation.ordinal)
        val resultRelation = typeConverter.toRelationshipToInsured(relation.ordinal)
        assert(resultRelation == relation)
    }
}
