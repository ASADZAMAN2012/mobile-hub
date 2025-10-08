/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PaymentInformationRequestBody(
    @Json(name = "ExpirationDate") val expirationDate: String,
    @Json(name = "Email") val email: String,
    @Json(name = "CardholderName") val cardholderName: String,
    @Json(name = "CardNumber") val cardNumber: String,
    @Json(name = "PhoneNumber") val phoneNumber: String
) : Parcelable {
    var isOnFile = false
}
