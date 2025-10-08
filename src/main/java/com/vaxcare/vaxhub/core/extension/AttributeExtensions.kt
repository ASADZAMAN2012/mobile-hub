/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

fun Context.getDimenAttribute(
    @AttrRes attr: Int
): Float {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.getDimension(resources.displayMetrics)
}
