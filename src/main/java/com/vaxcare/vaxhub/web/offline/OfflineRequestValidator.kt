/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.offline

import com.vaxcare.vaxhub.model.OfflineRequest
import okhttp3.Request

interface OfflineRequestValidator {
    fun validateRequest(request: Request): OfflineRequest?
}
