/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

fun ViewGroup.inflate(
    @LayoutRes id: Int
): View {
    return LayoutInflater.from(this.context).inflate(id, this, false)
}

fun ViewGroup.getInflater(): LayoutInflater {
    return LayoutInflater.from(context)
}
