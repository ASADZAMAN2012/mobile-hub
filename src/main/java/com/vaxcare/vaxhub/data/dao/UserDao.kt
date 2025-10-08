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
import com.vaxcare.vaxhub.model.User

@Dao
abstract class UserDao {
    @Query("SELECT * FROM Users")
    abstract fun getAll(): LiveData<List<User>>

    @Query("SELECT * FROM Users WHERE userId = :id")
    abstract fun getUserById(id: Int): LiveData<User>

    @Query("SELECT * FROM Users WHERE userId = :id")
    abstract suspend fun getUserByIdAsync(id: Int): User?

    @Query("SELECT * FROM Users WHERE UPPER(userName) = UPPER(:userName)")
    abstract suspend fun getUserByUsernameAsync(userName: String): User?

    @Query("SELECT * FROM Users WHERE pin = :pin")
    abstract suspend fun getUserByPin(pin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(data: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUser(data: User)

    @Query("DELETE FROM Users")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun deleteAndInsertAll(data: List<User>) {
        deleteAll()
        insertAll(data)
    }
}
