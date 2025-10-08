/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import java.util.UUID

/**
 * Interface for managing the User Session
 */
interface UserSessionService {
    fun generateAndCacheNewUserSessionId()

    fun getCurrentUserSessionId(): UUID?

    fun clearUserSessionId()

    fun lockCurrentSession()

    fun unlockCurrentSession()

    fun isSessionAuthenticated(): Boolean
}
