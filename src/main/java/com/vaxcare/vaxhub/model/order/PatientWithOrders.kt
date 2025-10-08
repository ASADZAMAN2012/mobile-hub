/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.order

import androidx.room.Embedded
import androidx.room.Relation
import com.vaxcare.vaxhub.model.Patient

class PatientWithOrders(
    @Embedded
    val patient: Patient,
    @Relation(parentColumn = "id", entityColumn = "patientId")
    var orders: List<OrderEntity>? = null
)
