/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.domain.signature.DeleteStaleSignatureUseCase
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.model.LocationData
import com.vaxcare.vaxhub.model.metric.InAppUpdatePromptMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.ClinicRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.UpdateRepository
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.update.InAppUpdates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val clinicRepository: ClinicRepository,
    private val appointmentRepository: AppointmentRepository,
    private val userRepository: UserRepository,
    private val updateRepository: UpdateRepository,
    private val deleteStaleSignatures: DeleteStaleSignatureUseCase,
    private val analyticReport: AnalyticReport,
    private val dispatcherProvider: DispatcherProvider,
    private val localStorage: LocalStorage,
    inAppUpdates: InAppUpdates
) : BaseViewModel(), InAppUpdates by inAppUpdates {
    sealed class SplashState : State {
        data class DataLoaded(
            val location: LocationData? = null,
            val clinics: List<Clinic> = emptyList(),
            val currentClinicId: Long = 0L,
            val parentClinicId: Long = 0L,
            val inAppUpdateEnabled: Boolean = false,
            val timePeriod: TimePeriod,
            val updateSeverity: UpdateSeverity
        ) : SplashState() {
            override fun equals(other: Any?): Boolean =
                when (other) {
                    is DataLoaded -> {
                        this.location == other.location &&
                            this.clinics == other.clinics &&
                            this.currentClinicId == other.currentClinicId &&
                            this.inAppUpdateEnabled == other.inAppUpdateEnabled &&
                            this.timePeriod == other.timePeriod &&
                            this.parentClinicId == other.parentClinicId &&
                            this.updateSeverity == other.updateSeverity
                    }

                    else -> super.equals(other)
                }
        }

        object Error : SplashState()
    }

    private val timerFlow = flow {
        val current = LocalDateTime.now()
        val nextMinute = current.plusMinutes(1)

        var difference = abs(ChronoUnit.MILLIS.between(current, nextMinute)) + 1

        emit(getCurrentTimePeriod())
        while (difference > 0) {
            delay(difference)

            emit(getCurrentTimePeriod())

            difference = MINUTE
        }
    }

    fun saveUpdatePromptMetric(buttonTitle: String, versionName: String) {
        analyticReport.saveMetric(
            InAppUpdatePromptMetric(
                buttonTitlePressed = buttonTitle,
                appVersionName = versionName
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val combinedFlow = timerFlow.flatMapLatest { timePeriod ->
        // this will launch a flow that executes every minute and find if the hour have changed
        flow {
            val state = try {
                val locationData =
                    withContext(dispatcherProvider.default) { locationRepository.getLocationAsync() }
                val clinics =
                    withContext(dispatcherProvider.default) { clinicRepository.getClinics() }
                val clinicId =
                    withContext(dispatcherProvider.default) { clinicRepository.getCurrentClinicId() }
                val parentClinicId =
                    withContext(dispatcherProvider.default) { clinicRepository.getParentClinicId() }
                val inAppUpdateEnabled = withContext(dispatcherProvider.default) {
                    locationRepository.getFeatureFlagByConstant(FeatureFlagConstant.UseInAppUpdates) != null
                }
                val updateSeverityStatus = withContext(dispatcherProvider.default) {
                    val disableOutOfDate = BuildConfig.DEBUG ||
                        locationRepository.getFeatureFlagByConstant(FeatureFlagConstant.DisableOutOfDate) != null
                    val needsUpdateCheck = (
                        localStorage.lastUpdateSeverityFetchDate
                            ?.let { LocalDate.now() > it } ?: true
                    )
                    when {
                        disableOutOfDate -> UpdateSeverityStatus.DISABLED
                        needsUpdateCheck -> UpdateSeverityStatus.ENABLED_OUT_OF_DATE
                        else -> UpdateSeverityStatus.ENABLED
                    }
                }

                val updateSeverity = when (updateSeverityStatus) {
                    UpdateSeverityStatus.DISABLED -> UpdateSeverity.NoAction
                    UpdateSeverityStatus.ENABLED_OUT_OF_DATE -> fetchLatestUpdateSeverity()
                    UpdateSeverityStatus.ENABLED ->
                        localStorage.lastUpdateSeverity ?: UpdateSeverity.NoAction
                }

                SplashState.DataLoaded(
                    location = locationData,
                    clinics = clinics,
                    currentClinicId = clinicId,
                    parentClinicId = parentClinicId,
                    inAppUpdateEnabled = inAppUpdateEnabled,
                    timePeriod = timePeriod,
                    updateSeverity = updateSeverity
                )
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                Timber.e(e, "Exception while Loading Data in SplashViewModel")
                SplashState.Error
            }

            emit(state)
        }
    }

    /**
     * Triggers the ConfigJob directly to retrieve the latest updateSeverity.
     *
     * @return updateSeverity from offline config endpoint
     */
    private suspend fun fetchLatestUpdateSeverity(): UpdateSeverity =
        try {
            updateRepository.getSetupConfigAndStoreUpdateSeverity(isCalledByJob = false).severity
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Timber.e(e, "Exception when fetching the latest update severity")
            localStorage.lastUpdateSeverity ?: UpdateSeverity.NoAction
        }

    fun loadData() =
        viewModelScope.safeLaunch {
            setState(LoadingState)

            // Check whether need to switch clinic ID when you first enter splash every day
            clinicRepository.getCurrentClinicId()

            // load data
            combinedFlow.collectLatest { state ->
                setState(state)
            }
        }

    fun clearStaleSignaturesActiveUserAndSessionId() {
        val deleted = deleteStaleSignatures()
        Timber.i("Deleted $deleted signature files")
        userRepository.clearActiveUserAndSessionId()
    }

    /**
     * Switch the current clinic and re-load the data
     *
     * @param selectedClinic the new selected clinic
     */
    suspend fun switchClinic(selectedClinic: Clinic) {
        if (selectedClinic.id != clinicRepository.getCurrentClinicId()) {
            setState(LoadingState)
            clinicRepository.switchClinicId(selectedClinic.id)
            appointmentRepository.deleteAll()
            locationRepository.refreshLocation()
            appointmentRepository.syncAppointmentsAndSave(
                date = selectedClinic.startDate ?: LocalDate.now()
            )
        }
    }

    /**
     * Returns time period Morning, Afternoon of After-hours base on time ranges
     *
     * @return the time period for the current datetime
     */
    private fun getCurrentTimePeriod(): TimePeriod =
        when (LocalDateTime.now().hour) {
            // integer from 0 to 23

            in 5..11 -> TimePeriod.Morning

            in 12..16 -> TimePeriod.Afternoon

            else -> TimePeriod.AfterHours
        }

    companion object {
        const val MINUTE = 60000L
    }
}

sealed class TimePeriod {
    object Morning : TimePeriod()

    object Afternoon : TimePeriod()

    object AfterHours : TimePeriod()
}

private enum class UpdateSeverityStatus {
    /**
     * Flag is OFF and it has not been 24 hours since last fetch
     */
    ENABLED,

    /**
     * Flag is OFF and it has been > 24 hours since last fetch
     */
    ENABLED_OUT_OF_DATE,

    /**
     * Flag is ON
     */
    DISABLED
}
