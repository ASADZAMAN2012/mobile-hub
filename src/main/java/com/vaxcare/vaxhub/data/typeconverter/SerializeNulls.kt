/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.typeconverter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class SerializeNulls {
    companion object {
        var jsonAdapterFactory: JsonAdapter.Factory =
            object : JsonAdapter.Factory {
                override fun create(
                    type: Type,
                    annotations: Set<Annotation?>,
                    moshi: Moshi
                ): JsonAdapter<*>? {
                    val nextAnnotations =
                        Types.nextAnnotations(annotations, SerializeNulls::class.java)
                            ?: return null

                    return moshi.nextAdapter<Any>(
                        this,
                        type,
                        nextAnnotations
                    ).serializeNulls()
                }
            }
    }
}
