/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@Entity
@JsonClass(generateAdapter = true)
data class ProductOneTouch(
    @Json(name = "salesProductId")
    @PrimaryKey
    val id: Int,
    @Json(name = "productId") val productId: Int,
    @Json(name = "selfPayRate") val selfPayRate: BigDecimal
)
