/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import java.time.LocalDate

sealed class DoseExpiredRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.Expired

    /**
     * Evaluates if a product is expired
     *
     * @property comparator - Lot expiration date
     */
    data class DoseExpiredValidator(override val comparator: LocalDate?) :
        DoseExpiredRules<LocalDate?>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            return comparator != null && LocalDate.now() > comparator
        }
    }
}
