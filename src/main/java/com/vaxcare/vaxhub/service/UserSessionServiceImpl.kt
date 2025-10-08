/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import com.vaxcare.core.storage.preference.LocalStorage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionServiceImpl @Inject constructor(
    private val localStorage: LocalStorage
) : UserSessionService {
    private var isSessionLocked = false

    override fun generateAndCacheNewUserSessionId() {
        localStorage.createNewUserSession()
    }

    override fun getCurrentUserSessionId(): UUID? = localStorage.getCurrentUserSessionId()

    override fun clearUserSessionId() {
        localStorage.clearUserSession()
    }

    override fun lockCurrentSession() {
        isSessionLocked = true
    }

    override fun unlockCurrentSession() {
        isSessionLocked = false
    }

    override fun isSessionAuthenticated(): Boolean = !isSessionLocked && getCurrentUserSessionId() != null
}
