/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import com.vaxcare.core.model.CodeCorpLicense
import com.vaxcare.core.model.DataDogLicense
import com.vaxcare.core.model.FeatureFlag
import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScannerLicenseMetric
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.args.VaxCareConfigJobArgs
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.UpdateRepository
import kotlinx.coroutines.ensureActive
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Job to check and get the latest configurations from backend
 */
class ConfigJob @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val locationRepository: LocationRepository,
    @MHAnalyticReport private val analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        (parameter as? VaxCareConfigJobArgs)?.let { args ->
            val result =
                try {
                    val setupConfig = updateRepository.getSetupConfigAndStoreUpdateSeverity(isCalledByJob = true)
                    val flags = locationRepository.getFeatureFlagsAsync()
                    val location = locationRepository.getLocationAsync()
                    val featureFlags: MutableList<FeatureFlag> = mutableListOf()
                    for (flag in flags) {
                        featureFlags.add(
                            FeatureFlag(
                                featureFlagId = flag.featureFlagId,
                                clinicId = location?.clinicId ?: -1,
                                featureFlagName = flag.featureFlagName
                            )
                        )
                    }

                    VaxCareConfigResult(
                        codeCorpLicense = setupConfig.codeCorpLicense,
                        dataDogLicense = setupConfig.dataDogLicense,
                        featureFlags = featureFlags,
                        updateSeverity = setupConfig.severity,
                        error = null
                    )
                } catch (e: Exception) {
                    coroutineContext.ensureActive()
                    Timber.e(e, "Error in ConfigJob")
                    VaxCareConfigResult(error = e)
                }

            val success = result.dataDogLicense != null || result.codeCorpLicense != null
            analyticReport.saveMetric(ScannerLicenseMetric(success, success))
            args.completeCallback(result)
        }
    }
}

/**
 * Wrapper for callback parameter
 */
data class VaxCareConfigResult(
    val codeCorpLicense: CodeCorpLicense? = null,
    val dataDogLicense: DataDogLicense? = null,
    val featureFlags: List<FeatureFlag> = emptyList(),
    val updateSeverity: UpdateSeverity? = null,
    val error: Exception?
)
