/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaxcare.vaxhub.model.ShotAdministrator

@Dao
abstract class ShotAdministratorDao {
    @Query("SELECT * FROM ShotAdministrators")
    abstract fun getAll(): LiveData<List<ShotAdministrator>>

    @Query("SELECT * FROM ShotAdministrators WHERE Id = :id ")
    abstract fun getById(id: Int): LiveData<ShotAdministrator?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(shotAdministrators: List<ShotAdministrator>)

    @Query("DELETE FROM ShotAdministrators")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM ShotAdministrators")
    abstract suspend fun getAllAsync(): List<ShotAdministrator>

    @Query("SELECT * FROM ShotAdministrators WHERE Id = :id ")
    abstract suspend fun getByIdAsync(id: Int): ShotAdministrator?
}
