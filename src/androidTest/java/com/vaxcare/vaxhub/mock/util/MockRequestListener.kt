/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util

import com.vaxcare.vaxhub.mock.model.MockRequest

/**
 * Interface for analyzing a request in the BaseMockDispatcher before the request is "sent"
 */
fun interface MockRequestListener {
    fun onBeforeRequest(request: MockRequest)
}
