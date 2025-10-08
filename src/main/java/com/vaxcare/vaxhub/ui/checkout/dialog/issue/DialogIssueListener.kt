/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog.issue

import android.os.Bundle
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.checkout.ProductIssue

/**
 * Listener interface for handling popped issues
 */
interface DialogIssueListener {
    /**
     * Handle the next issue
     *
     * @param issue ProductIssue popped from the hashmap
     */
    fun handleIssue(issue: ProductIssue)

    /**
     * Handle when the issues from the hashmap are empty/exhausted
     */
    fun onIssuesEmpty()

    /**
     * Handle when the issues from the hashmap are forcibly removed
     */
    fun onCancelIssues()

    /**
     * Handle when a dialog response is received from the UI
     *
     * @param action key of the fragmentResult
     * @param result bundle of goods
     */
    fun onDialogResponse(action: String, result: Bundle)

    /**
     * Handle when the flow needs to be restarted with an updated appointment
     *
     * @param appointment updated appointment associated with reset
     */
    fun onResetWithNewAppointment(appointment: Appointment?)
}
