/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

interface ActionBar {
    fun showActionBar(animate: Boolean = true)

    fun hideActionBar(pausing: Boolean = false)
}
