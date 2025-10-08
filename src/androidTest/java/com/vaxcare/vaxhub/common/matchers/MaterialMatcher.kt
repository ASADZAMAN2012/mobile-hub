/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.matchers

import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vaxcare.vaxhub.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun withBackgroundTintForFloatingActionButton(tintColor: Int): Matcher<View> {
    return FloatingActionButtonBackgroundTintMatcher(tintColor)
}

class FloatingActionButtonBackgroundTintMatcher(private val tintColor: Int) :
    TypeSafeMatcher<View>(View::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("The target button background tint is : $tintColor")
    }

    override fun matchesSafely(target: View): Boolean {
        val expectedTintColor = target.context.getColor(tintColor)
        val targetButton = (target as? FloatingActionButton)
        val targetTintColor = targetButton?.backgroundTintList?.getColorForState(
            targetButton.drawableState,
            R.color.primary_black
        )
        return expectedTintColor == targetTintColor
    }
}
