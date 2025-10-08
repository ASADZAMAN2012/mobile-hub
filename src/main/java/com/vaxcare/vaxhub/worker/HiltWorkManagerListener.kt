/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker

import androidx.work.WorkRequest

/**
 * Interface for WorkManager queuing. This is in order for tests to play well with Hilt and Workers
 */
interface HiltWorkManagerListener {
    /**
     * Callback for the jobRequest. Override this to consume the WorkRequest.
     */
    var handleJobQueued: (WorkRequest) -> Unit

    /**
     * Wrapper function for the builder to call
     *
     * @param request work request for the job
     */
    fun onJobQueued(request: WorkRequest)
}
