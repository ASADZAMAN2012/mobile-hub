/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import java.time.LocalDate

sealed class RequiresCopayReviewRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.CopayRequired

    /**
     * Evaluates if a product needs CopayReview.
     *
     * (original docs)
     * A Copay Review is required when the following criteria is met:
     *  - Message has Med D and check has not run yet
     *  - Product antigen is covered by copay
     *  - Product does not have a copay attached
     *  - Copay has not yet previously been reviewed
     */
    data class ProductRequiresCopayValidator(override val comparator: ProductRequiresCopayRuleArgs) :
        RequiresCopayReviewRules<ProductRequiresCopayRuleArgs>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            return comparator.isMedDVisit && comparator.dos == LocalDate.now() && comparator.medDInfo == null &&
                MedDVaccines.isMedDVaccine(productToValidate.product.antigen)
        }
    }

    /**
     * ProductRequiresCopayValidator arguments
     *
     * @property isMedDVisit This visit has a MedD tag
     * @property medDInfo MedD information from the appointment
     * @property dos Date of Service of the current appointment
     */
    data class ProductRequiresCopayRuleArgs(
        val isMedDVisit: Boolean,
        val medDInfo: MedDInfo?,
        val dos: LocalDate
    )
}
