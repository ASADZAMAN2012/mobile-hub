/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.util

object PhoneUtils {
    fun disassemblePhone(rawPhone: String?): Array<String>? {
        return if (rawPhone?.length == 10) {
            arrayOf(
                rawPhone.substring(0, 3),
                rawPhone.substring(3, 6),
                rawPhone.substring(6)
            )
        } else {
            null
        }
    }
}
