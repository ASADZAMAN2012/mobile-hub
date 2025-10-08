/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.util

import com.vaxcare.vaxhub.util.MathUtils.exp
import com.vaxcare.vaxhub.util.MathUtils.lerp
import com.vaxcare.vaxhub.util.MathUtils.log
import com.vaxcare.vaxhub.util.MathUtils.norm
import com.vaxcare.vaxhub.util.MathUtils.sq
import com.vaxcare.vaxhub.util.MathUtils.sqrt
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Since Android 9(Pie), the brightness value is not linear any more.
 * For instance, the range for brightness is [0, 255], if we set brightness to 127, the slider in the
 * system dropdown menu will not be in middle(50%), instead it's around 87%.
 * Inside AOSP, it calculates brightness level/percentage in BrightnessController.java.
 * This is how we should handle the brightness logic in our app.
 *
 * Reference
 *  - https://www.stkent.com/2018/11/12/more-on-android-pies-brightness-control-changes.html
 *  - com.android.systemui.settings.BrightnessController
 *  - com.android.settingslib.display.BrightnessUtils
 *  - android.util.MathUtils
 */
object BrightnessUtils {
    const val GAMMA_SPACE_MAX = 1023

    // Hybrid Log Gamma constant values
    private const val R = 0.5f
    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f

    /**
     * A function for converting from the gamma space that the slider works in to the
     * linear space that the setting works in.
     *
     * The gamma space effectively provides us a way to make linear changes to the slider that
     * result in linear changes in perception. If we made changes to the slider in the linear space
     * then we'd see an approximately logarithmic change in perception (c.f. Fechner's Law).
     *
     * Internally, this implements the Hybrid Log Gamma electro-optical transfer function, which is
     * a slight improvement to the typical gamma transfer function for displays whose max
     * brightness exceeds the 120 nit reference point, but doesn't set a specific reference
     * brightness like the PQ function does.
     *
     * Note that this transfer function is only valid if the display's backlight value is a linear
     * control. If it's calibrated to be something non-linear, then a different transfer function
     * should be used.
     *
     * @param value The slider value.
     * @param min The minimum acceptable value for the setting.
     * @param max The maximum acceptable value for the setting.
     * @return The corresponding setting value.
     */
    fun convertGammaToLinear(
        value: Int,
        min: Int = 10,
        max: Int = 255
    ): Int {
        val normalizedVal: Float = norm(0, GAMMA_SPACE_MAX, value)
        val ret: Float
        ret = if (normalizedVal <= R) {
            sq(normalizedVal / R)
        } else {
            exp((normalizedVal - C) / A) + B
        }
        // HLG is normalized to the range [0, 12], so we need to re-normalize to the range [0, 1]
        // in order to derive the correct setting value.
        return lerp(min, max, ret / 12).roundToInt()
    }

    /**
     * A function for converting from the linear space that the setting works in to the
     * gamma space that the slider works in.
     *
     * The gamma space effectively provides us a way to make linear changes to the slider that
     * result in linear changes in perception. If we made changes to the slider in the linear space
     * then we'd see an approximately logarithmic change in perception (c.f. Fechner's Law).
     *
     * Internally, this implements the Hybrid Log Gamma opto-electronic transfer function, which is
     * a slight improvement to the typical gamma transfer function for displays whose max
     * brightness exceeds the 120 nit reference point, but doesn't set a specific reference
     * brightness like the PQ function does.
     *
     * Note that this transfer function is only valid if the display's backlight value is a linear
     * control. If it's calibrated to be something non-linear, then a different transfer function
     * should be used.
     *
     * @param value The brightness setting value.
     * @param min The minimum acceptable value for the setting.
     * @param max The maximum acceptable value for the setting.
     * @return The corresponding slider value
     */
    fun convertLinearToGamma(
        value: Int,
        min: Int = 10,
        max: Int = 255
    ): Int {
        // For some reason, HLG normalizes to the range [0, 12] rather than [0, 1]
        val normalizedVal: Float = norm(min, max, value) * 12
        val ret: Float
        ret = if (normalizedVal <= 1f) {
            sqrt(normalizedVal) * R
        } else {
            A * log(normalizedVal - B) + C
        }
        return lerp(0, GAMMA_SPACE_MAX, ret).roundToInt()
    }
}

object MathUtils {
    fun norm(
        start: Int,
        stop: Int,
        value: Int
    ): Float {
        return (min(stop, max(start, value)) - start).toFloat() / (stop - start)
    }

    fun sq(v: Float): Float {
        return v * v
    }

    fun exp(a: Float): Float {
        return kotlin.math.exp(a.toDouble()).toFloat()
    }

    fun sqrt(a: Float): Float {
        return kotlin.math.sqrt(a.toDouble()).toFloat()
    }

    fun log(a: Float): Float {
        return ln(a.toDouble()).toFloat()
    }

    fun lerp(
        start: Int,
        stop: Int,
        amount: Float
    ): Float {
        return start + (stop - start) * amount
    }
}
