/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "FeatureFlags")
@JsonClass(generateAdapter = true)
data class FeatureFlag(
    @PrimaryKey
    val featureFlagId: Int,
    val featureFlagName: String
)
