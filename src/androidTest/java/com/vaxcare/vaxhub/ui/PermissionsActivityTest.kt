package com.vaxcare.vaxhub.ui


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PermissionsActivityTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(PermissionsActivity::class.java)

    @Test
    fun permissionsActivityTest() {
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

        val view = onView(
            allOf(
                withId(R.id.click_area),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.recycler_view),
                        1
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        view.perform(click())

        val appCompatImageView4 = onView(
            allOf(
                withId(R.id.patient_checkout_lot_search_btn),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.patient_info_container),
                        1
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView4.perform(click())

        val appCompatEditText2 = onView(
            allOf(
                withId(R.id.lot_search_et),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText2.perform(replaceText("VAR"), closeSoftKeyboard())

        val recyclerView = onView(
            allOf(
                withId(R.id.lot_results_rv),
                childAtPosition(
                    withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                    3
                )
            )
        )
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val materialButton8 = onView(
            allOf(
                withId(R.id.button_continue), withText("Yes, Keep Dose"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.dialog_content),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton8.perform(click())

        val floatingActionButton = onView(
            allOf(
                withId(R.id.fab_next),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        val recyclerView2 = onView(
            allOf(
                withId(R.id.recycler_view),
                childAtPosition(
                    withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                    1
                )
            )
        )
        recyclerView2.perform(actionOnItemAtPosition<ViewHolder>(1, click()))

        val recyclerView3 = onView(
            allOf(
                withId(R.id.rv_bottom),
                childAtPosition(
                    withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                    4
                )
            )
        )
        recyclerView3.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val floatingActionButton2 = onView(
            allOf(
                withId(R.id.fab_next),
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
        floatingActionButton2.perform(click())

        val materialTextView13 = onView(
            allOf(
                withId(R.id.patient_edit_no_insurance_card), withText("No Insurance Card"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    8
                ),
                isDisplayed()
            )
        )
        materialTextView13.perform(click())

        val materialButton9 = onView(
            allOf(
                withId(R.id.button_ok), withText("OK"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton9.perform(click())

        val frameLayout = onView(
            allOf(
                withId(R.id.continueBtn),
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
        frameLayout.perform(click())

        val materialButton10 = onView(
            allOf(
                withId(R.id.button_ok), withText("OK"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton10.perform(click())

        val materialButton11 = onView(
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
        materialButton11.perform(click())

        val materialButton12 = onView(
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
        materialButton12.perform(click())

        val materialButton13 = onView(
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
        materialButton13.perform(click())

        val materialButton14 = onView(
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
        materialButton14.perform(click())

        val appCompatImageView5 = onView(
            allOf(
                withId(R.id.checkout_btn),
                childAtPosition(
                    allOf(
                        withId(R.id.layout_patient_consent),
                        childAtPosition(
                            withId(R.id.layout_patient_bottom),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView5.perform(click())

        val materialTextView14 = onView(
            allOf(
                withId(R.id.check_out_another), withText("Check Out Another Patient"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        materialTextView14.perform(click())

        val materialButton15 = onView(
            allOf(
                withId(R.id.button_continue), withText("Retry"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.dialog_content),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton15.perform(click())

        val materialButton16 = onView(
            allOf(
                withId(R.id.button_delete), withText("Acknowledge"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.dialog_content),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        materialButton16.perform(click())

        val materialButton17 = onView(
            allOf(
                withId(R.id.button_continue), withText("Retry"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.dialog_content),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton17.perform(click())

        val materialButton18 = onView(
            allOf(
                withId(R.id.button_delete), withText("Acknowledge"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.dialog_content),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        materialButton18.perform(click())

        val materialButton19 = onView(
            allOf(
                withId(R.id.button_delete), withText("Acknowledge"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.dialog_content),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        materialButton19.perform(click())

        val view2 = onView(
            allOf(
                withId(R.id.click_area),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.recycler_view),
                        1
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        view2.perform(click())

        val appCompatImageView6 = onView(
            allOf(
                withId(R.id.toolbar_icon),
                childAtPosition(
                    allOf(
                        withId(R.id.constraintLayout_toolbar),
                        childAtPosition(
                            withId(R.id.top_bar),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatImageView6.perform(click())

        val view3 = onView(
            allOf(
                withId(R.id.click_area),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.recycler_view),
                        0
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        view3.perform(click())

        val appCompatImageView7 = onView(
            allOf(
                withId(R.id.patient_checkout_lot_search_btn),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.patient_info_container),
                        1
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView7.perform(click())

        val appCompatEditText3 = onView(
            allOf(
                withId(R.id.lot_search_et),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText3.perform(replaceText("VAR"), closeSoftKeyboard())

        val recyclerView4 = onView(
            allOf(
                withId(R.id.lot_results_rv),
                childAtPosition(
                    withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                    3
                )
            )
        )
        recyclerView4.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val materialButton20 = onView(
            allOf(
                withId(R.id.button_continue), withText("Yes, Keep Dose"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.dialog_content),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton20.perform(click())

        val floatingActionButton3 = onView(
            allOf(
                withId(R.id.fab_next),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        floatingActionButton3.perform(click())

        val recyclerView5 = onView(
            allOf(
                withId(R.id.recycler_view),
                childAtPosition(
                    withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                    1
                )
            )
        )
        recyclerView5.perform(actionOnItemAtPosition<ViewHolder>(1, click()))

        val recyclerView6 = onView(
            allOf(
                withId(R.id.rv_bottom),
                childAtPosition(
                    withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                    4
                )
            )
        )
        recyclerView6.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val floatingActionButton4 = onView(
            allOf(
                withId(R.id.fab_next),
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
        floatingActionButton4.perform(click())

        val materialTextView15 = onView(
            allOf(
                withId(R.id.patient_edit_no_insurance_card), withText("No Insurance Card"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    8
                ),
                isDisplayed()
            )
        )
        materialTextView15.perform(click())

        val materialButton21 = onView(
            allOf(
                withId(R.id.button_ok), withText("OK"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton21.perform(click())

        val frameLayout2 = onView(
            allOf(
                withId(R.id.continueBtn),
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
        frameLayout2.perform(click())

        val materialButton22 = onView(
            allOf(
                withId(R.id.button_ok), withText("OK"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.FrameLayout")),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        materialButton22.perform(click())

        val materialButton23 = onView(
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
        materialButton23.perform(click())

        val materialButton24 = onView(
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
        materialButton24.perform(click())

        val materialButton25 = onView(
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
        materialButton25.perform(click())

        val materialButton26 = onView(
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
        materialButton26.perform(click())
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
