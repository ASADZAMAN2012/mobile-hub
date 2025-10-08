/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.model

import android.view.Gravity
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.vaxcare.vaxhub.core.model.enums.DialogSize

data class OverlayProperties(
    @LayoutRes val actionBarResource: Int? = null,
    val showX: Boolean = false,
    val showBack: Boolean = false,
    val showHeader: Boolean = false,
    val headerTitle: String = "",
    val headerGravity: Int = Gravity.CENTER,
    @IdRes val scannerPreview: Int? = null
)

data class DialogProperties(
    @LayoutRes val actionBarResource: Int? = null,
    val adjustKeyboard: Boolean = false,
    val dialogSize: DialogSize,
    @IdRes val scannerViewport: Int? = null,
    @IdRes val scannerPreview: Int? = null
)
