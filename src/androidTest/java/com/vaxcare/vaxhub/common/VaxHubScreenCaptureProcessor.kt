/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import java.io.File

class VaxHubScreenCaptureProcessor(parentFolderPath: String) : BasicScreenCaptureProcessor() {
    init {
        this.mDefaultScreenshotPath = File(
            File(
                getExternalStoragePublicDirectory(DIRECTORY_PICTURES),
                "ui-testing"
            ).absolutePath,
            "screenshots/$parentFolderPath"
        )
    }

    override fun getFilename(prefix: String): String = prefix
}
