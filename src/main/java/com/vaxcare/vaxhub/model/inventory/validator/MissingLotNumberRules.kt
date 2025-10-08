/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct

sealed class MissingLotNumberRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.MissingLotNumber

    /**
     * Evaluates when a product has a missing or blank lotNumber
     *
     * @property comparator - lotNumber of product
     */
    data class ProductMissingLotNumberValidator(override val comparator: String?) :
        MissingLotNumberRules<String?>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            return comparator.isNullOrEmpty()
        }
    }
}
