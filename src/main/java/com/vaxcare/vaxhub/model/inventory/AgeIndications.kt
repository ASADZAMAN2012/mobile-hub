/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.model.enums.Gender

@Entity
@JsonClass(generateAdapter = true)
data class AgeIndication(
    val gender: Gender,
    @PrimaryKey(autoGenerate = false) val id: Int,
    val maxAge: Int?,
    val minAge: Int?,
    val productId: Int,
    @Embedded(prefix = "warning_") val warning: AgeWarning?
)

@Entity(tableName = "AgeWarning")
@JsonClass(generateAdapter = true)
data class AgeWarning(
    val title: String,
    val message: String,
    val promptType: PromptType
) {
    enum class PromptType {
        @Json(name = "singleSelect")
        SINGLE_SELECT,

        @Json(name = "boolean")
        BOOLEAN
    }
}

fun AgeIndication.isWarning() = this.warning?.let { it.title.isNotBlank() && it.message.isNotBlank() } ?: false

fun AgeIndication.matchesAge(patientAgeInDays: Int) = patientAgeInDays in ((this.minAge ?: 0)..(this.maxAge ?: 364885))

fun AgeIndication.matchesGender(patientGender: String?) =
    (patientGender == null || this.gender.name.contains(patientGender[0]))
