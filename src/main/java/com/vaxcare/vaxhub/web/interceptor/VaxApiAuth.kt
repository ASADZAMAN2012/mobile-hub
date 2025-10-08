/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.interceptor

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.time.Duration
import java.time.Instant

class VaxApiAuth : Authenticator {
    private val token by lazy {
        val algorithm = Algorithm.HMAC256("")
        JWT.create()
            .withIssuer("VaxIdent")
            .withAudience("VaxApi")
            .withClaim("nameid", "VaxCare Admin")
            .withClaim("unique_name", "VaxCare Admin")
            .withClaim("family_name", "atodd")
            .withClaim("given_name", "")
            .withClaim("assign_clinic", "ba5c7b49-9e80-4ca1-b3ff-6c1d7453b7d3")
            .withClaim("partner", "0bfcce6a-ba4d-4693-9073-7a4b3fd48024")
            .withClaim("demo", false)
            .withClaim("compliance_care", false)
            .withClaim("inventory_management", false)
            .withClaim("exp", Instant.now().plus(Duration.ofHours(2)).epochSecond)
            .sign(algorithm)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        Timber.d("Token: $token")

        return response.request.newBuilder()
            .header("Authorization", "bearer $token")
            .header("NodeId", "d164ed94-1d98-470e-8ccf-6b16325e269f")
            .build()
    }
}
