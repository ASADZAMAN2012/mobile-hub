/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker

import androidx.work.WorkRequest
import timber.log.Timber
import javax.inject.Inject

class HiltWorkManagerListenerImpl @Inject constructor() : HiltWorkManagerListener {
    override var handleJobQueued: (WorkRequest) -> Unit = { Timber.d("handleJobQueuedWorkRequest ${it.id}") }

    override fun onJobQueued(request: WorkRequest) = handleJobQueued(request)
}
