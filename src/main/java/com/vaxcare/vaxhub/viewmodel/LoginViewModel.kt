/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.core.model.login.PinLoginResponseDto
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.PinInStatus
import com.vaxcare.core.report.model.PinningStatus
import com.vaxcare.core.report.model.login.PinResetMetric
import com.vaxcare.vaxhub.core.constant.ResponseCodes
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.metric.CheckoutPinMetric
import com.vaxcare.vaxhub.model.user.SessionUser
import com.vaxcare.vaxhub.repository.SessionUserRepository
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.util.launchWithTimeoutIterations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import kotlin.runCatching

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionUserRepository: SessionUserRepository,
    @MHAnalyticReport private val analyticReport: AnalyticReport
) : BaseViewModel() {
    sealed class LoginState : State {
        /**
         * Wait - loading without notifying UI to show loading
         */
        object Idle : LoginState()

        /**
         * Okta returned 401 - invalid credentials
         */
        object BadLogin : LoginState()

        /**
         * Okta returned 423 - account locked
         */
        object AccountLocked : LoginState()

        /**
         * Okta returned 200 - success
         */
        object SuccessfulLogin : LoginState()

        /**
         * Okta/backend is down or the user was not set up correctly
         */
        object OktaError : LoginState()

        data class ResetResult(val isSuccessful: Boolean) : LoginState()

        /**
         * Valid SessionUser list has been fetched
         *
         * @property users list of SessionUser
         */
        data class CachedUsersLoaded(
            val users: List<SessionUser>
        ) : LoginState()

        /**
         * Users are being refreshed from backend
         */
        object UsersRefreshing : LoginState()

        /**
         * Users have been refreshed from backend
         */
        object UsersRefreshed : LoginState()

        /**
         * Error occurred while refreshing users
         */
        object UserRefreshError : LoginState()
    }

    private var timeoutState: State? = null

    companion object {
        const val LOADING_TIMEOUT = 5000L
    }

    init {
        loadingStates.add(LoginState.Idle)
    }

    /**
     * Fetches the list of unlocked cached Users that have logged in successfully today
     */
    fun loadCachedUsers() {
        viewModelScope.launch {
            setState(LoadingState)
            val list = sessionUserRepository.getCachedSessionUsersAsync()
            setState(
                LoginState.CachedUsersLoaded(list)
            )
        }
    }

    /**
     * Sends the POST to identity dev to login to Okta. After a 5 second wait, we will make tell
     * the UI to load
     *
     * @param username email for account
     * @param pin 6-20 character pin for account
     */
    fun loginUser(username: String, pin: String) {
        doWorkAndIdle {
            try {
                val response = sessionUserRepository.loginUser(username, pin)
                val userExists = verifyUserExistsLocally(response.body()?.userId, username)
                sendLoginMetrics(
                    response = response,
                    userExists = userExists,
                    pin = pin,
                    username = username
                )

                val state = if (!userExists && response.code() == ResponseCodes.OK.value) {
                    LoginState.OktaError
                } else {
                    getLoginStateFromResponse(response)
                }

                timeoutState = state
                setState(state)
            } catch (e: Exception) {
                Timber.e(e, "Error occurred while trying to log in!")
                setState(LoginState.OktaError)
                timeoutState = LoginState.OktaError
            }
        }
    }

    /**
     * Mitigate race condition - if the LoadingState and a Response state are emitted at the
     * same time, the hub will not be loading indefinitely
     */
    fun checkFromLoading() {
        timeoutState?.let {
            setState(Reset)
            setState(it)
        }
    }

    fun resetUser(username: String) {
        doWorkAndIdle {
            val state = try {
                val response = sessionUserRepository.resetPin(username)
                sendPinResetMetric(username, response.code())
                LoginState.ResetResult(response.isSuccessful)
            } catch (e: Exception) {
                Timber.e(e)
                sendPinResetMetric(username, 500)
                LoginState.ResetResult(false)
            }

            timeoutState = state
            setState(state)
        }
    }

    /**
     * Get a state derived from incoming response
     *
     * @param response response which the state is derived from
     * @return appropriate state for the UI
     */
    private suspend fun getLoginStateFromResponse(response: Response<PinLoginResponseDto>): State =
        when (response.code()) {
            ResponseCodes.BAD_CREDENTIALS.value -> LoginState.BadLogin
            ResponseCodes.ACCOUNT_LOCKED.value -> LoginState.AccountLocked
            ResponseCodes.OK.value -> response.body()?.let { onSuccessfulLogin(it) }
                ?: LoginState.OktaError

            else -> LoginState.OktaError
        }

    private fun doWorkAndIdle(work: suspend CoroutineScope.() -> Unit) {
        timeoutState = LoadingState
        setState(LoginState.Idle)
        viewModelScope.launchWithTimeoutIterations(
            timeoutLengthMillis = LOADING_TIMEOUT,
            dispatcher = Dispatchers.IO,
            timeoutCallback = { setState(timeoutState ?: LoadingState) },
            work = work
        )
    }

    /**
     * Sends pinin metrics with appropriate response code
     *
     * @param response used with code and userExists to determine if log in was success
     * @param userExists flag for the okta user also exists in the Users table locally
     * @param pin pin used to log in
     */
    private fun sendLoginMetrics(
        response: Response<PinLoginResponseDto>,
        userExists: Boolean,
        pin: String,
        username: String
    ) {
        val responseCode = response.code()
        val success = responseCode == ResponseCodes.OK.value
        val pinningStatus = getPinningStatus(success, userExists)
        analyticReport.saveMetric(
            PinInStatus(
                pinningStatus = pinningStatus,
                pinUsed = pin,
                responseCode = responseCode,
                userFoundLocally = userExists,
                username = username
            ),
            CheckoutPinMetric(isSuccess = success && userExists)
        )
    }

    private fun getPinningStatus(success: Boolean, userExists: Boolean) =
        if (success && userExists) PinningStatus.SUCCESS else PinningStatus.FAIL

    private fun sendPinResetMetric(username: String, responseCode: Int) {
        analyticReport.saveMetric(
            PinResetMetric(
                userName = username,
                responseCode = responseCode
            )
        )
    }

    fun doesEmailExistLocally(username: String) =
        runBlocking(Dispatchers.Default) {
            userRepository.getUserByUsernameAsync(username) != null
        }

    /**
     * Returns true if the incoming user exists in the User table
     *
     * @param userId from login payload
     */
    private suspend fun verifyUserExistsLocally(userId: Int?, userName: String) =
        (
            userId?.let { userRepository.getUserAsync(userId) }
                ?: userRepository.getUserByUsernameAsync(userName)
        ) != null

    private suspend fun onSuccessfulLogin(response: PinLoginResponseDto): State =
        userRepository.getUserAsync(response.userId)?.let { user ->
            if (user.userId != userRepository.activeUserId) {
                userRepository.storeActiveUserAndCreateNewUserSessionId(user)
                analyticReport.updateUserData(user.userId, user.userName.orEmpty(), user.fullName)
            }

            userRepository.unlockSession()
            LoginState.SuccessfulLogin
        } ?: LoginState.OktaError

    fun needUsersSynced(): Boolean = userRepository.needUsersSynced()

    fun updateAllUsers() {
        setState(LoginState.UsersRefreshing)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                userRepository.forceSyncUsers(rethrowExceptions = true)
            }.onSuccess {
                setState(LoginState.UsersRefreshed)
            }.onFailure {
                setState(LoginState.UserRefreshError)
            }
        }
    }
}
