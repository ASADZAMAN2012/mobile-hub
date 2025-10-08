/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.FcmEventMetric
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.worker.JobSelector
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EarlyEntryPoint
import dagger.hilt.android.EarlyEntryPoints
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@EarlyEntryPoint
@InstallIn(SingletonComponent::class)
interface GlobalMessagingServiceEntryPoint {
    fun getLocalStorage(): LocalStorage

    @MHAnalyticReport
    fun getAnalyticReport(): AnalyticReport
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlobalMessagingServiceLazyEntryPoint {
    fun getJobSelector(): JobSelector
}

@AndroidEntryPoint
class GlobalMessagingService : FirebaseMessagingService() {
    companion object {
        private const val MESSAGE = "eventType"
        private const val EVENT_ID = "eventId"
        private const val PAYLOAD = "payload"

        private const val GENERAL_TOPIC = "vaxcare"

        private var oldClinicId: Long = 0L
        private var oldPartnerId: Long = 0L

        private fun subscribeToGeneralTopic() {
            Timber.d("Subscribing to general topic")
            FirebaseMessaging.getInstance().subscribeToTopic(GENERAL_TOPIC)
                .addOnCompleteListener {
                    if (!it.isSuccessful) {
                        Timber.e(it.exception, "Failure to subscribe to vaxcare topic")
                    } else {
                        Timber.d("Subscribed to general topic")
                    }
                }
                .addOnCanceledListener {
                    Timber.w("General Topic Cancelled")
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "General Topic Failure")
                }
        }

        private fun subscribeToClinicTopic(clinic: Long) {
            Timber.d("Subscribing to clinic topic: $clinic")
            FirebaseMessaging.getInstance().subscribeToTopic("$clinic")
                .addOnCompleteListener {
                    oldClinicId = clinic
                    if (!it.isSuccessful) {
                        Timber.e(it.exception, "Failure to subscribe to clinic topic")
                    } else {
                        Timber.d("Subscribed to clinic topic: $clinic")
                    }
                }
                .addOnCanceledListener {
                    Timber.w("Clinic Topic Cancelled")
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "Clinic Topic Failure")
                }
        }

        private fun subscribeToPartnerTopic(partner: Long) {
            Timber.d("Subscribing to partner topic: $partner")
            FirebaseMessaging.getInstance().subscribeToTopic("$partner")
                .addOnCompleteListener {
                    oldPartnerId = partner
                    if (!it.isSuccessful) {
                        Timber.e(it.exception, "Failure to subscribe to partner topic")
                    } else {
                        Timber.d("Subscribed to partner topic: $partner")
                    }
                }
                .addOnCanceledListener {
                    Timber.w("Partner Topic Cancelled")
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "Partner Topic Failure")
                }
        }

        fun forceTopicRefresh(clinic: Long, partner: Long) {
            Timber.d("Unsubscribing to clinic topic: $oldClinicId")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("$oldClinicId")
                .addOnCompleteListener {
                    if (clinic != 0L) {
                        subscribeToClinicTopic(clinic)
                    }
                }

            Timber.d("Unsubscribing to partner topic: $oldPartnerId")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("$oldPartnerId")
                .addOnCompleteListener {
                    if (partner != 0L) {
                        subscribeToPartnerTopic(partner)
                    }
                }
        }
    }

    private val job: Job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    lateinit var localStorage: LocalStorage
    lateinit var analyticReport: AnalyticReport
    lateinit var jobSelector: JobSelector

    override fun onCreate() {
        super.onCreate()
        subscribeToGeneralTopic()
        val globalMessagingServiceEntryPoint =
            EarlyEntryPoints.get(applicationContext, GlobalMessagingServiceEntryPoint::class.java)
        localStorage = globalMessagingServiceEntryPoint.getLocalStorage()

        val clinic = localStorage.clinicId
        val partner = localStorage.partnerId
        forceTopicRefresh(clinic, partner)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.i("Remote Message: ${remoteMessage.data}")
        val currentEventId: String? = remoteMessage.data[EVENT_ID]
        val currentEventType: String? = remoteMessage.data[MESSAGE]

        val globalMessagingServiceEntryPoint =
            EarlyEntryPoints.get(applicationContext, GlobalMessagingServiceEntryPoint::class.java)
        val lazyMessagingServiceEntryPoint =
            EntryPoints.get(applicationContext, GlobalMessagingServiceLazyEntryPoint::class.java)
        analyticReport = globalMessagingServiceEntryPoint.getAnalyticReport()
        jobSelector = lazyMessagingServiceEntryPoint.getJobSelector()

        scope.launch(Dispatchers.IO) {
            try {
                currentEventType?.let { eventType ->
                    jobSelector.queueJob(eventType, remoteMessage.data[PAYLOAD])
                    analyticReport.saveMetric(
                        FcmEventMetric(currentEventId, eventType)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error executing job")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("onNewToken: $token")
    }

    override fun onDestroy() {
        job.cancel()

        super.onDestroy()
    }
}
