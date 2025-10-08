/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.utility

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.view.View
import android.widget.ImageView
import com.vaxcare.vaxhub.common.utility.DrawableUtils.drawableToBitmap
import com.vaxcare.vaxhub.common.utility.DrawableUtils.getTargetBitmap
import com.vaxcare.vaxhub.common.utility.DrawableUtils.getTargetDrawable

object ViewUtils {
    fun View.drawableMatches(drawableResourceId: Int, tintColorResId: Int? = null): Boolean {
        if (this is ImageView) {
            drawable.applyTheme(context.theme)
        } else {
            background.applyTheme(context.theme)
        }
        var expectedBitmap: Bitmap? = null
        return try {
            val expectedDrawable = context.theme.getDrawable(drawableResourceId)
            tintColorResId?.let {
                val tintColorRes = context.getColor(tintColorResId)
                expectedDrawable.setTintList(ColorStateList.valueOf(tintColorRes))
            }
            expectedBitmap = drawableToBitmap(expectedDrawable, this)
            val targetBitmap = getTargetBitmap(this)
            val isSameWithTint = targetBitmap?.sameAs(expectedBitmap) ?: false
            val targetDrawable = getTargetDrawable(this)
            val targetDrawableCopy = targetDrawable.constantState?.newDrawable()?.mutate()
            targetDrawableCopy?.setTintList(null)
            expectedDrawable.setTintList(null)
            val expectedBitmapWithoutTint = drawableToBitmap(expectedDrawable, this)
            val targetBitmapWithoutTint = drawableToBitmap(targetDrawableCopy, this)
            val isSameWithoutTint =
                expectedBitmapWithoutTint?.sameAs(targetBitmapWithoutTint) ?: false
            isSameWithTint && isSameWithoutTint
        } catch (error: OutOfMemoryError) {
            false
        } catch (e: Exception) {
            false
        } finally {
            expectedBitmap?.recycle()
        }
    }
}

object DrawableUtils {
    fun drawableToBitmap(drawable: Drawable?, view: View): Bitmap? {
        return try {
            when (drawable) {
                is GradientDrawable, is VectorDrawable, is BitmapDrawable -> {
                    val bitmap = if (view is ImageView) {
                        Bitmap.createBitmap(
                            view.drawable.intrinsicWidth,
                            view.drawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                    } else {
                        Bitmap.createBitmap(
                            view.measuredWidth,
                            view.measuredHeight,
                            Bitmap.Config.ARGB_8888
                        )
                    }
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)

                    bitmap
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getTargetBitmap(targetView: View): Bitmap? {
        return drawableToBitmap(getTargetDrawable(targetView), targetView)
    }

    fun getTargetDrawable(targetView: View): Drawable {
        return if (targetView is ImageView) targetView.drawable else targetView.background
    }
}
