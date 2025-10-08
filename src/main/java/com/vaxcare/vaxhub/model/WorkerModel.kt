/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.work.ExistingPeriodicWorkPolicy

data class WorkerModel<T>(
    val existingPolicy: ExistingPeriodicWorkPolicy,
    val name: String,
    val clazz: T
)
