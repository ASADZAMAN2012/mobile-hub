/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import androidx.annotation.DrawableRes
import com.vaxcare.vaxhub.R

enum class SignalStrengthLevel(
    @DrawableRes val icon: Int
) {
    GREAT(R.drawable.ic_wifi_great),
    GOOD(R.drawable.ic_wifi_good),
    FAIR(R.drawable.ic_wifi_fair),
    POOR(R.drawable.ic_wifi_poor),
    BAD(R.drawable.ic_wifi_error),
    NO_INTERNET(R.drawable.ic_wifi_no_internet);

    companion object {
        fun fromInt(dBmSignal: Int) =
            when {
                dBmSignal > -45 -> GREAT
                dBmSignal > -67 -> GOOD
                dBmSignal > -70 -> FAIR
                dBmSignal > -80 -> POOR
                else -> BAD
            }
    }
}
