/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.matchers

import android.view.View
import android.widget.TextView
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher

fun withTextIgnoringCase(elements: String): Matcher<View> {
    return withText(Matchers.equalToIgnoringCase(elements))
}

fun withTextOR(vararg elements: String): Matcher<View> {
    return TextORMatcher(elements.toList())
}

class TextORMatcher(private val elements: List<String>) : TypeSafeMatcher<View>(View::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("exist with : $elements")
    }

    override fun matchesSafely(target: View): Boolean {
        val targetText = (target as? TextView)?.text ?: ""
        elements.forEach {
            val matched = targetText.contains(it, ignoreCase = true)
            if (matched) return true
        }
        return false
    }
}
