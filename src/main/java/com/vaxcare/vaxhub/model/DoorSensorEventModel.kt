/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
data class DoorSensorEventModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val index: Int,
    val count: Int,
    val createdDate: Instant = Instant.now()
)
