/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable

fun Drawable.setColorFilter(color: Int) {
    this.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
}
