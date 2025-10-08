/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.vaxhub.model.user.SessionUser
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("ktlint:standard:max-line-length")
@Dao
abstract class SessionDao {
    @Query("SELECT * FROM SessionUser WHERE isLocked = 0")
    abstract suspend fun getAllUnlockedAsync(): List<SessionUser>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUser(sessionUser: SessionUser)

    @Query("SELECT * FROM SessionUser WHERE userId = :id")
    abstract suspend fun getUserById(id: Int): SessionUser?

    @Transaction
    open suspend fun upsertUser(sessionUser: SessionUser?) {
        sessionUser?.let {
            getUserById(sessionUser.userId)?.let {
                updateUser(
                    sessionUser.sessionToken,
                    sessionUser.username,
                    it.userId,
                    LocalDateTime.now()
                )
            } ?: run {
                insertUser(sessionUser)
            }
        }
    }

    @Transaction
    @Query(
        "UPDATE SessionUser SET lastLogin = :date, isLocked = 0, userName = :username, sessionToken = :token WHERE userId = :id"
    )
    abstract suspend fun updateUser(
        token: String,
        username: String,
        id: Int,
        date: LocalDateTime
    )

    @Transaction
    @Query("DELETE FROM SessionUser")
    abstract suspend fun deleteAll()

    @Transaction
    @Query("DELETE FROM SessionUser where initialLogin < :date")
    abstract suspend fun cleanupStaleSessions(date: LocalDate)

    @Transaction
    @Query("UPDATE SessionUser SET isLocked = 1 WHERE username = :username")
    abstract suspend fun lockUserByUsername(username: String)

    @Transaction
    @Query("UPDATE SessionUser SET isLocked = 0 WHERE username = :username")
    abstract suspend fun unLockUserByUsername(username: String)
}
