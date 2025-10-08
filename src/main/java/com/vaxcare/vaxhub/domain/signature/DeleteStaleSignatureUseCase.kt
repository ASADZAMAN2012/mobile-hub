/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.domain.signature

import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.vaxhub.AppInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteStaleSignatureUseCase @Inject constructor(
    private val fileStorage: FileStorage,
    private val appInfo: AppInfo
) {
    private val signaturePrefix = "SIGN_"

    operator fun invoke() =
        fileStorage.deleteAllFilesWithCondition(
            listOf(appInfo.fileDirectory!!.absolutePath)
        ) { file -> file.name.startsWith(signaturePrefix) }
}
