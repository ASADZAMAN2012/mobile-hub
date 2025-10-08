/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.domain.signature

import android.net.Uri
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.vaxhub.AppInfo
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase that will save content with "SIGN_" prefix
 *
 * @property fileStorage MP lib for file storage operations
 * @property appInfo object for referencing the files directory
 */
@Singleton
class SaveSignatureUseCase @Inject constructor(
    private val fileStorage: FileStorage,
    private val appInfo: AppInfo
) {
    /**
     * @return URI string of incoming content's newly created file
     */
    operator fun invoke(content: String): String {
        val fileName = "SIGN_${LocalDate.now()}_${UUID.randomUUID()}"
        val path = fileStorage.createFile(
            data = content.byteInputStream(),
            deleteIfExisting = true,
            appInfo.fileDirectory!!.absolutePath,
            fileName
        )

        return Uri.fromFile(File(path)).toString()
    }
}
