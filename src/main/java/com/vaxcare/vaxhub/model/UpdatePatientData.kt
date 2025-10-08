/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UpdatePatientData(
    val appointmentId: Int?,
    var payer: Payer? = null,
    val frontInsurancePath: String? = null,
    val backInsurancePath: String? = null,
    val updatePatient: UpdatePatient? = null,
    val retriedPhoto: Boolean = false,
    var updatedPhone: String? = null,
    // PhoneNumberWorkflowPresented metric data
    var phoneNumberWorkflowShown: Boolean = false,
    var phoneNumberPrefilled: Boolean = false,
    var phoneNumberUpdated: Boolean = false,
    var phoneContactAgreement: Boolean = false,
    var phoneEntered: String? = null
) : Parcelable {
    fun dataCollected() = frontInsurancePath != null || phoneContactAgreement
}
