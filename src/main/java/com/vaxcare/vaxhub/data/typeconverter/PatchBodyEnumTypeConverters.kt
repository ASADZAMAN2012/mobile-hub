/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.typeconverter

import androidx.room.TypeConverter
import com.vaxcare.vaxhub.core.extension.stringToEnum
import com.vaxcare.vaxhub.model.Operation

class PatchBodyEnumTypeConverters {
    @TypeConverter
    fun fromOperation(operation: Operation): String = operation.value

    @TypeConverter
    fun toOperation(value: String) = stringToEnum(value, Operation.ADD)
}
