/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import java.time.LocalDateTime

data class SessionCleanScheduleMetric(
    val nextScheduled: LocalDateTime,
    val previousScheduledDeleted: Boolean
) : BaseMetric() {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().toMutableMap().apply {
            put("nextScheduledRun", nextScheduled.toString())
            put("previousScheduledDeleted", previousScheduledDeleted.toString())
        }
    }
}
