/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui.appointment

import android.os.Parcelable
import com.vaxcare.vaxhub.model.PatientCollectData
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppointmentCheckoutMetaData(
    val appointmentId: Int = -1,
    val isForceRiskFree: Boolean = false,
    val isLocallyCreated: Boolean = false,
    val curbsideNewPatient: Boolean = false,
    val updateData: PatientCollectData? = null
) : Parcelable
