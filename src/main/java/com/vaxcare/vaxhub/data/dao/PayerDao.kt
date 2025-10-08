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
import androidx.room.Update
import com.vaxcare.vaxhub.model.Payer
import kotlinx.coroutines.flow.Flow

@Suppress("ktlint:standard:max-line-length")
@Dao
abstract class PayerDao {
    @Transaction
    @Query(
        """SELECT * FROM Payers WHERE id >= 0 AND insuranceName LIKE '%' || :identifier || '%' ORDER BY trim(insuranceName)"""
    )
    abstract fun getPayersByIdentifier(identifier: String): LiveData<List<Payer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(data: List<Payer>)

    @Query("SELECT * FROM Payers WHERE updatedTime IS NOT NULL ORDER BY updatedTime DESC LIMIT 2")
    abstract fun getLastTwoRecentPayers(): LiveData<List<Payer>>

    @Transaction
    @Query("SELECT * FROM Payers WHERE updatedTime IS NOT NULL ORDER BY updatedTime DESC LIMIT 2")
    abstract suspend fun getLastTwoRecentPayersAsync(): List<Payer>?

    @Transaction
    @Query("SELECT * FROM Payers WHERE updatedTime IS NOT NULL ORDER BY updatedTime DESC LIMIT 3")
    abstract fun getLastThreeRecentPayers(): Flow<List<Payer>>

    @Transaction
    @Query("SELECT * FROM Payers WHERE updatedTime IS NOT NULL ORDER BY updatedTime DESC LIMIT 3")
    abstract suspend fun getLastThreeRecentPayersAsync(): List<Payer>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(payer: Payer)

    @Query("DELETE FROM Payers")
    abstract suspend fun deleteAll()

    @Transaction
    @Query("""SELECT * FROM Payers WHERE id >= 0 AND Payers.insuranceName LIKE '%' || :identifier || '%' ORDER BY id""")
    abstract suspend fun getPayersByIdentifierAsync(identifier: String): List<Payer>

    @Query("""SELECT * FROM Payers WHERE insuranceId = :insuranceId""")
    abstract suspend fun getPayerByInsuranceId(insuranceId: Int): Payer?
}
