/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.model

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

data class FragmentProperties(
    @LayoutRes val resource: Int,
    val hasMenu: Boolean = false,
    val hasToolbar: Boolean = true,
    @LayoutRes val actionBarResource: Int? = null,
    val actionBarStartVisible: Boolean = false,
    @IdRes val scannerPreview: Int? = null,
    @IdRes val scannerSearchIcon: Int? = null,
    @IdRes val scannerViewport: Int? = null,
    val canOpenDrawer: Boolean = false,
    val showControlPanel: Boolean = false,
    val showStatusBarIcons: Boolean = false
)
