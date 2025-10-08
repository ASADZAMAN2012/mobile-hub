/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import androidx.annotation.VisibleForTesting
import com.azure.storage.blob.BlobClientBuilder
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.Database
import com.vaxcare.core.storage.file.FileLogger
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.AppInfo
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.di.MHAnalyticReport
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active job - Called ad hoc or from FCM when eventType is "com.vaxcare.vaxhub.firebase.DIAGNOSTIC"
 * this compiles, zips and sends the DB and logs from the past 3 days
 */
@Singleton
class DiagnosticJob @Inject constructor(
    private val fileLogger: FileLogger,
    private val files: FileStorage,
    private val appInfo: AppInfo,
    @MHAnalyticReport report: AnalyticReport
) : BaseVaxJob(report) {
    companion object {
        const val DIAGNOSTIC_JOB_NAME = "DiagnosticJob"
    }

    override suspend fun doWork(parameter: Any?) {
        Timber.d("Diagnostic job started")
        Timber.d("Compiling files...")
        val srcFiles = compileFiles()
        Timber.d("Zipping files...")
        val zipPath = zipFiles(srcFiles)
        Timber.d("Uploading and deleting zip file...")
        uploadZip(zipPath)
        Timber.d("Diagnostic job completed")
    }

    private fun compileFiles(): List<String> {
        val srcFiles = mutableListOf<String>()

        val logFiles = fileLogger.listOfLogsFiles().map {
            it.absolutePath
        }
        // adding logs files
        srcFiles.addAll(logFiles)

        // Create/get the logcat file
        try {
            val logCat = files.createFile(
                data = null,
                deleteIfExisting = true,
                appInfo.filesDirectoryPath,
                "cat.log"
            )

            Timber.d("Created Logcat File: $logCat")

            val cmd = "logcat -d -f $logCat"
            Runtime.getRuntime().exec(cmd)
            Timber.d("Path to logcat file: $logCat")

            // adding logcat file
            srcFiles.add(logCat)
        } catch (e: IOException) {
            Timber.e(e, "Error creating logcat file: $e")
        }

        // Create/get the db file
        val db =
            "/data/data/${appInfo.packageName}/databases/${Database.DATABASE_NAME}"
        try {
            if (files.fileExists(db)) {
                Timber.d("Found the Database: $db")
                // adding database file
                srcFiles.add(db)
            } else {
                Timber.e(
                    FileNotFoundException("database file do not exists"),
                    "Error finding db file"
                )
            }
        } catch (e: IOException) {
            Timber.e(e, "Error finding db file")
        }

        return srcFiles
    }

    private fun zipFiles(srcFiles: List<String>): String {
        val zipFilePath = files.zipFiles(
            filesToZip = srcFiles,
            appInfo.filesDirectoryPath,
            createFileName(LocalDateTime.now())
        )

        Timber.d("Created the Diagnostics zip: $zipFilePath")
        // create the worker to send the zip file

        Timber.d("DiagnosticZipWorker completed")
        return zipFilePath
    }

    private fun uploadZip(filePath: String?) {
        filePath?.let { path ->
            try {
                val filename = files.getName(path)
                val endpoint =
                    "${BuildConfig.BLOB_STORE_URL}${BuildConfig.BLOB_STORE_CONTAINER}/" +
                        "${filename}${BuildConfig.BLOB_STORE_MIDURL}" +
                        BuildConfig.BLOB_STORE_SAS
                val client = BlobClientBuilder()
                    .endpoint(endpoint)
                    .buildClient()

                client.uploadFromFile(path, true)
                Timber.d("File: $path successfully uploaded.")
                files.deleteFile(filename)

                Timber.d("DiagnosticUploadWorker completed")
            } catch (e: Exception) {
                Timber.e(e, "Error uploading to azure blob storage: ${e.message}")
            }
        } ?: kotlin.run {
            Timber.e(
                Exception("Diagnostic Job Upload Failed: filePath was null"),
                "Diagnostic Job Upload Failed: filePath was null"
            )
        }
    }

    @VisibleForTesting
    fun createFileName(date: LocalDateTime): String = "${appInfo.deviceSerialNumber}_${appInfo.buildVariant}_$date.zip"
}
