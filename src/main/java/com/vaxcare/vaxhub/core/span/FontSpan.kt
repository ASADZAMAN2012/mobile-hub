/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.span

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class FontSpan(private val font: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) {
        applyCustomTypeFace(tp, font)
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        applyCustomTypeFace(textPaint, font)
    }

    private fun applyCustomTypeFace(paint: TextPaint, font: Typeface) {
        paint.typeface = font
    }
}
