/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.typeadapter.legacy

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import com.vaxcare.vaxhub.model.legacy.enums.TransactionType

class LegacyTransactionTypeAdapter {
    @ToJson
    fun toJson(transactionType: TransactionType) = transactionType.id.toString()

    @FromJson
    fun fromJson(reader: JsonReader): TransactionType {
        TODO()
    }
}
