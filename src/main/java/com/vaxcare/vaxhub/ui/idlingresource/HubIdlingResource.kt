/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.idlingresource

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import com.vaxcare.vaxhub.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Basic IdlingResource for automated testing.
 * If the Build is not debug, the instance will be null.
 */
class HubIdlingResource : IdlingResource {
    private var resCallback: ResourceCallback? = null
    private val isIdle = AtomicBoolean(true)

    companion object {
        private var _instance: HubIdlingResource? = null
        val instance: HubIdlingResource?
            get() {
                if (_instance == null && BuildConfig.DEBUG) {
                    _instance = HubIdlingResource()
                }
                return _instance
            }
    }

    override fun getName(): String = javaClass.name

    override fun isIdleNow(): Boolean = isIdle.get()

    override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
        resCallback = callback
    }

    fun setIdle(idle: Boolean) {
        isIdle.set(idle)
        if (isIdleNow()) {
            resCallback?.onTransitionToIdle()
        }
    }
}
