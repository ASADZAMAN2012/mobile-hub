/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.view.View

/**
 * This class is for the extension function setOnSingleClickListener
 *
 * The point of this is to prevent someone from spam clicking/touching.
 * This will prevent navigator issues (i.e. TransactionFragment)
 *
 */
class OnSingleClickListener : View.OnClickListener {
    private val listener: View.OnClickListener

    constructor(listener: View.OnClickListener) {
        this.listener = listener
    }

    constructor(listener: (View) -> Unit) {
        this.listener = View.OnClickListener { listener.invoke(it) }
    }

    companion object {
        private const val DELAY = 500L
        private var prevTime = 0L
    }

    override fun onClick(v: View?) {
        val time = System.currentTimeMillis()
        if (time >= prevTime + DELAY) {
            prevTime = time
            listener.onClick(v)
        }
    }
}
