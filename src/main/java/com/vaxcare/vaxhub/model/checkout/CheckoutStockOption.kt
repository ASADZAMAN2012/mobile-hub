/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.checkout

import androidx.annotation.StringRes
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.PRIVATE
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.STATE
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.THREE_SEVENTEEN
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_AMERICAN_INDIAN_OR_ALASKA_NATIVE
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_ENROLLED_IN_MEDICAID
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_UNDERINSURED
import com.vaxcare.vaxhub.model.checkout.CheckoutStockOption.VFC_UNINSURED
import com.vaxcare.vaxhub.model.enums.VfcFinancialClass
import timber.log.Timber

enum class CheckoutStockOption(
    @StringRes val displayName: Int
) {
    PRIVATE(R.string.menu_stock_selector_private),
    VFC_ENROLLED_IN_MEDICAID(R.string.menu_stock_selector_vfc_enrolled),
    VFC_UNINSURED(R.string.menu_stock_selector_vfc_uninsured),
    VFC_UNDERINSURED(R.string.menu_stock_selector_vfc_underinsured),
    VFC_AMERICAN_INDIAN_OR_ALASKA_NATIVE(R.string.menu_stock_selector_vfc_american_indian_or_alaska_native),
    STATE(R.string.menu_stock_selector_state),
    THREE_SEVENTEEN(R.string.menu_stock_selector_three_seventeen)
}

fun InventorySource.toCheckoutStockOption(vfcFinancialClass: VfcFinancialClass? = null): CheckoutStockOption =
    when {
        this == InventorySource.PRIVATE -> PRIVATE
        this == InventorySource.VFC &&
            vfcFinancialClass == VfcFinancialClass.V02 -> VFC_ENROLLED_IN_MEDICAID
        this == InventorySource.VFC &&
            vfcFinancialClass == VfcFinancialClass.V03 -> VFC_UNINSURED
        this == InventorySource.VFC &&
            vfcFinancialClass == VfcFinancialClass.V04 -> VFC_AMERICAN_INDIAN_OR_ALASKA_NATIVE
        this == InventorySource.VFC &&
            vfcFinancialClass == VfcFinancialClass.V05 -> VFC_UNDERINSURED
        this == InventorySource.STATE -> STATE
        this == InventorySource.THREE_SEVENTEEN -> THREE_SEVENTEEN
        else -> {
            Timber.e(
                "Unexpected Inventory source or VfcFinancialClass!",
                this,
                vfcFinancialClass
            )
            PRIVATE
        }
    }

fun List<InventorySource>.toCheckoutStockOptions(): Set<CheckoutStockOption> =
    flatMap { inventorySource ->
        when (inventorySource) {
            InventorySource.PRIVATE -> setOf(PRIVATE)
            InventorySource.VFC -> setOf(
                VFC_ENROLLED_IN_MEDICAID,
                VFC_UNINSURED,
                VFC_UNDERINSURED,
                VFC_AMERICAN_INDIAN_OR_ALASKA_NATIVE
            )

            InventorySource.STATE -> setOf(STATE)
            InventorySource.THREE_SEVENTEEN -> setOf(THREE_SEVENTEEN)
            else -> emptySet()
        }
    }.toSet()

fun CheckoutStockOption.toInventorySourceAndFinancialClass(): Pair<Int, String?> =
    when (this) {
        PRIVATE -> InventorySource.PRIVATE.id to null
        VFC_ENROLLED_IN_MEDICAID -> InventorySource.VFC.id to VfcFinancialClass.V02.name
        VFC_UNINSURED -> InventorySource.VFC.id to VfcFinancialClass.V03.name
        VFC_UNDERINSURED -> InventorySource.VFC.id to VfcFinancialClass.V05.name
        VFC_AMERICAN_INDIAN_OR_ALASKA_NATIVE -> InventorySource.VFC.id to VfcFinancialClass.V04.name
        STATE -> InventorySource.STATE.id to null
        THREE_SEVENTEEN -> InventorySource.THREE_SEVENTEEN.id to null
    }
