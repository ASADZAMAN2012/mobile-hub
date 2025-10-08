/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.offline

import okhttp3.Response

interface OfflineRequestResponseHandler {
    suspend fun handleResponse(response: Response)
}
