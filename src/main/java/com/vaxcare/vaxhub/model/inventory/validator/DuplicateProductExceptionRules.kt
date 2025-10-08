/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.VaccineAdapterDto
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class DuplicateProductExceptionRules<T> : BaseRules<T>() {
    companion object {
        const val DUPLICATE_RSV_PRODUCT_ID: Int = 364
        const val RSV_MIN_AGE_IN_DAYS: Int = 210
        const val RSV_MAX_AGE_IN_DAYS: Int = 600
        const val RSV_MAX_DOSES_ALLOWED: Int = 2
    }

    class DuplicateRSVValidator(override val comparator: DuplicateProductExceptionRuleArgs) :
        DuplicateProductExceptionRules<DuplicateProductExceptionRuleArgs>() {
        override val associatedIssue: ProductIssue = ProductIssue.DuplicateProductException

        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            comparator.patientDoB?.let {
                val ageInDays = ChronoUnit.DAYS.between(comparator.patientDoB, LocalDate.now()).toInt()
                val stagedAddedProducts =
                    comparator.stagedProducts.filterIsInstance<VaccineAdapterProductDto>()
                        .filter { !it.isRemoveDoseState() }

                return if (!comparator.isDisableDuplicateRSV &&
                    productToValidate.product.id == DUPLICATE_RSV_PRODUCT_ID &&
                    ageInDays in (RSV_MIN_AGE_IN_DAYS..RSV_MAX_AGE_IN_DAYS)
                ) {
                    stagedAddedProducts.count { it.product.id == productToValidate.product.id } in
                        (1 until RSV_MAX_DOSES_ALLOWED)
                } else {
                    false
                }
            }
            return false
        }
    }

    /**
     * ConflictingProductValidator arguments
     *
     * @property stagedProducts products present in cart
     */
    data class DuplicateProductExceptionRuleArgs(
        val stagedProducts: List<VaccineAdapterDto>,
        val isDisableDuplicateRSV: Boolean,
        val patientDoB: LocalDate?
    )
}
