/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentInformationResponse(
    val expirationDate: String?,
    val email: String?,
    val cardholderName: String?,
    val cardNumber: String?,
    val phoneNumber: String?
)
