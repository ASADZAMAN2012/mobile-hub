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
import com.vaxcare.vaxhub.model.Clinic

@Dao
abstract class ClinicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(data: List<Clinic>)

    @Transaction
    @Query("SELECT * FROM Clinics")
    abstract suspend fun getClinics(): List<Clinic>

    @Transaction
    @Query("SELECT * FROM Clinics WHERE Id = :id")
    abstract suspend fun getByIdAsync(id: Long): Clinic?

    @Transaction
    @Query("SELECT * FROM Clinics WHERE Id = :id")
    abstract fun getById(id: Long): LiveData<Clinic>

    @Query("DELETE FROM Clinics")
    abstract suspend fun deleteAll()

    @Transaction
    @Insert
    suspend fun insertAll(data: List<Clinic>) {
        deleteAll()
        insert(data)
    }
}
