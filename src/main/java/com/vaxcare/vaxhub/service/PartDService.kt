/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import android.content.Intent
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.tools.Quadruple
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.domain.partd.ConvertPartDCopayToProductUseCase
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.toMedDInfo
import com.vaxcare.vaxhub.model.metric.PartDResultReceivedMetric
import com.vaxcare.vaxhub.model.partd.PartDCopayResponse
import com.vaxcare.vaxhub.model.partd.PartDResponse
import com.vaxcare.vaxhub.repository.AppointmentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Interface for the Part D Singleton Service
 *
 * In charge of providing the MedDInfo for an appointment - to be used by the viewmodel
 */
interface PartDService {
    val medDInfo: Flow<MedDInfo?>

    /**
     * Manually set the current MedDInfo with the incoming medDInfo param
     *
     * @param medDInfo info to use
     */
    fun updateMedDInfo(medDInfo: MedDInfo)

    /**
     * Manually set the current MedDInfo with the incoming intent from the Broadcast Receiver.
     * This function also sends a PartDResultReceivedMetric to analytics
     *
     * @param appointmentId associated appointmentId
     * @param intent bundle from the BroadcastEvent
     * @param medDCta medDCta name from the MedD message of the appointment at the time of creation
     * @param riskAssessmentId riskAssessment ID directly from the above med D cta message
     */
    fun updatePartDResponseFromEvent(
        appointmentId: Int,
        intent: Intent,
        medDCta: String?,
        riskAssessmentId: Int?
    )

    suspend fun setMedDAppointmentIdAndGetInitialInfo(
        appointmentId: Int,
        notCoveredResId: Int?,
        notFoundResId: Int?
    )

    /**
     * Sets the state to Refresh to force a web call
     */
    fun forceRefresh()

    /**
     * Resets the info. To be used at the end of the checkout session
     */
    fun resetMedDInfo()
}

class PartDServiceImpl @Inject constructor(
    private val repository: AppointmentRepository,
    private val convertCopayToProduct: ConvertPartDCopayToProductUseCase,
    @MHAnalyticReport
    private val analytics: AnalyticReport
) : PartDService {
    private var notCoveredResId: Int? = null
    private var notFoundResId: Int? = null
    private var cachedAppointmentId: Int? = null

    private val dataStateFlow: MutableStateFlow<DataState> = MutableStateFlow(DataState.INIT)
    private val appointmentIdFlow: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val cachedPartDResponse: MutableStateFlow<MedDInfo?> = MutableStateFlow(null)
    private val eventPartDResponse: MutableStateFlow<MedDInfo?> = MutableStateFlow(null)

    private enum class DataState {
        INIT,
        REFRESH,
        DONE
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val medDInfo: Flow<MedDInfo?> = combine(
        flow = dataStateFlow,
        flow2 = appointmentIdFlow,
        flow3 = cachedPartDResponse,
        flow4 = eventPartDResponse,
        transform = ::Quadruple
    ).flatMapLatest(::mapLocalOrApiResponse)

    override fun updatePartDResponseFromEvent(
        appointmentId: Int,
        intent: Intent,
        medDCta: String?,
        riskAssessmentId: Int?
    ) {
        intent.getPartDResponseCopays(Receivers.PART_D_COPAYS)?.let { copays ->
            val response = PartDResponse(
                copays = copays,
                patientVisitId = appointmentId
            )
            analytics.saveMetric(
                PartDResultReceivedMetric(
                    totalCopaysReceived = response.copays.size,
                    patientVisitId = appointmentId,
                    medDCta = medDCta,
                    riskAssessmentId = riskAssessmentId
                )
            )
            eventPartDResponse.update {
                response.toMedDInfo(
                    notFoundMessageResId = notFoundResId,
                    notCoveredMessageResId = notCoveredResId,
                    converter = convertCopayToProduct::invoke
                )
            }
        }
    }

    override fun updateMedDInfo(medDInfo: MedDInfo) {
        cachedPartDResponse.update { medDInfo }
    }

    override suspend fun setMedDAppointmentIdAndGetInitialInfo(
        appointmentId: Int,
        notCoveredResId: Int?,
        notFoundResId: Int?
    ) {
        val isNewAppointmentId = cachedAppointmentId != appointmentId
        if (isNewAppointmentId) {
            cachedAppointmentId = appointmentId
            this.notCoveredResId = notCoveredResId
            this.notFoundResId = notFoundResId
            val initialInfo = repository.getPartDCopays(appointmentId)?.toMedDInfo(
                notFoundMessageResId = notFoundResId,
                notCoveredMessageResId = notCoveredResId,
                converter = convertCopayToProduct::invoke
            )
            cachedPartDResponse.update { initialInfo }
            appointmentIdFlow.update { appointmentId }
            dataStateFlow.update { DataState.DONE }
        }
    }

    override fun forceRefresh() {
        dataStateFlow.update { DataState.REFRESH }
    }

    override fun resetMedDInfo() {
        cachedAppointmentId = null
        notCoveredResId = null
        notFoundResId = null
        cachedPartDResponse.update { null }
        appointmentIdFlow.update { null }
        eventPartDResponse.update { null }
        dataStateFlow.update { DataState.INIT }
    }

    /**
     * Map the incoming appointmentId or PartDResponse into a flow. If the PartDResponse is not
     * null, it means we received the data from the FCM event from JobSelector and this will take
     * priority. Otherwise if the appointmentId gets changed, we will fetch the latest from the
     * PartDResponse from the backend
     * @see com.vaxcare.vaxhub.worker.JobSelector
     *
     * @param data refresh flag, appointmentId and PartDResponse
     * @return Flow of MedDInfo
     */
    private suspend fun mapLocalOrApiResponse(data: Quadruple<DataState, Int?, MedDInfo?, MedDInfo?>): Flow<MedDInfo?> {
        val (state, appointmentId, localResponse, eventResponse) = data
        val flow = when (state) {
            // always start with null
            DataState.INIT -> flowOf(null)

            // force fetch response from partD endpoint
            DataState.REFRESH -> appointmentId?.let { id ->
                cachedPartDResponse.update {
                    it ?: repository.getPartDCopays(id)?.toMedDInfo(
                        notFoundMessageResId = notFoundResId,
                        notCoveredMessageResId = notCoveredResId,
                        converter = convertCopayToProduct::invoke
                    )
                }
                dataStateFlow.update { DataState.DONE }
                flowOf()
            } ?: flowOf()

            DataState.DONE -> flowOf(localResponse ?: eventResponse)
        }

        return flow
    }
}

private fun Intent.getPartDResponseCopays(key: String): List<PartDCopayResponse>? =
    extras?.getParcelableArray(key)?.let { array ->
        array.mapNotNull { it as? PartDCopayResponse }
    }
