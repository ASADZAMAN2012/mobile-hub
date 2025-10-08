/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import com.vaxcare.core.model.SetupConfig
import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.web.WebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

interface UpdateRepository {
    /**
     * Download SetupConfig which contains CodeCorp scanner license
     *
     * @return [SetupConfig]
     */
    suspend fun getSetupConfigAndStoreUpdateSeverity(
        isOffline: Boolean = true,
        isCalledByJob: Boolean = false
    ): SetupConfig
}

class UpdateRepositoryImpl @Inject constructor(
    private val webServer: WebServer,
    private val localStorage: LocalStorage
) : UpdateRepository {
    override suspend fun getSetupConfigAndStoreUpdateSeverity(isOffline: Boolean, isCalledByJob: Boolean): SetupConfig =
        withContext(Dispatchers.IO) {
            val config = webServer.getSetupConfig(
                isOffline = isOffline.toString(),
                isCalledByJob = isCalledByJob
            )

            storeUpdateSeverity(config.severity)
            config
        }

    private fun storeUpdateSeverity(updateSeverity: UpdateSeverity?) {
        localStorage.lastUpdateSeverity = updateSeverity
        localStorage.lastUpdateSeverityFetchDate = LocalDate.now()
    }
}
