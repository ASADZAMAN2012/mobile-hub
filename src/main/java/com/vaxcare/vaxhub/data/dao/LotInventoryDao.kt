/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaxcare.vaxhub.model.LotInventory

@Dao
abstract class LotInventoryDao {
    @Query("SELECT * FROM LotInventory")
    abstract fun getAll(): LiveData<List<LotInventory>>

    @Query("SELECT * FROM LotInventory WHERE inventorySource = :inventorySource")
    abstract fun findLotInventoryByInventorySource(inventorySource: String): LiveData<List<LotInventory>>

    @Query("SELECT DISTINCT inventorySource from LotInventory")
    abstract fun getInventorySources(): LiveData<List<Int>>

    suspend fun insertAll(lotInventory: List<LotInventory>) {
        deleteAll()
        insertLotInventory(lotInventory)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLotInventory(data: List<LotInventory>)

    @Query("DELETE FROM LotInventory")
    abstract suspend fun deleteAll()
}
