/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.log

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.vaxcare.core.report.crash.CrashReport
import timber.log.Timber

/**
 *
 * Build logs for Vaxcare Apps, look on build config parameters to define behavior
 *
 * buildConfigField "boolean", "VERBOSE_LOG_ENABLED", "false"
 * buildConfigField "boolean", "DEBUG_LOG_ENABLED", "false"
 * buildConfigField "boolean", "INFO_LOG_ENABLED"
 */
class VaxCareLog(
    private val verboseEnabled: Boolean,
    private val debugEnabled: Boolean,
    private val infoEnabled: Boolean,
    private val buildType: String,
    private val crashReport: CrashReport,
) : Timber.DebugTree() {
    companion object {
        const val RELEASE_BUILD_TYPE = "release"
    }

    /**
     * Write a log message to its destination. Called for all level-specific methods by default.
     *
     * @param priority Log level. See [Log] for constants.
     * @param tag Explicit or inferred tag. May be `null`.
     * @param message Formatted log message. May be `null`, but then `t` will not be.
     * @param t Accompanying exceptions. May be `null`, but then `message` will not be.
     */
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        when (priority) {
            Log.VERBOSE -> logVerbose(tag, message, t)
            Log.DEBUG -> logDebug(tag, message, t)
            Log.INFO -> logInfo(tag, message, t)
            Log.WARN -> logWarn(tag, message, t)
            Log.ERROR -> logError(tag, message, t)
            Log.ASSERT -> logAssert(tag, message, t)
        }
    }

    @SuppressLint("LogNotTimber")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun logVerbose(
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (buildType != RELEASE_BUILD_TYPE && verboseEnabled) {
            Log.v(tag, message, t)
        }
    }

    @SuppressLint("LogNotTimber")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun logDebug(
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (buildType != RELEASE_BUILD_TYPE && debugEnabled) {
            Log.d(tag, message, t)
        }
    }

    @SuppressLint("LogNotTimber")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun logInfo(
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (buildType != RELEASE_BUILD_TYPE && infoEnabled) {
            Log.i(tag, message, t)
        }
    }

    @SuppressLint("LogNotTimber")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun logWarn(
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (buildType != RELEASE_BUILD_TYPE) {
            Log.w(tag, message, t)
        }
    }

    @SuppressLint("LogNotTimber")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun logError(
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        val exception = t ?: Exception(message)
        crashReport.reportException(exception)

        if (buildType != RELEASE_BUILD_TYPE) {
            Log.e(tag, message, t)
        }
    }

    @SuppressLint("LogNotTimber")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun logAssert(
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (buildType != RELEASE_BUILD_TYPE) {
            Log.wtf(tag, message, t)
        }
    }
}
