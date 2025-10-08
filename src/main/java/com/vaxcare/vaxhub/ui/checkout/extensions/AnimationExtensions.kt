/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.extensions

import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener

fun onAnimationEndCallback(block: () -> Unit) =
    object : AnimationListener {
        override fun onAnimationStart(p0: Animation?) {}

        override fun onAnimationEnd(p0: Animation?) {
            block()
        }

        override fun onAnimationRepeat(p0: Animation?) {}
    }
