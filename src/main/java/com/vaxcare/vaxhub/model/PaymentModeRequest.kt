/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
class PaymentModeRequest(
    @Json(name = "PaymentMode") val paymentMode: PaymentMode,
    @Json(name = "PaymentModeReason") val paymentModeReason: PaymentModeReason? = null
) : Parcelable
