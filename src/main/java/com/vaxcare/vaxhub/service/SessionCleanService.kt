/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.login.SessionCleanScheduleMetric
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.core.extension.safeLet
import com.vaxcare.vaxhub.core.extension.toMillis
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.receiver.SessionCleanReceiver
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to keep track of when an alarm for clearing the session cache
 * is active and to schedule or cancel the existing
 */
interface SessionCleanService {
    /**
     * Schedule a new alarm for the incoming executionTime. If existing, we should only
     * cancel/re-schedule when the execution time > the previous execution time
     *
     * @param context context for AlarmManager
     * @param executionTime time for execution. This will be recursive and the receiver will add 1
     * day when finishing
     */
    fun scheduleSessionClean(context: Context?, executionTime: LocalDateTime)

    fun cancelPendingSession(context: Context?)
}

@Singleton
class SessionCleanServiceImpl @Inject constructor(
    @MHAnalyticReport private val analyticReport: AnalyticReport
) : SessionCleanService {
    private var existingAlarmRuntime: Long? = null

    override fun scheduleSessionClean(context: Context?, executionTime: LocalDateTime) {
        val executionTimeMillis = executionTime.toMillis()
        context?.let {
            val shouldRescheduleIfExists =
                existingAlarmRuntime?.let { existingRuntime -> executionTimeMillis > existingRuntime }
                    ?: true

            if (shouldRescheduleIfExists) {
                Timber.d("Scheduling clear session alarm for: $executionTime")
                /*
                we have an existing alarm only when existingAlarmRuntime is not null and we are not
                being called from the BroadcastReceiver
                 */
                val existingPresent = existingAlarmRuntime != null
                val followingExecutionTimeMillis = executionTime.plusDays(1).toMillis()
                val pendingIntent = it.buildPendingIntent(followingExecutionTimeMillis)
                (it.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.let { alarmManager ->
                    existingAlarmRuntime = executionTimeMillis
                    pendingIntent?.let { pi ->
                        alarmManager.cancel(pendingIntent)
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            executionTimeMillis,
                            pi
                        )
                    }

                    analyticReport.saveMetric(
                        SessionCleanScheduleMetric(
                            nextScheduled = executionTime,
                            isPreviousScheduledDeleted = existingPresent
                        )
                    )
                }
            }
        }
    }

    override fun cancelPendingSession(context: Context?) {
        safeLet(context, existingAlarmRuntime) { ctx, existing ->
            Timber.d("Cancelling pending clear session alarm")
            val pendingIntent = ctx.buildPendingIntent(existing)
            if (pendingIntent != null) {
                (ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pendingIntent)
            }
            existingAlarmRuntime = null
        }
    }

    private fun Context.buildPendingIntent(nextExecutionTimeMillis: Long?) =
        nextExecutionTimeMillis?.let {
            Intent(applicationContext, SessionCleanReceiver::class.java).let { intent ->
                intent.action = Receivers.SESSION_CLEAN_ACTION
                intent.putExtra(Receivers.RESCHEDULE_EXTRA, nextExecutionTimeMillis)
                PendingIntent.getBroadcast(
                    this,
                    Receivers.SESSION_CLEAN_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            }
        }
}
