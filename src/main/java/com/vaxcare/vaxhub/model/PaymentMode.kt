/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/
package com.vaxcare.vaxhub.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = false)
sealed class PaymentMode(val id: Int) : Parcelable {
    @Parcelize
    object InsurancePay : PaymentMode(0)

    @Parcelize
    object PartnerBill : PaymentMode(1)

    @Parcelize
    object SelfPay : PaymentMode(2)

    @Parcelize
    object EmployerPay : PaymentMode(3)

    @Parcelize
    object NoPay : PaymentMode(4)
}
