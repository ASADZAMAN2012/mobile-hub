/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

/**
 * Interface to unify BaseFragment and BaseDialog for handleBack()
 */
interface BackNavigationHandler {
    /**
     * Called by activity, handle custom navigation here. Otherwise navigateUp will
     * be called by default
     *
     * @return true if handled, false if navigateUp must be called
     */
    fun handleBack(): Boolean
}
