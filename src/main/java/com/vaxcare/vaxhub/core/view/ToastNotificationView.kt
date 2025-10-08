/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.ViewToastNotificationBinding
import com.vaxcare.vaxhub.model.legacy.ToastProperties
import com.vaxcare.vaxhub.ui.checkout.extensions.onAnimationEndCallback
import timber.log.Timber

class ToastNotificationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    companion object {
        const val CLOSING_DELAY: Long = 1000 * 5
        const val DEFAULT_FADE_IN_MILLIS = 500L
    }

    private val toastBinding: ViewToastNotificationBinding

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        toastBinding = ViewToastNotificationBinding.inflate(inflater, this, true)

        toastBinding.close.setOnClickListener {
            hide()
        }
    }

    /**
     * Shows a small custom toast notification in the upper right.  Default is to show the toast
     * persistently.  Include a toastProperties.closeAfterMilliseconds value to have the toast fade
     * out.
     *
     * @param toastProperties
     * @param fadeTimeInMillis
     */
    fun showToast(toastProperties: ToastProperties, fadeTimeInMillis: Long = DEFAULT_FADE_IN_MILLIS) {
        try {
            if (!isShown) {
                val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fadein)
                val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fadeout)
                fadeIn.duration = fadeTimeInMillis

                toastBinding.header.text = toastProperties.header
                toastBinding.message.text = toastProperties.message
                setOnClickListener(toastProperties.onClick)

                show()
                startAnimation(fadeIn)
                toastProperties.closeAfterMilliseconds?.let {
                    fadeOut.duration = fadeTimeInMillis
                    Handler(Looper.getMainLooper())
                        .postDelayed({
                            startAnimation(fadeOut)
                            fadeOut.setAnimationListener(onAnimationEndCallback { hide() })
                        }, it)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun hideToast() {
        hide()
    }
}
