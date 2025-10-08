/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.isWarning
import com.vaxcare.vaxhub.model.inventory.matchesAge
import com.vaxcare.vaxhub.model.inventory.matchesGender
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class AgeIndicatedRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.OutOfAgeIndication

    /**
     * Evaluates if a patient is out of the product age indications (when dob is missing, ignore)
     *
     * @property comparator AgeIndicatedArgs (patient gender & date of birth)
     */
    data class AgeIndicatedValidator(override val comparator: AgeIndicatedArgs) :
        AgeIndicatedRules<AgeIndicatedArgs>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            comparator.patientDoB?.let {
                val now = LocalDate.now()
                val patientAgeInDays = ChronoUnit.DAYS.between(it, now).toInt()

                val rules = productToValidate.ageIndications.none { ageIndication ->
                    !ageIndication.isWarning() &&
                        ageIndication.matchesGender(comparator.patientGender) &&
                        ageIndication.matchesAge(patientAgeInDays)
                }
                return rules
            }
            return false
        }
    }

    data class AgeIndicatedArgs(
        val patientGender: String?,
        val patientDoB: LocalDate?
    )
}
