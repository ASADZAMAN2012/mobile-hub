/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.config

import com.vaxcare.core.config.VaxCareConfig
import com.vaxcare.core.config.VaxCareConfigListener
import com.vaxcare.core.config.VaxCareConfigResult
import com.vaxcare.core.model.CodeCorpLicense
import com.vaxcare.core.model.DataDogLicense
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.di.args.VaxCareConfigJobArgs
import com.vaxcare.vaxhub.worker.jobs.hub.ConfigJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

class VaxCareConfigImpl @Inject constructor(
    private val configJob: ConfigJob,
) : VaxCareConfig {
    override var codeCorplicense: CodeCorpLicense = CodeCorpLicense(
        type = "offline",
        customerId = BuildConfig.DEFAULT_SCANNER_CUSTOMER_ID,
        key = BuildConfig.DEFAULT_SCANNER_KEY,
        expiration = LocalDateTime.now()
    )
    override var dataDogLicense: DataDogLicense = DataDogLicense(
        applicationId = BuildConfig.DATADOG_APPLICATION_ID,
        clientToken = BuildConfig.DATADOG_CLIENT_TOKEN,
        enabled = true,
        rumSampleRate = BuildConfig.DATADOG_RUM_SAMPLING_RATE.toFloat(),
        sessionReplaySampleRate = BuildConfig.DATADOG_SESSION_REPLAY_SAMPLING_RATE.toFloat(),
        site = BuildConfig.DATADOG_SITE
    )

    private val job: Job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var listeners: MutableSet<VaxCareConfigListener?> = mutableSetOf()

    private fun isDataDogUpdated(fetchedDataDog: DataDogLicense): Boolean =
        dataDogLicense.enabled != fetchedDataDog.enabled ||
            dataDogLicense.site != fetchedDataDog.site ||
            dataDogLicense.rumSampleRate != fetchedDataDog.rumSampleRate ||
            dataDogLicense.sessionReplaySampleRate != fetchedDataDog.sessionReplaySampleRate ||
            dataDogLicense.clientToken != fetchedDataDog.clientToken ||
            dataDogLicense.applicationId != fetchedDataDog.applicationId

    private fun isCodeCorpUpdated(fetchedCodeCorp: CodeCorpLicense): Boolean =
        codeCorplicense.key != fetchedCodeCorp.key ||
            codeCorplicense.type != fetchedCodeCorp.type ||
            codeCorplicense.expiration != fetchedCodeCorp.expiration ||
            codeCorplicense.customerId != fetchedCodeCorp.customerId

    override fun registerListener(listener: VaxCareConfigListener?) {
        listeners.add(listener)
    }

    override fun refresh() {
        val args = VaxCareConfigJobArgs { result ->
            result.error?.let {
                val error = result.error
                Timber.e(error)
                for (listener in listeners) {
                    listener?.onFetchFailure(error)
                }
            } ?: run {
                var dataDogUpdated = false
                var codeCorpUpdated = false
                result.dataDogLicense?.let {
                    dataDogUpdated = isDataDogUpdated(result.dataDogLicense)
                    if (dataDogUpdated) {
                        dataDogLicense = result.dataDogLicense
                    }
                }
                result.codeCorpLicense?.let {
                    codeCorpUpdated = isCodeCorpUpdated(result.codeCorpLicense)
                    if (codeCorpUpdated) {
                        codeCorplicense = result.codeCorpLicense
                    }
                }
                val vaxCareConfigResult = VaxCareConfigResult()
                vaxCareConfigResult.isCodeCorpUpdated = codeCorpUpdated
                vaxCareConfigResult.isDataDogUpdated = dataDogUpdated
                vaxCareConfigResult.featureFlags = (result.featureFlags ?: listOf())

                for (listener in listeners) {
                    listener?.onFetchSuccess(vaxCareConfigResult)
                }
            }
        }
        scope.launch { configJob.doWork(args) }
    }
}
