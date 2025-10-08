/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.typeadapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.vaxcare.vaxhub.core.extension.toLocalDateTime
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.IntegrationType
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.PhoneContactConsentStatus
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.PartDEligibilityStatusCode
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.enums.ProductStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class TypeAdapters {
    @ToJson
    fun uuidToString(uuid: UUID) = uuid.toString()

    @FromJson
    fun stringToUuid(string: String): UUID = UUID.fromString(string)

    @ToJson
    fun instantToString(value: Instant): String = value.toLocalDateTime()

    @FromJson
    fun stringToInstant(string: String): Instant = Instant.parse(string)

    @ToJson
    fun networkStatusToInt(status: NetworkStatus) = status.value

    @FromJson
    fun intToNetworkStatus(int: Int) = NetworkStatus.fromInt(int)

    @ToJson
    fun productStatusToInt(status: ProductStatus) = status.id

    @FromJson
    fun intToProductStatus(int: Int) = ProductStatus.fromInt(int)

    @ToJson
    fun productCategoryToInt(cat: ProductCategory) = cat.id

    @FromJson
    fun intToProductCategory(int: Int) = ProductCategory.fromInt(int)

    @ToJson
    fun appointmentChangeTypeToInt(type: AppointmentChangeType): Int = type.ordinal

    @FromJson
    fun intToAppointmentChangeType(int: Int): AppointmentChangeType = AppointmentChangeType.fromInt(int)

    @ToJson
    fun appointmentChangeReasonToInt(type: AppointmentChangeReason): Int = type.ordinal

    @FromJson
    fun intToAppointmentChangeReasonType(int: Int): AppointmentChangeReason = AppointmentChangeReason.fromInt(int)

    @FromJson
    fun stringToBigDecimal(string: String): BigDecimal = BigDecimal(string)

    @ToJson
    fun bigDecimalToString(value: BigDecimal) = value.toString()

    @FromJson
    fun stringToCallToAction(string: String): CallToAction =
        CallToAction::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .firstOrNull { it.javaClass.simpleName == string } ?: CallToAction.None

    @ToJson
    fun callToActionToString(callToAction: CallToAction): String = callToAction.javaClass.simpleName

    @FromJson
    fun stringToPaymentMode(string: String): PaymentMode =
        PaymentMode::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .firstOrNull { it.javaClass.simpleName == string } ?: PaymentMode.InsurancePay

    @ToJson
    fun paymentModeToString(paymentMode: PaymentMode): String = paymentMode.javaClass.simpleName

    @FromJson
    fun stringToPaymentModeReason(string: String): PaymentModeReason =
        PaymentModeReason::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .firstOrNull { it.javaClass.simpleName == string } ?: PaymentModeReason.Unknown

    @ToJson
    fun paymentModeReasonToString(paymentModeReason: PaymentModeReason): String = paymentModeReason.javaClass.simpleName

    @FromJson
    fun stringToAppointmentIcon(value: String?): AppointmentIcon? = AppointmentIcon.fromString(value)

    @ToJson
    fun appointmentIconToString(appointmentIcon: AppointmentIcon?): String? = appointmentIcon?.value

    @FromJson
    fun stringToAppointmentStatus(value: String): AppointmentStatus = AppointmentStatus.fromString(value)

    @ToJson
    fun appointmentStatusToString(appointmentStatus: AppointmentStatus): String = appointmentStatus.display

    @FromJson
    fun stringToAppointmentServiceType(value: String): AppointmentServiceType = AppointmentServiceType.fromString(value)

    @ToJson
    fun appointmentServiceTypeToString(appointmentServiceType: AppointmentServiceType): String =
        appointmentServiceType.display

    @FromJson
    fun stringToPhoneContactConsentStatus(value: String?): PhoneContactConsentStatus =
        PhoneContactConsentStatus.fromValue(value)

    @ToJson
    fun phoneContactConsentStatusToString(phoneContactConsentStatus: PhoneContactConsentStatus) =
        phoneContactConsentStatus.value

    @FromJson
    fun toIntegrationType(value: String): IntegrationType = IntegrationType.valueOf(value.uppercase())

    @ToJson
    fun fromIntegrationType(value: IntegrationType): String = value.name

    @ToJson
    fun partDEligibilityStatusCodeToInt(type: PartDEligibilityStatusCode): Int = type.ordinal

    @FromJson
    fun intToPartDEligibilityStatusCode(int: Int): PartDEligibilityStatusCode = PartDEligibilityStatusCode.fromInt(int)
}
