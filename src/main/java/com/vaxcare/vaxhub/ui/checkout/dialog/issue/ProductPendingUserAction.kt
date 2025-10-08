/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog.issue

/**
 * Action that denotes an action the user needs to do in order to move forward during Product issue
 * resolution
 */
enum class ProductPendingUserAction {
    /**
     * The user to be prompted with the Stock Selector
     */
    SET_STOCK,

    /**
     * No action needed, move on immediately
     */
    NONE
}

/**
 * Result from the User based on the previous PendingUserAction
 */
enum class ProductPendingUserResult {
    /**
     * The stock was set successfully
     */
    SET_STOCK_COMPLETE,

    /**
     * The stock was not set and the flow was cancelled
     */
    SET_STOCK_CANCEL,

    /**
     * The UI has the new Med D Info
     */
    MEDD_INFO_APPLIED
}
