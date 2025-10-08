/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// As onSignalStrengthsChanged() is called only once when we listen. This is not calling when the signal is changing.
// My current practice is to request every 5 seconds
class SignalStrengthHelper {
    companion object {
        private val asyncExecutor by lazy {
            val thread = HandlerThread(SignalStrengthHelper::class.java.simpleName).apply {
                start()
            }
            Handler(thread.looper)
        }
    }

    fun getSignalStrength(context: Context): SignalStrength? {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var listener: SignalStrengthsListener? = null
        val asyncLock = CountDownLatch(1)
        var signal: SignalStrength? = null

        asyncExecutor.post {
            listener = SignalStrengthsListener {
                signal = it
                asyncLock.countDown()
            }

            telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }

        try {
            asyncLock.await(100, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
        }

        listener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }

        return signal ?: telephonyManager.signalStrength
    }

    private class SignalStrengthsListener(
        private val simStateListener: SignalStrengthsListener.(state: SignalStrength) -> Unit
    ) : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            super.onSignalStrengthsChanged(signalStrength)
            if (signalStrength != null) {
                simStateListener.invoke(this, signalStrength)
            }
        }
    }
}
