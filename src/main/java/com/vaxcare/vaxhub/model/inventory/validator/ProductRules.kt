/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct

interface ProductRules<T> {
    val comparator: T
    val associatedIssue: ProductIssue

    fun validate(productToValidate: LotNumberWithProduct): Boolean
}
