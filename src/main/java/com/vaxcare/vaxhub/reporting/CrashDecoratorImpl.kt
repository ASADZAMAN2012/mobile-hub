/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.reporting

import com.vaxcare.core.report.crash.CrashDecorator
import com.vaxcare.core.storage.preference.LocalStorage

class CrashDecoratorImpl(
    private val localStorage: LocalStorage
) : CrashDecorator {
    override fun decorateMetrics(details: Map<String, String>): Map<String, String> =
        details.toMutableMap().apply {
            put("partnerId", localStorage.partnerId.toString())
            put("clinicId", localStorage.clinicId.toString())
            put("userId", localStorage.userId.toString())
            put("serialNumber", localStorage.deviceSerialNumber)
        }
}
