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
import com.vaxcare.vaxhub.model.inventory.LotNumber
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct

@Dao
abstract class LotNumberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(lots: List<LotNumber>)

    @Query("SELECT * FROM LotNumber")
    abstract fun getAll(): LiveData<List<LotNumber>>

    @Query("SELECT * FROM LotNumber WHERE name = :lotNumber")
    abstract suspend fun getLotWithProductById(lotNumber: String): LotNumberWithProduct

    @Transaction
    @Query("SELECT * FROM LotNumber l INNER JOIN Product p ON l.productId = p.Id WHERE p.categoryId in (2,3)")
    abstract fun getAllWithProduct(): LiveData<List<LotNumberWithProduct>>

    @Query("delete from LotNumber")
    abstract fun deleteAll()
}
