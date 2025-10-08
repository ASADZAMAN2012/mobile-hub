/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.appointment

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vaxcare.vaxhub.model.enums.AppointmentHubContext
import java.time.LocalDateTime

@Entity
data class AppointmentHubMetaData(
    @PrimaryKey val appointmentId: Int,
    val updatedTime: LocalDateTime,
    val context: AppointmentHubContext = AppointmentHubContext.DEFAULT
)
