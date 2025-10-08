/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "ShotAdministrators")
@JsonClass(generateAdapter = true)
data class ShotAdministrator(
    @PrimaryKey val id: Int,
    val firstName: String,
    val lastName: String
)
