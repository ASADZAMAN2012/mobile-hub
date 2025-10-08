/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.core.model.inventory.WrongProductNdcEntity

@Dao
abstract class WrongProductNdcDao {
    @Query("SELECT * FROM WrongProductNdcEntity")
    abstract suspend fun getAsync(): List<WrongProductNdcEntity>

    @Query("SELECT * FROM WrongProductNdcEntity WHERE ndc = :ndc")
    abstract suspend fun findNDC(ndc: String): WrongProductNdcEntity?

    @Transaction
    open suspend fun insertAll(wrongProductNDCList: List<WrongProductNdcEntity>) {
        deleteAll()
        insert(wrongProductNDCList)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(wrongProductNDCList: List<WrongProductNdcEntity>)

    @Query("DELETE FROM WrongProductNdcEntity")
    abstract suspend fun deleteAll()
}
