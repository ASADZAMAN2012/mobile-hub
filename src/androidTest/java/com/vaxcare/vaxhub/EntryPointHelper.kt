/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub

import android.app.Activity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import dagger.hilt.android.EntryPointAccessors

/**
 * Interface that is used as a decorator to indicate that an interface is providing
 * dependencies for the lazyEntryPoint delegate
 *
 */
interface HiltEntryPointInterface

/**
 * Interface that is used as a decorator to indicate a class can use the lazyEntryPoint
 * delegate to inject HiltEntryPointInterface dependencies
 *
 */
interface HiltEntryPoint

object EntryPointHelper {
    /**
     * Delegate to grab a lazy HiltEntryPointInterface from a HiltEntryPoint class
     *
     * @param R The entryPoint interface to return
     * @param T The HiltEntryPoint class requesting the HiltEntryPointInterface dependencies
     * @return
     */
    inline fun <reified R : HiltEntryPointInterface, T : HiltEntryPoint> T.lazyEntryPoint(): Lazy<R> {
        return lazy { EntryPointAccessors.fromActivity(getActivity()!!, R::class.java) }
    }

    /**
     * Get the current running automation test activity
     *
     * @return Activity
     */
    fun getActivity(): Activity? {
        var activity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                Stage.RESUMED
            )
            if (resumedActivities.iterator().hasNext()) {
                resumedActivities.iterator().next()?.let {
                    activity = it
                }
            }
        }
        return activity
    }
}
