/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.model

import okio.Buffer

/**
 * Wrapper class for a mock request
 *
 * @property endpoint the endpoint for this request i.e. "api/patients/appointment/123?version=2.0"
 * @property requestMethod the requestMethod of this request i.e. "GET"
 * @property requestBody the body of the request
 */
data class MockRequest(
    val endpoint: String,
    val requestMethod: String,
    val requestBody: Buffer? = null
)
