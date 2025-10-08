/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.R

sealed class TestCards(
    val cardNumber: String,
    val expiration: String,
    val nameOnCard: String,
    val phoneNumber: String,
    val emailAddress: String,
    val cardIconResId: Int
) {
    val phoneNumberFirst
        get() = phoneNumber.substring(0, 3)
    val phoneNumberMid
        get() = phoneNumber.substring(3, 6)
    val phoneNumberLast
        get() = phoneNumber.substring(6)
    val expMonth
        get() = expiration.split(",")[0]
    val expYear
        get() = expiration.split(",")[1]

    object ILabsCard : TestCards(
        cardNumber = "4111111111111111",
        expiration = "Jan,2025",
        nameOnCard = "ILabs, Test",
        phoneNumber = "1235550123",
        emailAddress = "test@ilabs.com",
        cardIconResId = R.drawable.ic_icon_visa
    )
}
