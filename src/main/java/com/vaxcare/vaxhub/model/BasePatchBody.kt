/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BasePatchBody(
    val op: Operation,
    val path: String,
    val value: Any? = null
)

enum class Operation(val value: String) {
    @Json(name = "replace")
    REPLACE("replace"),

    @Json(name = "add")
    ADD("add"),

    @Json(name = "remove")
    REMOVE("remove"),
}
