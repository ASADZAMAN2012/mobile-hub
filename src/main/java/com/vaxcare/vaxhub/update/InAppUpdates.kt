/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.update

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.vaxcare.core.extensions.safeLaunch
import com.vaxcare.vaxhub.core.constant.Receivers.UPDATE_REQUEST_CODE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class VaxCareUpdate(
    val stagedUpdateAvailable: Boolean = false
)

interface InAppUpdates {
    val availableUpdate: StateFlow<VaxCareUpdate>

    /**
     * Begins the Update Flow with Play Core API
     *
     * @param activity required for the Play Core API
     */
    fun startFlow(activity: Activity)

    /**
     * Trigger PlayCore's update flow
     *
     * @param activity needed if the instance has not been instantiated yet
     */
    fun immediateUpdate(activity: Activity? = null)
}

@Singleton
class InAppUpdatesImpl @Inject constructor() : InAppUpdates {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val _availableUpdate = MutableStateFlow(VaxCareUpdate())
    override val availableUpdate: StateFlow<VaxCareUpdate> = _availableUpdate

    private var instance: VaxCarePlayCoreListener? = null

    override fun startFlow(activity: Activity) {
        if (instance == null) {
            instance = VaxCarePlayCoreListener(activity)
        } else {
            instance?.startNew()
        }
    }

    override fun immediateUpdate(activity: Activity?) {
        instance?.startImmediateUpdate() ?: activity?.let {
            startFlow(it)
            instance?.startImmediateUpdate()
        }
    }

    inner class VaxCarePlayCoreListener(var activity: Activity) {
        private var updater: AppUpdateManager = AppUpdateManagerFactory.create(activity.baseContext)

        init {
            updater.appUpdateInfo.addOnCompleteListener(::onComplete)
        }

        /**
         * Adding the OnCompleteListener will kick off PlayCore's flow for checking for an update
         * availability
         */
        fun startNew() {
            updater.appUpdateInfo.addOnCompleteListener(::onComplete)
        }

        fun startImmediateUpdate() {
            scope.safeLaunch {
                val appUpdateInfo = updater.requestAppUpdateInfo()
                updater.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                    UPDATE_REQUEST_CODE
                )
            }
        }

        /**
         * OnComplete callback hook for the AppUpdateManager
         *
         * @param task completed task with result
         */
        private fun onComplete(task: Task<AppUpdateInfo>) {
            with(activity) {
                scope.safeLaunch {
                    if (!task.isSuccessful) {
                        runOnUiThread {
                            Timber.e(task.exception, "error from update check")
                        }
                        return@safeLaunch
                    }

                    parseUpdateInfoAndEmit(task.result)
                }
            }
        }

        private fun parseUpdateInfoAndEmit(info: AppUpdateInfo) {
            val stagedUpdateAvailable =
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            _availableUpdate.tryEmit(VaxCareUpdate(stagedUpdateAvailable))
        }
    }
}
