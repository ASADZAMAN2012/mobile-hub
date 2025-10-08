/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProductDTO

@Dao
abstract class SimpleOnHandInventoryDao {
    @Query("SELECT * FROM SimpleOnHandProductDTO")
    abstract fun getAllAsync(): List<SimpleOnHandProductDTO>

    @Transaction
    open suspend fun insertAll(simpleOnHandInventory: List<SimpleOnHandProductDTO>) {
        deleteAll()
        insertLotInventory(simpleOnHandInventory)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLotInventory(simpleOnHandList: List<SimpleOnHandProductDTO>)

    @Query("DELETE FROM SimpleOnHandProductDTO")
    abstract suspend fun deleteAll()
}
