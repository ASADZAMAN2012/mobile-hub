/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util

import com.vaxcare.vaxhub.mock.model.MockRequest

/**
 * Interface for mutating the response from a request before the response is "sent"
 */
fun interface MockMutator {
    fun onBeforeResponse(request: MockRequest, responseBody: String?): String?
}
