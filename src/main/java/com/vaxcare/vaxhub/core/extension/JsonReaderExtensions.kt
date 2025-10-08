/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import com.squareup.moshi.JsonReader

fun JsonReader.readObject(work: () -> Unit) {
    beginObject()
    while (hasNext()) {
        work.invoke()
    }
    endObject()
}

fun JsonReader.readArray(work: () -> Unit) {
    beginArray()
    while (hasNext()) {
        work.invoke()
    }
    endArray()
}
