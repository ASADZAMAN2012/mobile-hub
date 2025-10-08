/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.VaccineAdapterDto
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct

sealed class ConflictingProductRules<T> : BaseRules<T>() {
    /**
     * Evaluates if product's LotNumber already exists in the added staged products list.
     * We are excluding pending orders and products marked for deletion.
     * @returns true when the LotNumber is not existing
     */
    class DuplicateLotValidator(override val comparator: ConflictingProductRuleArgs) :
        ConflictingProductRules<ConflictingProductRuleArgs>() {
        override val associatedIssue: ProductIssue = ProductIssue.DuplicateLot

        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            val stagedAddedProducts =
                comparator.stagedProducts.filterIsInstance<VaccineAdapterProductDto>()
                    .filter { !it.isRemoveDoseState() }
            return stagedAddedProducts.any { it.lotNumber == productToValidate.name }
        }
    }

    /**
     * Evaluates if product already exists in the added staged products list.
     * We are excluding pending orders and products marked for deletion.
     * @returns true when the product is not existing
     */
    class DuplicateProductValidator(override val comparator: ConflictingProductRuleArgs) :
        ConflictingProductRules<ConflictingProductRuleArgs>() {
        override val associatedIssue: ProductIssue = ProductIssue.DuplicateProduct

        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            val stagedAddedProducts =
                comparator.stagedProducts.filterIsInstance<VaccineAdapterProductDto>()
                    .filter { !it.isRemoveDoseState() }
            return stagedAddedProducts.any { it.product.id == productToValidate.product.id }
        }
    }

    /**
     * ConflictingProductValidator arguments
     *
     * @property stagedProducts products present in cart
     */
    data class ConflictingProductRuleArgs(
        val stagedProducts: List<VaccineAdapterDto>
    )
}
