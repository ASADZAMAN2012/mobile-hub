/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.util

import java.time.LocalDateTime

object AppointmentUtils {
    fun LocalDateTime.getNextAppointmentSlot(): LocalDateTime {
        val nextSlot = when {
            minute < 15 -> {
                plusMinutes(15 - minute.toLong())
            }

            minute in 15..29 -> {
                plusMinutes(30 - minute.toLong())
            }

            minute in 30..44 -> {
                plusMinutes(45 - minute.toLong())
            }

            else -> {
                plusMinutes(60 - minute.toLong())
            }
        }
        return nextSlot.minusSeconds(second.toLong())
    }
}
