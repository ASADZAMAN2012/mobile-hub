/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.matchers

import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import com.google.common.base.Preconditions.checkNotNull
import junit.framework.AssertionFailedError
import org.hamcrest.Matcher
import org.hamcrest.StringDescription
import java.util.Locale

/**
 * If need to wait for the view to match, throw this error during the waiting time
 * @property message
 */
class WaitingNotMatchViewException(override val message: String) : RuntimeException()

fun waitMatches(viewMatcher: Matcher<View>): ViewAssertion {
    return WaitMatchesViewAssertion(checkNotNull(viewMatcher))
}

@VisibleForTesting
class WaitMatchesViewAssertion(private val viewMatcher: Matcher<View>) : ViewAssertion {
    private val tag = WaitMatchesViewAssertion::class.java.simpleName

    override fun check(view: View?, noViewException: NoMatchingViewException?) {
        val description = StringDescription()
        description.appendText("'")
        viewMatcher.describeTo(description)
        if (noViewException != null) {
            description.appendText(
                String.format(
                    Locale.ROOT,
                    "' check could not be performed because view '%s' was not found.\n",
                    noViewException.viewMatcherDescription
                )
            )
            Log.e(tag, description.toString())
            throw noViewException
        } else {
            try {
                description.appendText("' doesn't match the selected view.")
                ViewMatchers.assertThat(description.toString(), view, viewMatcher)
            } catch (e: AssertionFailedError) {
                throw WaitingNotMatchViewException("doesn't match the selected view.")
            }
        }
    }

    override fun toString(): String {
        return String.format(Locale.ROOT, "MatchesViewAssertion{viewMatcher=%s}", viewMatcher)
    }
}
