/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui

import com.vaxcare.core.report.crash.BaseUncaughtExceptionHandler
import com.vaxcare.core.report.crash.CrashReport
import kotlin.system.exitProcess

class VaxUncaughtExceptionHandler(private val crashReport: CrashReport) :
    BaseUncaughtExceptionHandler {
    private var mDefaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        crashReport.reportCrash(exception, mapOf())

        // mDefaultUncaughtExceptionHandler is com.microsoft.appcenter.crashes.UncaughtExceptionHandler here
        mDefaultUncaughtExceptionHandler?.uncaughtException(thread, exception)

        // close the app. This is a mandatory call - otherwise the thread will remain frozen
        exitProcess(1)
    }

    override fun register() {
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}
