/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct

sealed class LarcAddedRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.LarcAdded

    data class LarcAddedValidator(override val comparator: ProductCategory) :
        LarcAddedRules<ProductCategory>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean =
            productToValidate.product.categoryId == comparator
    }
}
