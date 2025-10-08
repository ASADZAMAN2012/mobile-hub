/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog.issue

import android.os.Bundle
import com.vaxcare.vaxhub.core.extension.pop
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import timber.log.Timber

/**
 * State Machine for handling ProductIssues.
 *
 * This uses a DialogIssueListener as a middle-man for communication between UI,VM and this.
 *
 * @see ProductIssue
 * @see DialogIssueListener
 */
class DialogIssueMachine {
    private val issues = mutableSetOf<ProductIssue>()
    private var listener: DialogIssueListener? = null
    private var actionAfterIssuesResolved: () -> Unit = {}

    /**
     * Clears and add all ProductIssues to the HashSet. Additionally sets the listener.
     *
     * @param issuesToRegister issues to add to the hashset.
     * @param listener DialogIssueListener for handling issue pops
     */
    fun registerIssues(issuesToRegister: Set<ProductIssue>, listener: DialogIssueListener) {
        this.listener = listener
        issues.apply {
            clear()
            addAll(issuesToRegister.sorted())
        }
    }

    /**
     * Pops the next issue from the HashSet and feeds it into the listener.
     * If the HashSet is empty, notify the listener the issues are exhausted
     */
    fun getNextDialogIssue() {
        Timber.i("Getting next issue from list: ${issues.map { it::class.java.simpleName }}")
        if (issues.size > 0) {
            listener?.handleIssue(issues.pop())
        } else {
            actionAfterIssuesResolved()
            listener?.onIssuesEmpty()
            actionAfterIssuesResolved = {}
            listener = null
        }
    }

    /**
     * Clears the issues and notify the listener. Immediately set listener to null after call.
     */
    fun cancelPendingIssues() {
        actionAfterIssuesResolved = {}
        issues.clear()
        listener?.onCancelIssues()
        listener = null
    }

    /**
     * Notify the listener that a result was received from the fragment.
     *
     * @param action key of the FragmentResult
     * @param result fragment result from the associated key
     */
    fun notifyResultReceived(action: String, result: Bundle) {
        listener?.onDialogResponse(action, result)
    }

    /**
     * Forcibly add an issue to the HashSet.
     * This is specific for some cases where a dialog issue's result would determine a new issue
     * is now needed.
     *
     * @param issue associated ProductIssue to add
     */
    fun forceAddIssue(issue: ProductIssue) {
        val newList = (issues + issue).sorted()
        issues.apply {
            clear()
            addAll(newList)
        }
    }

    /**
     * Forcibly remove issues from the HashSet.
     * This is specific for some cases where a dialog issue's result would determine an existing
     * issue is no longer relevant.
     *
     * @param issuesToRemove associated vararg of ProductIssue to remove
     */
    fun forceRemoveIssues(vararg issuesToRemove: ProductIssue) {
        issues -= issuesToRemove.toSet()
    }

    fun addActionAfterIssuesResolved(function: () -> Unit) {
        actionAfterIssuesResolved = function
    }

    fun resetWithNewAppointment(appointment: Appointment?) {
        listener?.onResetWithNewAppointment(appointment)
    }
}
