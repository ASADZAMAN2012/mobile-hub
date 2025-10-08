/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.interceptor

import android.content.Context
import android.widget.Toast
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.core.extension.getMainThread
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.net.ssl.SSLPeerUnverifiedException

class WebLogger(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        Timber.v("Starting Intercept")
        val request = chain.request()

        val t1 = System.nanoTime()
        Timber.v(
            "Sending request %s on %s%n%s",
            request.url,
            chain.connection(),
            request.headers
        )

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            val code =
                when (e) {
                    is SocketTimeoutException -> -2
                    is SSLPeerUnverifiedException -> -1
                    else -> -9999
                }
            showResponseToast(code, request.url.toString())
            throw e
        }

        showResponseToast(response.code, response.request.url.toString())
        Timber.v("Response Code: ${response.code}")

        val t2 = System.nanoTime()
        Timber.v(
            "Received response for %s in %.1fms%n%s",
            response.request.url,
            (t2 - t1) / 1e6,
            response.headers
        )

        return response
    }

    private fun showResponseToast(code: Int, url: String) {
        if (BuildConfig.SHOW_HTTP_RESPONSE_TOAST) {
            val msg = String.format("Received %d response for %s", code, url)
            context.getMainThread {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
