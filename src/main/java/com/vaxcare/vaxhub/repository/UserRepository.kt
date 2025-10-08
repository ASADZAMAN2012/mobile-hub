/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.datadog.android.Datadog
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.UserSyncMetric
import com.vaxcare.core.report.model.UserSyncStatus
import com.vaxcare.core.report.model.UserUpdateMetric
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.storage.preference.UserSessionManager
import com.vaxcare.vaxhub.data.dao.UserDao
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.service.UserSessionService
import com.vaxcare.vaxhub.web.WebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

interface UserRepository {
    val activeUserId: Int

    /**
     * @return all users in dao
     */
    fun getAll(): LiveData<List<User>>

    /**
     * Get single user by User Id
     *
     * @param id User Id to lookup
     * @return Live Data of selected user
     */
    fun getUserById(id: Int): LiveData<User>

    /**
     * Boolean to check localStorage if it has been 1 minute since last sync
     *
     * @return Comparison of now and last sync + 1 minute
     */
    fun needUsersSynced(): Boolean

    /**
     * get User by Id
     *
     * @param id - Id of user
     * @return User with associated Id
     */
    suspend fun getUserAsync(id: Int): User?

    /**
     * get User by UserName
     *
     * @param userName - UserName of the user
     * @return User with associated Id
     */
    suspend fun getUserByUsernameAsync(userName: String): User?

    /**
     * get User by Pin
     *
     * @param pin - Pin of user
     * @return User with associated Pin
     */
    suspend fun getUserByPin(pin: String): User?

    /**
     * Inserts user into the dao
     *
     * @param user - User to insert
     */
    suspend fun insert(user: User)

    /**
     * Inserts a collection of users to the dao
     *
     * @param users - List of users to insert
     */
    suspend fun insertAll(users: List<User>)

    /**
     * Clears the dao of users
     */
    suspend fun deleteAll()

    /**
     * Deletes and inserts list of users. Essentially combining deleteAll and insertAll
     */
    suspend fun deleteAndInsertAll(users: List<User>)

    /**
     * Caches the session user and generates a session ID to send with requests
     *
     * @param user to cache
     */
    fun storeActiveUserAndCreateNewUserSessionId(user: User)

    /**
     * Clears the current user session
     */
    fun clearActiveUserAndSessionId()

    /**
     * Locks the current user session
     */
    fun lockSession()

    /**
     * Unlocks the current user session
     */
    fun unlockSession()

    /**
     * Grabs users from api and forces deleteAndInsertAll to run, and overriding
     * the lastSync in localStorage
     */
    suspend fun forceSyncUsers(isCalledByJob: Boolean = false, rethrowExceptions: Boolean = false)

    /**
     * Get the last user session
     *
     * @return the last user session in millis
     */
    suspend fun getLastUserSession(): Long

    /**
     * Set the last user session
     *
     * @param dateTimeMillis the last user session in millis
     */
    suspend fun setLastUserSession(dateTimeMillis: Long)
}

class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao,
    private val localStorage: LocalStorage,
    private val api: WebServer,
    private val sessionManager: UserSessionManager,
    private val userSessionService: UserSessionService,
    @MHAnalyticReport private val analyticReport: AnalyticReport
) : UserRepository {
    override val activeUserId: Int
        get() = localStorage.userId

    override fun getAll(): LiveData<List<User>> = dao.getAll()

    override fun getUserById(id: Int): LiveData<User> = dao.getUserById(id)

    override fun needUsersSynced(): Boolean {
        val lastSyncDate = localStorage.lastUsersSyncDate
        return lastSyncDate.isNullOrBlank() ||
            LocalDateTime.now().isAfter(
                LocalDateTime.parse(lastSyncDate).plusMinutes(1L)
            )
    }

    override suspend fun getUserAsync(id: Int) = withContext(Dispatchers.IO) { dao.getUserByIdAsync(id) }

    override suspend fun getUserByUsernameAsync(userName: String): User? =
        withContext(Dispatchers.IO) { dao.getUserByUsernameAsync(userName) }

    override suspend fun getUserByPin(pin: String): User? = withContext(Dispatchers.IO) { dao.getUserByPin(pin) }

    override suspend fun insert(user: User) = withContext(Dispatchers.IO) { dao.insertUser(user) }

    override suspend fun insertAll(users: List<User>) = withContext(Dispatchers.IO) { dao.insertAll(users) }

    override suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }

    override suspend fun deleteAndInsertAll(users: List<User>) =
        withContext(Dispatchers.IO) {
            analyticReport.saveMetric(UserUpdateMetric(users.count()))
            dao.deleteAndInsertAll(users)
        }

    override fun storeActiveUserAndCreateNewUserSessionId(user: User) {
        Datadog.setUserInfo("${user.userId}", "${user.firstName} ${user.lastName}", user.userName)
        localStorage.storeUser(
            userId = user.userId,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.userName
        )
        userSessionService.generateAndCacheNewUserSessionId()
    }

    override fun clearActiveUserAndSessionId() {
        Datadog.setUserInfo("", "", "")
        localStorage.clearUser()
        sessionManager.clearUserSession()
    }

    override suspend fun forceSyncUsers(isCalledByJob: Boolean, rethrowExceptions: Boolean) {
        withContext(Dispatchers.IO) {
            getUsersForPartner(
                partnerId = localStorage.partnerId.toString(),
                isCalledByJob = isCalledByJob,
                rethrowExceptions = rethrowExceptions
            )?.let { users ->
                deleteAndInsertAll(users)
                localStorage.lastUsersSyncDate = LocalDateTime.now().toString()
            }
        }
    }

    override suspend fun getLastUserSession(): Long =
        withContext(Dispatchers.IO) {
            localStorage.lastUserSession
        }

    override suspend fun setLastUserSession(dateTimeMillis: Long) =
        withContext(Dispatchers.IO) {
            localStorage.lastUserSession = dateTimeMillis
        }

    private suspend fun getUsersForPartner(
        partnerId: String,
        isCalledByJob: Boolean,
        rethrowExceptions: Boolean
    ): List<User>? =
        withContext(Dispatchers.IO) {
            try {
                api.getUsersForPartner(partnerId, isCalledByJob).also {
                    analyticReport.saveMetric(
                        UserSyncMetric(
                            userSyncStatus = UserSyncStatus.SUCCESS,
                            syncedUsers = it.count()
                        )
                    )
                }
            } catch (e1: HttpException) {
                analyticReport.saveMetric(
                    UserSyncMetric(
                        userSyncStatus = UserSyncStatus.FAIL,
                        syncedUsers = 0,
                        responseCode = e1.code()
                    )
                )
                if (rethrowExceptions) throw e1
                if (e1.code() == 401) {
                    listOf()
                } else {
                    null
                }
            } catch (e: Exception) {
                analyticReport.saveMetric(
                    UserSyncMetric(
                        userSyncStatus = UserSyncStatus.FAIL,
                        syncedUsers = 0
                    )
                )
                Timber.e(e, "Caught exception updating users")
                if (rethrowExceptions) throw e
                null
            }
        }

    override fun lockSession() {
        userSessionService.lockCurrentSession()
    }

    override fun unlockSession() {
        userSessionService.unlockCurrentSession()
    }
}
