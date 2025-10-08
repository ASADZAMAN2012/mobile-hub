/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy.enums

import com.vaxcare.core.model.enums.InventorySource

enum class TransactionType(
    val id: Int,
    val label: String,
    val header: String,
    val columnText: String,
    val lotColumnText: String,
    val delta: Int
) {
    TRANSFER_IN(11, "Transfer In", "Transfer In from %1\$s", "Transferred", "Transfer", 1),
    RECEIVE_DELIVERY(15, "Receive Delivery", "Receive", "Received", "Receive", 1),
    VAX_BUY(31, "VaxBuy", "VaxBuy", "Added", "Add", 1),
    TRANSFER_OUT(12, "Transfer Out", "Transfer Out to %1\$s", "Transferred", "Transfer", -1),
    LOSS_WASTE(13, "Loss/Waste", "Loss/Waste", "Loss/Waste", "Loss/Waste", -1),
    RETURN(16, "Return Doses", "Return Doses", "Returned", "Return", -1),
    FIRST_COUNT(0, "Count", "Count Confirmation", "Confirmed", "Confirm", 1),
    COUNT(0, "Count", "Count Confirmation", "Confirmed", "Confirm", 1);

    fun isCount() = this == COUNT || this == FIRST_COUNT

    fun isTransferBetweenStocks(destination: InventorySource?) =
        isTransfer() && destination != InventorySource.ANOTHER_LOCATION &&
            destination != InventorySource.ANOTHER_STOCK

    fun isTransfer() = this == TRANSFER_IN || this == TRANSFER_OUT
}
