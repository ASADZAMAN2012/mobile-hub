/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.core.extensions.toMilli
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseViewModel() {
    companion object {
        private val TEN_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(10)
    }

    sealed class PinLockState : State {
        data class PinSuccess(val pin: String, val user: User, val shouldUploadLogs: Boolean) : PinLockState()

        data class PinFailed(val pin: String, val retries: Int) : PinLockState()
    }

    private var retries: Int = 0

    fun attemptPinIn(pin: String) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.getUserByPin(pin)?.let { user ->
                    if (user.userId != userRepository.activeUserId) {
                        userRepository.storeActiveUserAndCreateNewUserSessionId(user)
                    }

                    setState(PinLockState.PinSuccess(pin, user, shouldUploadLogs()))
                } ?: setState(PinLockState.PinFailed(pin, retries++))
            } catch (e: Exception) {
                Timber.e(e, "Error pin-in")
                setState(PinLockState.PinFailed(pin, retries++))
            }
        }

    fun needUsersSynced(): Boolean = userRepository.needUsersSynced()

    fun updateAllUsers() =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.forceSyncUsers()
            } catch (e: Exception) {
                Timber.e(e, "Error updating all the users")
            }
        }

    private suspend fun shouldUploadLogs(): Boolean {
        // get previous session
        val prevSessionMillis = userRepository.getLastUserSession()

        // save current session time
        val now = LocalDateTime.now().toMilli()
        userRepository.setLastUserSession(now)

        // return true if pin in 2 times in 10 seconds interval
        return prevSessionMillis > -1 && now - prevSessionMillis <= TEN_SECONDS_MILLIS
    }
}
