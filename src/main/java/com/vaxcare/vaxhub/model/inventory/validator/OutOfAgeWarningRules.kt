/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.AgeIndication
import com.vaxcare.vaxhub.model.inventory.AgeWarning
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.isWarning
import com.vaxcare.vaxhub.model.inventory.matchesAge
import com.vaxcare.vaxhub.model.inventory.matchesGender
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class OutOfAgeWarningRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue
        get() = warning.let {
            ProductIssue.OutOfAgeWarning(
                title = it?.title,
                message = it?.message,
                promptType = it?.promptType
            )
        }
    var warning: AgeWarning? = null

    /**
     * Evaluates if a patient is within a product age warning (when dob is missing, ignore)
     *
     * @property comparator OutOfAgeWarningArgs (patient gender & date of birth)
     */
    data class AgeWarningValidator(override val comparator: OutOfAgeWarningArgs) :
        OutOfAgeWarningRules<OutOfAgeWarningArgs>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            var ageWarningRule: AgeIndication? = null

            if (comparator.patientDoB != null) {
                val now = LocalDate.now()
                val ageInDays = ChronoUnit.DAYS.between(comparator.patientDoB, now).toInt()

                val ageIndicationExists = productToValidate.ageIndications.any {
                    !it.isWarning() && it.matchesAge(ageInDays) && it.matchesGender(comparator.patientGender)
                }

                if (!ageIndicationExists) {
                    ageWarningRule = productToValidate.ageIndications.firstOrNull {
                        it.isWarning() && it.matchesAge(ageInDays) && it.matchesGender(comparator.patientGender)
                    }?.also {
                        warning = it.warning
                    }
                }
            }

            return ageWarningRule != null
        }
    }

    data class OutOfAgeWarningArgs(
        val patientGender: String?,
        val patientDoB: LocalDate?
    )
}
