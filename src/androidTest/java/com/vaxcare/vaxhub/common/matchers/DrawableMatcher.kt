/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.matchers

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.view.View
import android.widget.ImageView
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.vaxcare.vaxhub.common.RiskIconConstant
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun withDrawable(resourceId: Int): Matcher<View> {
    return DrawableMatcher(expectedId = resourceId)
}

fun withDrawableWithTintColor(resourceId: Int, tintColor: Int? = null): Matcher<View> {
    return DrawableMatcher(expectedId = resourceId, tintColorResId = tintColor)
}

fun withDrawableWithTintColorByInstrumentation(resource: RiskIconConstant): Matcher<View> {
    return DrawableMatcher(
        expectedId = resource.resourceId,
        isInstrumentation = resource.isInstrumentation,
        tintColorResId = resource.tintColorResId
    )
}

fun withTint(tintColor: Int): Matcher<View> {
    return TintMatcher(tintColor)
}

/**
 * Checked resourceId is from folder: androidTest/res/drawable
 *
 * @param resourceId
 * @return
 */
fun withDrawableByInstrumentation(resourceId: Int, tintColor: Int? = null): Matcher<View> {
    return DrawableMatcher(
        expectedId = resourceId,
        isInstrumentation = true,
        tintColorResId = tintColor
    )
}

class DrawableMatcher(
    private val expectedId: Int,
    private val isInstrumentation: Boolean = false,
    private val tintColorResId: Int? = null
) :
    TypeSafeMatcher<View>(View::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("has Drawable with drawable ID: $expectedId")
    }

    override fun matchesSafely(target: View): Boolean {
        // Applies the specified theme to this Drawable and its children, because the background drawable may use style attributes
        // For example: ?bgWhite / ?cornerRadius
        if (target is ImageView) {
            target.drawable.applyTheme(target.context.theme)
        } else {
            target.background.applyTheme(target.context.theme)
        }

        var expectedBitmap: Bitmap? = null
        return try {
            val expectedDrawable = if (isInstrumentation) {
                getInstrumentation().context.resources.getDrawable(expectedId, null)
            } else {
                target.context.theme.getDrawable(expectedId)
            }
            tintColorResId?.let {
                val tintColorRes = target.context.getColor(tintColorResId)
                expectedDrawable.setTintList(ColorStateList.valueOf(tintColorRes))
            }
            expectedBitmap = drawableToBitmap(expectedDrawable, target)
            val targetBitmap = getTargetBitmap(target)
            val isSameWithTint = targetBitmap?.sameAs(expectedBitmap) ?: false
            val targetDrawable = getTargetDrawable(target)
            val targetDrawableCopy = targetDrawable.constantState?.newDrawable()?.mutate()
            targetDrawableCopy?.setTintList(null)
            expectedDrawable.setTintList(null)
            val expectedBitmapWithoutTint = drawableToBitmap(expectedDrawable, target)
            val targetBitmapWithoutTint = drawableToBitmap(targetDrawableCopy, target)
            val isSameWithoutTint =
                expectedBitmapWithoutTint?.sameAs(targetBitmapWithoutTint) ?: false
            return isSameWithTint && isSameWithoutTint
        } catch (error: OutOfMemoryError) {
            false
        } catch (e: Exception) {
            false
        } finally {
            expectedBitmap?.recycle()
        }
    }

    private fun drawableToBitmap(drawable: Drawable?, view: View): Bitmap? {
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

    private fun getTargetBitmap(targetView: View): Bitmap? {
        return drawableToBitmap(getTargetDrawable(targetView), targetView)
    }

    private fun getTargetDrawable(targetView: View): Drawable {
        return if (targetView is ImageView) targetView.drawable else targetView.background
    }
}

class TintMatcher(private val tintColor: Int) :
    TypeSafeMatcher<View>(View::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("The target button background tint is : $tintColor")
    }

    override fun matchesSafely(target: View): Boolean {
        val expectedTintColor = target.context.getColor(tintColor)
        val targetTintColor = target.backgroundTintList?.getColorForState(
            target.drawableState,
            com.vaxcare.vaxhub.R.color.primary_black
        )
        return expectedTintColor == targetTintColor
    }
}
