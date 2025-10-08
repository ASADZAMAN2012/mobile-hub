/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vaxcare.vaxhub.model.DoorSensorEventModel
import java.time.Instant

@Dao
abstract class DoorSensorEventDao {
    @Query("SELECT * FROM DoorSensorEventModel")
    abstract fun getAll(): LiveData<List<DoorSensorEventModel>>

    @Query("SELECT * FROM DoorSensorEventModel WHERE id = :id")
    abstract fun getModelById(id: Int): LiveData<DoorSensorEventModel>

    @Query(
        "SELECT * FROM DoorSensorEventModel WHERE createdDate >= :start " +
            "AND createdDate <= :end"
    )
    abstract fun getModelsFromRange(start: Instant, end: Instant): LiveData<DoorSensorEventModel>

    @Insert
    abstract suspend fun insertAll(data: List<DoorSensorEventModel>)

    @Insert
    abstract suspend fun insertModel(data: DoorSensorEventModel)
}
