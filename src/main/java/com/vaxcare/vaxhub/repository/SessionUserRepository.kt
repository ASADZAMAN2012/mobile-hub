/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import com.vaxcare.core.api.retrofit.LoginApi
import com.vaxcare.core.model.login.PinLoginRequestDto
import com.vaxcare.core.model.login.PinLoginResponseDto
import com.vaxcare.core.model.login.PinResetRequestDto
import com.vaxcare.core.model.login.PinResetResponseDto
import com.vaxcare.vaxhub.core.constant.ResponseCodes
import com.vaxcare.vaxhub.data.dao.SessionDao
import com.vaxcare.vaxhub.data.dao.UserDao
import com.vaxcare.vaxhub.di.MobileVaxHubLogin
import com.vaxcare.vaxhub.model.user.SessionUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

interface SessionUserRepository {
    /**
     * Posts to api/auth/login to okta and returns the response
     */
    suspend fun loginUser(userName: String, pin: String): Response<PinLoginResponseDto>

    /**
     * @return list of SessionUser that are not locked and initial login was today
     */
    suspend fun getCachedSessionUsersAsync(): List<SessionUser>

    /**
     * Clear all "stale" sessions
     *
     * @param dateToNotDeleteAfter the cut off date for what is considered "stale"
     */
    suspend fun clearStaleCachedSessionUsers(dateToNotDeleteAfter: LocalDate = LocalDate.now())

    suspend fun resetPin(userName: String): Response<PinResetResponseDto>

    /**
     * Calls the Ping endpoint to update VHDashboard and the tablets table in the backend
     */
    suspend fun pingVaxCareServer()
}

@Singleton
class SessionUserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    @MobileVaxHubLogin private val loginApi: LoginApi,
    private val sessionDao: SessionDao
) : SessionUserRepository {
    override suspend fun loginUser(userName: String, pin: String): Response<PinLoginResponseDto> {
        val dto = PinLoginRequestDto(userName, pin)
        val response = loginApi.login(dto)

        when (response.code()) {
            ResponseCodes.ACCOUNT_LOCKED.value -> sessionDao.lockUserByUsername(userName)
            ResponseCodes.OK.value -> {
                response.body()?.let { responseBody ->
                    if (userDao.getUserByIdAsync(responseBody.userId) != null) {
                        sessionDao.upsertUser(responseBody.constructSessionUser())
                    }
                }
            }
        }

        return response
    }

    override suspend fun getCachedSessionUsersAsync(): List<SessionUser> {
        return withContext(Dispatchers.IO) {
            sessionDao.getAllUnlockedAsync()
                .filter { it.initialLogin == LocalDate.now() }
                .sortedByDescending { it.lastLogin }
        }
    }

    override suspend fun clearStaleCachedSessionUsers(dateToNotDeleteAfter: LocalDate) {
        sessionDao.cleanupStaleSessions(dateToNotDeleteAfter)
        Timber.d("All stale user sessions prior to $dateToNotDeleteAfter deleted.")
    }

    override suspend fun resetPin(userName: String) = loginApi.resetPin(PinResetRequestDto(userName))

    override suspend fun pingVaxCareServer() {
        loginApi.pingVaxCareServer()
    }

    private fun PinLoginResponseDto?.constructSessionUser(): SessionUser? =
        this?.let {
            with(it) {
                SessionUser(
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    sessionToken = sessionToken,
                    username = userName,
                    lastLogin = LocalDateTime.now(),
                    isLocked = false
                )
            }
        }
}
