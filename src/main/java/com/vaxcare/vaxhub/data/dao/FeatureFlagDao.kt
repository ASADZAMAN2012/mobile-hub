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
import com.vaxcare.vaxhub.model.FeatureFlag

@Dao
abstract class FeatureFlagDao {
    @Query("SELECT * FROM FeatureFlags")
    abstract fun getAll(): LiveData<List<FeatureFlag>>

    @Transaction
    @Query("SELECT * FROM FeatureFlags")
    abstract suspend fun getAllAsync(): List<FeatureFlag>

    @Query("SELECT * FROM FeatureFlags WHERE featureFlagName = :name")
    abstract suspend fun getByName(name: String): FeatureFlag?

    @Transaction
    open suspend fun insertAll(data: List<FeatureFlag>) {
        deleteAll()
        insert(data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(data: List<FeatureFlag>)

    @Query("DELETE FROM FeatureFlags")
    abstract suspend fun deleteAll()
}
