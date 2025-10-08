/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProduct

sealed class WrongStockRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue
        get() = ProductIssue.WrongStock

    data class WrongStockValidator(override val comparator: WrongStockArgs) :
        WrongStockRules<WrongStockArgs>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean =
            !isNewLotNumber(comparator.simpleOnHandInventory, productToValidate) &&
                !isProductAvailableInAppointmentStock(
                    productToValidate,
                    comparator.simpleOnHandInventory,
                    comparator.appointmentInventorySource
                )

        private fun isProductAvailableInAppointmentStock(
            productToValidate: LotNumberWithProduct,
            simpleOnHandInventory: List<SimpleOnHandProduct>,
            appointmentInventorySource: InventorySource
        ) = simpleOnHandInventory.filterByLotNumberName(productToValidate.name)
            .filterOnlyPositiveOnHandAmount()
            .map { it.inventorySource }
            .contains(appointmentInventorySource)

        private fun isNewLotNumber(
            simpleOnHandInventory: List<SimpleOnHandProduct>,
            productToValidate: LotNumberWithProduct
        ) = simpleOnHandInventory
            .filterByLotNumberName(productToValidate.name)
            .filterOnlyPositiveOnHandAmount()
            .isEmpty()

        private fun List<SimpleOnHandProduct>.filterOnlyPositiveOnHandAmount(): List<SimpleOnHandProduct> =
            this.filter { it.onHandAmount > 0 }

        private fun List<SimpleOnHandProduct>.filterByLotNumberName(lotNumberName: String): List<SimpleOnHandProduct> =
            this.filter { it.lotNumberName == lotNumberName }
    }

    data class WrongStockArgs(
        val appointmentInventorySource: InventorySource,
        val simpleOnHandInventory: List<SimpleOnHandProduct>
    )
}
