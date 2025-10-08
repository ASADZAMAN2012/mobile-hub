/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.core.extension.nextMidnight
import com.vaxcare.vaxhub.core.extension.toMillis
import com.vaxcare.vaxhub.service.SessionCleanService
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Using Alarm Manager and this broadcast receiver to trigger the session clean at midnight
 */
@AndroidEntryPoint
class SessionCleanReceiver : BroadcastReceiver() {
    @Inject
    lateinit var service: SessionCleanService

    @Inject
    lateinit var listener: HiltWorkManagerListener

    override fun onReceive(context: Context?, intent: Intent) {
        context?.let {
            val wm = WorkManager.getInstance(it)
            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = wm,
                parameters = OneTimeParams.SessionCacheCleanup,
                listener = listener
            )

            val reschedule =
                intent.getLongExtra(
                    Receivers.RESCHEDULE_EXTRA,
                    LocalDate.now().nextMidnight().toMillis()
                )
            service.scheduleSessionClean(
                context = it,
                executionTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(reschedule),
                    ZoneId.systemDefault()
                )
            )
        }
    }
}
