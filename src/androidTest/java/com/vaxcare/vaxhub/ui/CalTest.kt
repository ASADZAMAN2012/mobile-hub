package com.vaxcare.vaxhub.ui


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CalTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(PermissionsActivity::class.java)

    @Test
    fun calTest() {
        val materialTextView = onView(
            allOf(
                withId(R.id.admin_access_action), withText("Login"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.control_panel),
                        0
                    ),
                    29
                ),
                isDisplayed()
            )
        )
        materialTextView.perform(click())

        val appCompatEditText = onView(
            allOf(
                withId(R.id.password_input),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatEditText.perform(replaceText("vxc3"), closeSoftKeyboard())

        val materialButton = onView(
            allOf(
                withId(R.id.btn_login), withText("Log In"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    6
                ),
                isDisplayed()
            )
        )
        materialButton.perform(click())

        val materialButton2 = onView(
            allOf(
                withId(R.id.btn_enter_partner_id), withText("Enter Partner ID"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                        1
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton2.perform(click())

        val materialTextView2 = onView(
            allOf(
                withId(R.id.button_one), withText("1"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialTextView2.perform(click())

        val materialTextView3 = onView(
            allOf(
                withId(R.id.button_seven), withText("7"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    14
                ),
                isDisplayed()
            )
        )
        materialTextView3.perform(click())

        val materialTextView4 = onView(
            allOf(
                withId(R.id.button_eight), withText("8"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    16
                ),
                isDisplayed()
            )
        )
        materialTextView4.perform(click())

        val materialTextView5 = onView(
            allOf(
                withId(R.id.button_seven), withText("7"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    14
                ),
                isDisplayed()
            )
        )
        materialTextView5.perform(click())

        val materialTextView6 = onView(
            allOf(
                withId(R.id.button_six), withText("6"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    12
                ),
                isDisplayed()
            )
        )
        materialTextView6.perform(click())

        val materialTextView7 = onView(
            allOf(
                withId(R.id.button_four), withText("4"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    8
                ),
                isDisplayed()
            )
        )
        materialTextView7.perform(click())

        val appCompatImageView = onView(
            allOf(
                withId(R.id.button_enter),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    23
                ),
                isDisplayed()
            )
        )
        appCompatImageView.perform(click())

        val materialButton3 = onView(
            allOf(
                withId(R.id.btn_enter_clinic_id), withText("Enter Clinic ID"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                        1
                    ),
                    10
                ),
                isDisplayed()
            )
        )
        materialButton3.perform(click())

        val materialTextView8 = onView(
            allOf(
                withId(R.id.button_eight), withText("8"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    16
                ),
                isDisplayed()
            )
        )
        materialTextView8.perform(click())

        val materialTextView9 = onView(
            allOf(
                withId(R.id.button_nine), withText("9"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    18
                ),
                isDisplayed()
            )
        )
        materialTextView9.perform(click())

        val materialTextView10 = onView(
            allOf(
                withId(R.id.button_five), withText("5"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    10
                ),
                isDisplayed()
            )
        )
        materialTextView10.perform(click())

        val materialTextView11 = onView(
            allOf(
                withId(R.id.button_three), withText("3"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    6
                ),
                isDisplayed()
            )
        )
        materialTextView11.perform(click())

        val materialTextView12 = onView(
            allOf(
                withId(R.id.button_four), withText("4"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    8
                ),
                isDisplayed()
            )
        )
        materialTextView12.perform(click())

        val appCompatImageView2 = onView(
            allOf(
                withId(R.id.button_enter),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.keypad),
                        0
                    ),
                    23
                ),
                isDisplayed()
            )
        )
        appCompatImageView2.perform(click())

        val appCompatImageView3 = onView(
            allOf(
                withId(R.id.toolbar_icon),
                childAtPosition(
                    allOf(
                        withId(R.id.constraintLayout_toolbar),
                        childAtPosition(
                            withId(R.id.toolbar),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatImageView3.perform(click())

        val constraintLayout = onView(
            allOf(
                withId(R.id.container),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        constraintLayout.perform(click())

        val materialButton4 = onView(
            allOf(
                withId(R.id.button_six), withText("6"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.lock_keypad),
                        0
                    ),
                    13
                ),
                isDisplayed()
            )
        )
        materialButton4.perform(click())

        val materialButton5 = onView(
            allOf(
                withId(R.id.button_eight), withText("8"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.lock_keypad),
                        0
                    ),
                    17
                ),
                isDisplayed()
            )
        )
        materialButton5.perform(click())

        val materialButton6 = onView(
            allOf(
                withId(R.id.button_six), withText("6"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.lock_keypad),
                        0
                    ),
                    13
                ),
                isDisplayed()
            )
        )
        materialButton6.perform(click())

        val materialButton7 = onView(
            allOf(
                withId(R.id.button_four), withText("4"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.lock_keypad),
                        0
                    ),
                    9
                ),
                isDisplayed()
            )
        )
        materialButton7.perform(click())

        val imageView = onView(
            allOf(
                withId(R.id.toolbar_icon),
                withParent(
                    allOf(
                        withId(R.id.constraintLayout_toolbar),
                        withParent(withId(R.id.top_bar))
                    )
                ),
                isDisplayed()
            )
        )
        imageView.check(matches(isDisplayed()))

        val textView = onView(
            allOf(
                withId(R.id.toolbar_title), withText("Select Patient"),
                withParent(withParent(withId(R.id.constraintLayout_toolbar))),
                isDisplayed()
            )
        )
        textView.check(matches(withText("Select Patient")))

        val imageView2 = onView(
            allOf(
                withId(R.id.right_icon1),
                withParent(
                    allOf(
                        withId(R.id.constraintLayout_toolbar),
                        withParent(withId(R.id.top_bar))
                    )
                ),
                isDisplayed()
            )
        )
        imageView2.check(matches(isDisplayed()))

        val imageView3 = onView(
            allOf(
                withId(R.id.right_icon2),
                withParent(
                    allOf(
                        withId(R.id.constraintLayout_toolbar),
                        withParent(withId(R.id.top_bar))
                    )
                ),
                isDisplayed()
            )
        )
        imageView3.check(matches(isDisplayed()))

        val imageView4 = onView(
            allOf(
                withId(R.id.calendar_icon),
                withParent(withParent(withId(R.id.calendar_item_container))),
                isDisplayed()
            )
        )
        imageView4.check(matches(isDisplayed()))

        val textView2 = onView(
            allOf(
                withId(R.id.date_time), withText("Thursday, Aug. 7th"),
                withParent(
                    allOf(
                        withId(R.id.calendar_item_container),
                        withParent(withId(R.id.calendar_item))
                    )
                ),
                isDisplayed()
            )
        )
        textView2.check(matches(withText("Thursday, Aug. 7th")))

        val imageButton = onView(
            allOf(
                withId(R.id.fab_add),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                isDisplayed()
            )
        )
        imageButton.check(matches(isDisplayed()))

        val imageButton2 = onView(
            allOf(
                withId(R.id.fab_lookup),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                isDisplayed()
            )
        )
        imageButton2.check(matches(isDisplayed()))

        val imageButton3 = onView(
            allOf(
                withId(R.id.fab_lookup),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                isDisplayed()
            )
        )
        imageButton3.check(matches(isDisplayed()))

        val linearLayout = onView(
            allOf(
                withId(R.id.calendar_item_container),
                childAtPosition(
                    allOf(
                        withId(R.id.calendar_item),
                        childAtPosition(
                            withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                            3
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        linearLayout.perform(click())

        val imageView5 = onView(
            allOf(
                withId(R.id.navigate_back),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        imageView5.check(matches(isDisplayed()))

        val imageView6 = onView(
            allOf(
                withId(R.id.navigate_forward),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        imageView6.check(matches(isDisplayed()))

        val textView3 = onView(
            allOf(
                withId(R.id.current_month), withText("August 2025"),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        textView3.check(matches(withText("August 2025")))

        val textView4 = onView(
            allOf(
                withId(R.id.sunday), withText("S"),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        textView4.check(matches(withText("S")))

        val textView5 = onView(
            allOf(
                withId(R.id.monday), withText("M"),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        textView5.check(matches(withText("M")))

        val viewGroup = onView(
            allOf(
                withId(R.id.calendar_item),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                isDisplayed()
            )
        )
        viewGroup.check(matches(isDisplayed()))

        val gridView = onView(
            allOf(
                withId(R.id.rv_month),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        gridView.check(matches(isDisplayed()))

        val textView6 = onView(
            allOf(
                withId(R.id.date), withText("30"),
                withParent(
                    allOf(
                        withId(R.id.root),
                        withParent(withId(R.id.rv_month))
                    )
                ),
                isDisplayed()
            )
        )
        textView6.check(matches(withText("30")))

        val textView7 = onView(
            allOf(
                withId(R.id.date), withText("27"),
                withParent(
                    allOf(
                        withId(R.id.root),
                        withParent(withId(R.id.rv_month))
                    )
                ),
                isDisplayed()
            )
        )
        textView7.check(matches(withText("27")))

        val textView8 = onView(
            allOf(
                withId(R.id.date), withText("27"),
                withParent(
                    allOf(
                        withId(R.id.root),
                        withParent(withId(R.id.rv_month))
                    )
                ),
                isDisplayed()
            )
        )
        textView8.check(matches(withText("27")))

        val appCompatImageView4 = onView(
            allOf(
                withId(R.id.navigate_forward),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.calendar_picker),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView4.perform(click())

        val appCompatImageView5 = onView(
            allOf(
                withId(R.id.navigate_forward),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.calendar_picker),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView5.perform(click())

        val appCompatImageView6 = onView(
            allOf(
                withId(R.id.navigate_forward),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.calendar_picker),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView6.perform(click())

        val imageView7 = onView(
            allOf(
                withId(R.id.navigate_forward),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        imageView7.check(matches(isDisplayed()))

        val imageView8 = onView(
            allOf(
                withId(R.id.navigate_forward),
                withParent(withParent(withId(R.id.calendar_picker))),
                isDisplayed()
            )
        )
        imageView8.check(matches(isDisplayed()))
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
