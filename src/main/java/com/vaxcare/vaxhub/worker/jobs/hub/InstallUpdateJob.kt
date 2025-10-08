/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.di.MHAnalyticReport
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallUpdateJob @Inject constructor(
    @ApplicationContext private val context: Context,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        Timber.d("Do the thing")
        (parameter as? String)?.let { filesDir ->
            Timber.d("Do the thing")
            val file = File(filesDir, "vaxhub.apk")
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            intent.setDataAndType(
                uri,
                "application/vnd.android.package-archive"
            )

            context.startActivity(intent)
        }
    }
}
