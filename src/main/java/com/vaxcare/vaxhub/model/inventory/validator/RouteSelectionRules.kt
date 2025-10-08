/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.MMR_II
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.PRO_QUAD
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.VARIVAX
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.Product

private val excludedRouteAntigens = listOf(VARIVAX, PRO_QUAD, MMR_II)

sealed class RouteSelectionRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.RouteSelectionRequired

    data class RouteSelectionRequiredValidator(override val comparator: RouteSelectionArgs) :
        RouteSelectionRules<RouteSelectionArgs>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean =
            comparator.product.needsRouteSelection() &&
                comparator.product.antigen !in excludedRouteAntigens
    }

    data class RouteSelectionArgs(val product: Product)
}
