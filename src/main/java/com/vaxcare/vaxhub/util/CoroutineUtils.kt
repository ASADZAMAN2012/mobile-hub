/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Launches a block of work with an asynchronous timeout block
 *
 * @param timeoutLengthMillis the amount of time to wait between the timeout
 * @param maxIterations sentinel iterations to loop the timeout
 * @param dispatcher dispatcher to use for the block of work
 * @param timeoutCallback callback to be called when a timeout is reached
 * @param work work to be done asynchronously
 */
fun CoroutineScope.launchWithTimeoutIterations(
    timeoutLengthMillis: Long,
    maxIterations: Int = 1,
    dispatcher: CoroutineContext = Dispatchers.Default,
    timeoutCallback: (Int) -> Unit,
    work: suspend CoroutineScope.() -> Unit
) = launch {
    launch(context = dispatcher, block = work)
    var timeoutIteration = 0
    while (timeoutIteration < maxIterations) {
        delay(timeoutLengthMillis)
        timeoutCallback(++timeoutIteration)
    }
}
