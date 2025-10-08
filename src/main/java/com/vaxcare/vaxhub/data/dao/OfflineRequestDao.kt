/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.vaxhub.model.OfflineRequest
import com.vaxcare.vaxhub.model.OfflineRequestInfo

@Dao
abstract class OfflineRequestDao {
    // insert transactions
    @Transaction
    @Insert
    fun insert(offlineRequests: List<OfflineRequest>) {
        insertOfflineRequests(offlineRequests)
    }

    // delete transactions
    @Transaction
    @Delete
    suspend fun delete(offlineRequests: List<OfflineRequest>) {
        deleteOfflineRequests(offlineRequests)
    }

    @Transaction
    @Delete
    suspend fun deleteAll() {
        deleteAllOfflineRequests()
    }

    // get transactions
    @Transaction
    @Query("SELECT * FROM OfflineRequest")
    abstract fun getAll(): LiveData<List<OfflineRequest>>

    @Transaction
    @Query("SELECT * FROM OfflineRequest")
    abstract suspend fun getAllAsync(): List<OfflineRequest>?

    @Transaction
    @Query("SELECT id, requestUri, length(requestBody) as bodySize, originalDateTime FROM OfflineRequest")
    abstract suspend fun getOfflineRequestList(): List<OfflineRequestInfo>

    // protected inserts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertOfflineRequests(offlineRequest: List<OfflineRequest>)

    // protected deletes
    @Delete
    protected abstract suspend fun deleteOfflineRequests(appointments: List<OfflineRequest>)

    @Query("DELETE FROM OfflineRequest")
    protected abstract suspend fun deleteAllOfflineRequests()

    @Query("DELETE FROM OfflineRequest WHERE id IN (:ids)")
    abstract suspend fun deleteOfflineRequestsByIds(ids: List<Int>)
}
