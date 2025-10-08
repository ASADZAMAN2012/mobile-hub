/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.RelationshipToInsured
import java.time.LocalDate

data class PayerInfoUiData(
    val memberId: String? = null,
    val groupId: String? = null,
    val relationship: RelationshipToInsured = RelationshipToInsured.Self,
    val insuredFirstName: String? = null,
    val insuredLastName: String? = null,
    val insuredDob: LocalDate? = null,
    val insuredGender: DriverLicense.Gender? = null
)
