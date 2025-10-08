/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VaxHubUpdate(
    @Json(name = "PatchPath") val apkUri: String,
    @Json(name = "TargetVersionCode") val versionCode: Int,
    @Json(name = "VersionName") val versionName: String,
    @Json(name = "TabletId") val tabletId: String
)
