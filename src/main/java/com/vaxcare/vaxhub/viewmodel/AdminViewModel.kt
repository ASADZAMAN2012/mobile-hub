/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.data.AppDatabase
import com.vaxcare.vaxhub.model.LocationData
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.service.GlobalMessagingService
import com.vaxcare.vaxhub.web.WebServer
import com.vaxcare.vaxhub.worker.WorkerBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val localStorage: LocalStorage,
    private val webServer: WebServer,
    private val appDatabase: AppDatabase,
    private val repository: UserRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    var hasError = false
    val setupState = MutableLiveData(AdminSetupState.UNKNOWN)

    init {
        if (localStorage.isTabletSetup()) {
            setupState.postValue(AdminSetupState.SETUP)
        }
    }

    fun validatePassword(password: String, callback: (Boolean) -> Unit) {
        viewModelScope.safeLaunch {
            if (BuildConfig.DEBUG && password == "vxc3") {
                callback(true)
            } else {
                try {
                    val result = webServer.validatePassword(password)
                    callback(result)
                } catch (e: HttpException) {
                    Timber.d(e, "Caught http exception")
                    callback(false)
                } catch (e: IOException) {
                    Timber.e(e, "Catch IO Exception")
                    callback(false)
                } catch (e: Exception) {
                    Timber.e(e, "Unknown Error")
                    callback(false)
                }
            }
        }
    }

    fun setupVaxHub(
        count: Int,
        context: Context,
        callback: (Boolean, Int) -> Unit
    ) {
        Timber.d("Starting initial setup work...")
        val clinicId = localStorage.clinicId
        val partnerId = localStorage.partnerId
        viewModelScope.safeLaunch(Dispatchers.IO) {
            try {
                WorkerBuilder.destroy(context)
                appDatabase.clearAllTables()
                localStorage.resetUserAndSyncDates()
                getUsers(partnerId.toString())?.let {
                    repository.insertAll(it)
                }
                getLocationData(clinicId.toString())?.let {
                    locationRepository.insert(it)
                }

                GlobalMessagingService.forceTopicRefresh(clinicId, partnerId)

                WorkerBuilder.initialize(context)
                callback(true, count + 1)
            } catch (e: Exception) {
                Timber.d("Caught $e")
                callback(false, count + 1)
            }
        }
    }

    private suspend fun getUsers(partnerId: String): List<User>? {
        try {
            return webServer.getUsersForPartner(partnerId)
        } catch (e: Exception) {
            Timber.d("Caught network exception $e")
        }
        return null
    }

    private suspend fun getLocationData(clinicId: String): LocationData? {
        try {
            return webServer.getLocationData(clinicId)
        } catch (e: Exception) {
            Timber.d("Caught network exception $e")
        }
        return null
    }
}

enum class AdminSetupState {
    NOT_SETUP,
    SETUP,
    CONFIGURED,
    UNKNOWN
}
