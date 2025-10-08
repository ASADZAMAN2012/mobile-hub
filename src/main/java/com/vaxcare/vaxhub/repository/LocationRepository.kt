/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.extension.combineWith
import com.vaxcare.vaxhub.data.dao.FeatureFlagDao
import com.vaxcare.vaxhub.data.dao.LocationDao
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.LocationData
import com.vaxcare.vaxhub.web.WebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface LocationRepository {
    /**
     * Get the LocationData stored on the hub
     *
     * @return Live data locationDate
     */
    fun get(): LiveData<LocationData?>

    /**
     * Get ClinicName stored on the hub
     *
     * @return Live Data clinicName
     */
    fun getClinicName(): LiveData<String>

    /**
     * Get PartnerName stored on the hub
     *
     * @return Live Data partnerName
     */
    fun getPartnerName(): LiveData<String>

    /**
     * Insert locationData into the dao
     *
     * @param locationData Data to insert
     */
    suspend fun insert(locationData: LocationData)

    /**
     * Delete live data on the hub
     */
    suspend fun delete()

    /**
     * Refresh the location data
     * Getting the current clinic id request the location data
     *
     * Caching feature flags, location data and delivery days
     */
    suspend fun refreshLocation(isCalledByJob: Boolean = false)

    /**
     * Get all the feature Flags
     *
     * @return a [LiveData] with a [List] of the available [FeatureFlag]
     */
    fun getFeatureFlags(): LiveData<List<FeatureFlag>>

    /**
     * Get the location data async
     *
     * @return the location data or null if is not available
     */
    suspend fun getLocationAsync(): LocationData?

    /**
     * Get all the feature Flags
     *
     * @return a [List] of the available [FeatureFlag]
     */
    suspend fun getFeatureFlagsAsync(): List<FeatureFlag>

    /**
     * Get a [FeatureFlag] by constant if is available
     *
     * @return the [FeatureFlag] or Null if is not available
     */
    suspend fun getFeatureFlagByConstant(constant: FeatureFlagConstant): FeatureFlag?

    /**
     * Get the state of location
     *
     * @return state of clinic
     */
    suspend fun getClinicState(): String

    suspend fun getInventorySourcesAsync(): List<InventorySource>?
}

class LocationRepositoryImpl @Inject constructor(
    private val localStorage: LocalStorage,
    private val webServer: WebServer,
    private val locationDao: LocationDao,
    private val featureFlagDao: FeatureFlagDao
) : LocationRepository {
    override fun get(): LiveData<LocationData?> =
        locationDao.get()
            .combineWith(featureFlagDao.getAll()) { locationData, featureFlagList ->
                locationData?.apply {
                    activeFeatureFlags = featureFlagList ?: emptyList()
                }
            }

    override fun getClinicName(): LiveData<String> = locationDao.getClinicName()

    override fun getPartnerName(): LiveData<String> = locationDao.getPartnerName()

    override suspend fun insert(locationData: LocationData) {
        featureFlagDao.insertAll(locationData.activeFeatureFlags)
        locationDao.insert(locationData)
    }

    override suspend fun delete() = locationDao.delete()

    override suspend fun refreshLocation(isCalledByJob: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val clinicId = localStorage.currentClinicId
                if (clinicId != 0L) {
                    webServer.getLocationData(
                        clinicId = clinicId.toString(),
                        isCalledByJob = isCalledByJob
                    )?.let { locationData ->
                        featureFlagDao.insertAll(locationData.activeFeatureFlags)
                        locationDao.insert(locationData)
                    }
                }
            } catch (e: Exception) {
                Timber.i("Catching and swallowing exception: ${e.message}")
            }
        }
    }

    override fun getFeatureFlags(): LiveData<List<FeatureFlag>> {
        return featureFlagDao.getAll()
    }

    override suspend fun getLocationAsync(): LocationData? =
        try {
            coroutineScope {
                val locationData = withContext(Dispatchers.IO) { locationDao.getAsync() }
                val featureFlags = withContext(Dispatchers.IO) { featureFlagDao.getAllAsync() }

                locationData?.apply {
                    activeFeatureFlags = featureFlags
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting location data")
            null
        }

    override suspend fun getFeatureFlagsAsync(): List<FeatureFlag> {
        return featureFlagDao.getAllAsync()
    }

    override suspend fun getFeatureFlagByConstant(constant: FeatureFlagConstant): FeatureFlag? {
        return featureFlagDao.getByName(constant.value)
    }

    override suspend fun getClinicState(): String = locationDao.getClinicState()

    override suspend fun getInventorySourcesAsync(): List<InventorySource>? = locationDao.getAsync()?.inventorySources
}
