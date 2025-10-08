/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Gets value given type T
 *
 * @param time timeout value
 * @param timeUnit timeout unit
 * @param changeEventsCount the amount of changes to observe
 * @param afterObserve function to do work after observation
 * @return
 */
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    changeEventsCount: Int = 1,
    afterObserve: () -> Unit = {}
): T {
    var data: T? = null
    val latch = CountDownLatch(changeEventsCount)
    val observer = Observer<T> { value ->
        data = value
        latch.countDown()
    }

    try {
        this.observeForever(observer)
        afterObserve.invoke()
        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(time, timeUnit)) {
            this.removeObserver(observer)
            throw TimeoutException("LiveData value was never set.")
        }

        @Suppress("UNCHECKED_CAST")
        return data as T
    } catch (e: Exception) {
        throw e
    } finally {
        this.removeObserver(observer)
    }
}
