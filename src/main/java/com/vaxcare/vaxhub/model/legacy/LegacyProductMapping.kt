/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class LegacyProductMapping(
    @Json(name = "coreProductId")
    @PrimaryKey
    val id: Int,
    val epProductName: String,
    val epPackageId: Int,
    val epProductId: Int,
    val prettyName: String?,
    val dosesInSeries: Int?
)
