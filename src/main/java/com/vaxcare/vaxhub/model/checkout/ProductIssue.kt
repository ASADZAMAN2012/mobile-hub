/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.checkout

import android.os.Parcelable
import com.vaxcare.vaxhub.model.inventory.AgeWarning
import kotlinx.parcelize.Parcelize

/**
 * All possible issues wrong with a product.
 *
 * @property weight weight used for sorting a collection of Weighable
 */
@Suppress("ParcelCreator")
@Parcelize
sealed class ProductIssue(
    override val weight: Int
) : ProductIssueBase(), Parcelable {
    object DuplicateProductException : ProductIssue(weight = DUPLICATE_PRODUCT_EXCEPTION_WEIGHT)

    object DuplicateProduct : ProductIssue(weight = DUPLICATE_PRODUCT_WEIGHT)

    object DuplicateLot : ProductIssue(weight = DUPLICATE_LOT_WEIGHT)

    object LarcAdded : ProductIssue(weight = LARC_ADDDED_WEIGHT)

    object Unordered : ProductIssue(weight = UNORDERED_WEIGHT)

    object WrongStock : ProductIssue(weight = WRONG_STOCK_WEIGHT)

    object CopayRequired : ProductIssue(weight = MEDD_CHECK_WEIGHT)

    object ProductNotCovered : ProductIssue(weight = NOT_COVERED_WEIGHT)

    data class OutOfAgeWarning(
        val title: String?,
        val message: String?,
        val promptType: AgeWarning.PromptType?
    ) : ProductIssue(weight = OUT_OF_AGE_WARNING)

    object OutOfAgeIndication : ProductIssue(weight = OUT_OF_AGE_WEIGHT)

    object Expired : ProductIssue(weight = EXPIRED_WEIGHT)

    object RestrictedProduct : ProductIssue(weight = RESTRICTED_PRODUCT_WEIGHT)

    object MissingLotNumber : ProductIssue(weight = MISSING_LOT_WEIGHT)

    object RouteSelectionRequired : ProductIssue(weight = ROUTE_REQUIRED_WEIGHT)

    companion object {
        private const val DUPLICATE_PRODUCT_EXCEPTION_WEIGHT = 9
        private const val DUPLICATE_PRODUCT_WEIGHT = 10
        private const val DUPLICATE_LOT_WEIGHT = 11
        private const val LARC_ADDDED_WEIGHT = 12
        private const val UNORDERED_WEIGHT = 20
        private const val WRONG_STOCK_WEIGHT = 25
        private const val MEDD_CHECK_WEIGHT = 30
        private const val NOT_COVERED_WEIGHT = 40
        private const val OUT_OF_AGE_WARNING = 45
        private const val OUT_OF_AGE_WEIGHT = 50
        private const val EXPIRED_WEIGHT = 70
        private const val RESTRICTED_PRODUCT_WEIGHT = 71
        private const val MISSING_LOT_WEIGHT = 90
        private const val ROUTE_REQUIRED_WEIGHT = 1000
    }
}
