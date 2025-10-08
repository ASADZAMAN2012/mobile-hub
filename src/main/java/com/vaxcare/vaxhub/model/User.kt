/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "Users")
@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "FirstName")
    val firstName: String?,
    @Json(name = "LastName")
    val lastName: String?,
    @Json(name = "Pin")
    val pin: String,
    @Json(name = "UserId")
    @PrimaryKey(autoGenerate = false)
    val userId: Int,
    @Json(name = "UserName")
    val userName: String?
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}
