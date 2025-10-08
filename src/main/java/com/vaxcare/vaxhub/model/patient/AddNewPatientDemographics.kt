/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.Race
import java.time.LocalDate

data class AddNewPatientDemographics(
    val firstName: String,
    val lastName: String,
    val dob: LocalDate,
    val gender: Int,
    var phoneNumber: String,
    val address1: String?,
    val address2: String?,
    val city: String?,
    val state: String?,
    val zip: String?,
    val race: Race? = null,
    val ethnicity: Ethnicity? = null,
    val ssn: String? = null,
    val mbi: String? = null
)
