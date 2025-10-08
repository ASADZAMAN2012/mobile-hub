/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InvalidInfoWrapper(
    val infoType: InfoType,
    val appointmentId: Int,
    val patientId: Int
) : Parcelable

/**
 * Objects with descriptive fields for either Demographic or Payer information
 *
 */
sealed class InfoType : Parcelable {
    companion object {
        // demographics constants
        const val FIRSTNAME_PATH = "/firstName"
        const val LASTNAME_PATH = "/lastName"
        const val PHONE_PATH = "/phoneNumber"
        const val GENDER_PATH = "/gender"
        const val DOB_PATH = "/dob"
        const val ADDRESS1_PATH = "/address1"
        const val ADDRESS2_PATH = "/address2"
        const val CITY_PATH = "/city"
        const val STATE_PATH = "/state"
        const val ZIP_PATH = "/zip"

        // payer constants
        const val INSURANCEID_PATH = "/PaymentInformation/primaryInsuranceId"
        const val MEMBERID_PATH = "/PaymentInformation/primaryMemberId"
        const val GROUPID_PATH = "/PaymentInformation/primaryGroupId"
        const val PLANID_PATH = "/PaymentInformation/primaryInsurancePlanId"
        const val PORTALMAPPINGID_PATH = "/PaymentInformation/portalInsuranceMappingId"
        const val FINANCIAL_PATH = "/PaymentInformation/vfcFinancialClass"
        const val STOCK_PATH = "/PaymentInformation/stock"

        // media constants
        const val FLAG_PATH = "/metaData/flags"
        const val MEDIA_PATH = "/metaData/mediaProvided"
    }

    abstract val fields: List<InfoField>

    @Parcelize
    data class Demographic(override val fields: List<InfoField>) : InfoType()

    @Parcelize
    data class Payer(override val fields: List<InfoField>) : InfoType()

    @Parcelize
    data class Both(override val fields: List<InfoField>) : InfoType()
}
