/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@JsonClass(generateAdapter = true)
data class SessionUser(
    @PrimaryKey(autoGenerate = false)
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val sessionToken: String,
    val username: String,
    val lastLogin: LocalDateTime,
    val initialLogin: LocalDate = LocalDate.now(),
    val isLocked: Boolean = false
)
