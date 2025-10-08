/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.vaxhub.model.Location
import com.vaxcare.vaxhub.model.LocationData

@Dao
abstract class LocationDao {
    @Transaction
    @Query("SELECT * FROM Location")
    abstract fun get(): LiveData<LocationData>

    @Transaction
    @Query("SELECT * FROM Location")
    abstract suspend fun getAsync(): LocationData?

    @Query("SELECT clinicName FROM Location")
    abstract fun getClinicName(): LiveData<String>

    @Query("SELECT partnerName FROM Location")
    abstract fun getPartnerName(): LiveData<String>

    @Query("SELECT state FROM Location")
    abstract suspend fun getClinicState(): String

    @Insert
    suspend fun insert(locationData: LocationData) {
        insert(
            Location(
                clinicId = locationData.clinicId,
                partnerId = locationData.partnerId,
                partnerName = locationData.partnerName,
                clinicNumber = locationData.clinicNumber,
                clinicName = locationData.clinicName,
                address = locationData.address,
                city = locationData.city,
                state = locationData.state,
                zipCode = locationData.zipCode,
                primaryPhone = locationData.primaryPhone,
                contactId = locationData.contactId,
                parentClinicId = locationData.parentClinicId,
                inventorySources = locationData.inventorySources,
                integrationType = locationData.integrationType
            )
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(location: Location)

    @Query("DELETE FROM Location")
    abstract suspend fun delete()
}
