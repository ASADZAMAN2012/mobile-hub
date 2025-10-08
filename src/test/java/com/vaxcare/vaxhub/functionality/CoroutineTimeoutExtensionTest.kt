/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.util.launchWithTimeoutIterations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CoroutineTimeoutExtensionTest {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    @Test
    fun `couroutine launch with timeout iterations test`() {
        val maxIterations = 5
        val waitTime = 1000L
        var launchedStartMillis = 0L
        var completed = false

        runBlocking {
            scope.launchWithTimeoutIterations(
                maxIterations = maxIterations,
                timeoutLengthMillis = waitTime,
                timeoutCallback = { iteration ->
                    assert(iteration.hasPassedSeconds(launchedStartMillis))
                    completed = iteration == maxIterations
                }
            ) { launchedStartMillis = System.currentTimeMillis() }
            while (!completed) {
                delay((maxIterations + 1) * waitTime)
                assert(completed)
            }
        }
    }

    private fun Int.hasPassedSeconds(startTimeMillis: Long) =
        (System.currentTimeMillis() - startTimeMillis) >= (this * 1000L)
}
