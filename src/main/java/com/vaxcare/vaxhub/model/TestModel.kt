/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
data class TestModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val guid: String,
    val date: Instant = Instant.now()
)
