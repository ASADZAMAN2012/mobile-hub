/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy

import android.os.Build
import com.squareup.moshi.JsonClass
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.BuildConfig
import java.util.UUID

@JsonClass(generateAdapter = true)
data class LegacyPostDto<T>(
    val androidSdk: Int,
    val androidVersion: String,
    val assetTag: String,
    val clinicId: Long,
    val deviceSerialNumber: String,
    val key: String,
    val payload: T,
    val version: Int,
    val versionName: String,
    val userName: String,
    val userId: Int
)

inline fun <reified T> generatePostDto(payload: T, localStorage: LocalStorage): LegacyPostDto<T> {
    return LegacyPostDto(
        androidSdk = Build.VERSION.SDK_INT,
        androidVersion = Build.VERSION.RELEASE,
        assetTag = "-1",
        clinicId = localStorage.currentClinicId,
        deviceSerialNumber = localStorage.deviceSerialNumber,
        key = UUID.randomUUID().toString(),
        payload = payload,
        version = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME,
        userName = localStorage.userName,
        userId = localStorage.userId
    )
}
