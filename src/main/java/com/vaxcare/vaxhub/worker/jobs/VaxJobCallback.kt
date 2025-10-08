/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs

import timber.log.Timber

fun interface VaxJobCallback {
    fun onJobFinished(jobName: String, success: Boolean)
}

class VaxJobCallbackImpl : VaxJobCallback {
    override fun onJobFinished(jobName: String, success: Boolean) {
        Timber.d("Vaxjob $jobName finished successfully: $success")
    }
}
