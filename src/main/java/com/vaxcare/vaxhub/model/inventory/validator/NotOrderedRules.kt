/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.order.OrderEntity

sealed class NotOrderedRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.Unordered

    data class ProductNotOrderedValidator(override val comparator: ProductNotOrderedRuleArgs) :
        NotOrderedRules<ProductNotOrderedRuleArgs>() {
        /**
         * Evaluates when a product's salesProductId is not contained in any of the appointment's
         * ORM unlinked orders
         */
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            return comparator.rprd &&
                !comparator.orders.filter { it.patientVisitId == null }
                    .any {
                        it.satisfyingProductIds.contains(productToValidate.salesProductId)
                    }
        }
    }

    /**
     * ProductNotOrderedValidator arguments
     *
     * @property rprd - RightPatientRightDose FeatureFlag is on
     * @property orders - Orders attached to the appointment
     */
    data class ProductNotOrderedRuleArgs(
        val rprd: Boolean,
        val orders: List<OrderEntity>
    )
}
