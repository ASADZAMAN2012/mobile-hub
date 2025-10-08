/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaxcare.vaxhub.model.Provider

@Dao
abstract class ProviderDao {
    @Query("SELECT * FROM Providers")
    abstract fun getAll(): LiveData<List<Provider>>

    @Query("SELECT * FROM Providers")
    abstract suspend fun getAllAsync(): List<Provider>

    @Query("SELECT * FROM Providers WHERE Id = :id ")
    abstract fun getById(id: Int): LiveData<Provider>

    @Query("SELECT * FROM Providers WHERE Id = :id ")
    abstract suspend fun getByIdAsync(id: Int): Provider?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(providers: List<Provider>)

    @Query("DELETE FROM Providers")
    abstract suspend fun deleteAll()
}
