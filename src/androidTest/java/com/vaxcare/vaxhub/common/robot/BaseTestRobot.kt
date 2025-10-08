/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.EntryPointHelper.lazyEntryPoint
import com.vaxcare.vaxhub.HiltEntryPoint
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.WAIT_TIME_DEFAULT
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.WAIT_TIME_FOR_LOAD
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.isViewConditionTrue
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.waitFor
import com.vaxcare.vaxhub.common.matchers.WaitingNotMatchViewException
import com.vaxcare.vaxhub.common.matchers.waitMatches
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.worker.JobSelector
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import timber.log.Timber

@EntryPoint
@InstallIn(ActivityComponent::class)
interface BaseTestRobotEntryPoint : HiltEntryPoint {
    fun jobSelector(): JobSelector
}

/**
 * Robot with common UI controls and validators for general purpose
 */
open class BaseTestRobot : HiltEntryPoint {
    private val entryPoint: BaseTestRobotEntryPoint by lazyEntryPoint()

    companion object {
        const val DEFAULT_WAIT_TIME = 2
        const val EXTENDED_WAIT_TIME = 5
    }

    /**
     * Most have the same id, but some screens do not. When this is the case, override this
     */
    protected open val recyclerViewResId: Int = R.id.recycler_view

    fun waitAndVerifyTextOnView(
        viewResId: Int,
        textToVerify: String,
        userFriendlyName: String,
        ignoreCase: Boolean = false
    ) {
        val textMatcher = if (ignoreCase) {
            withTextIgnoringCase(textToVerify)
        } else {
            withText(textToVerify)
        }

        val view = onView(withId(viewResId))
        val (timedOut, waitTime) = runBlocking {
            waitFor(EXTENDED_WAIT_TIME) {
                view.isViewConditionTrue { (it as? TextView)?.text?.isNotEmpty() == false }
            }
        }

        val logMessage =
            "${if (timedOut) "Timed Out at " else "Waited for "} " +
                "$waitTime seconds for $userFriendlyName to show text: $textToVerify"
        Timber.tag("TestStep").d(logMessage)

        view.check(matches(textMatcher))
    }

    fun verifyTextResourceOnView(viewResId: Int, textResId: Int) {
        onView(withId(viewResId))
            .check(matches(withText(textResId)))
    }

    fun verifyToolbarTitle(stringRes: Int, additionalTime: Int = 0) {
        waitForSeconds(additionalTime)
        waitForViewToAppearWithTextResource(stringRes)
        IntegrationUtil.verifyElementsPresentOnPage(
            WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.toolbar_title),
                withText(stringRes)
            )
        )
    }

    fun waitForElementToMatch(element: ViewInteraction, matcher: Matcher<View>): Boolean {
        return try {
            listOf(element.check(waitMatches(matcher))).size == 1
        } catch (e: NoMatchingViewException) {
            false
        } catch (e: WaitingNotMatchViewException) {
            false
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    fun waitForViewToAppearAndPerformAction(
        resId: Int,
        userFriendlyName: String,
        additionalTime: Int = 0,
        action: ViewAction
    ) {
        waitForViewToAppearAndPerformAction(
            onView(withId(resId)),
            userFriendlyName,
            additionalTime,
            action
        )
    }

    fun waitForViewToAppearAndPerformAction(
        viewInteraction: ViewInteraction,
        userFriendlyName: String,
        additionalTime: Int = 0,
        action: ViewAction
    ) {
        IntegrationUtil.waitForElementToAppear(viewInteraction, userFriendlyName)
        waitForSeconds(additionalTime)
        viewInteraction.perform(action)
        Timber.tag("TestStep")
            .d("Performed action ${action.description}")
    }

    fun waitForViewToAppear(resId: Int, additionalTime: Int = 0) {
        waitForSeconds(additionalTime)
        IntegrationUtil.waitForElementToAppear(onView(withId(resId)))
    }

    fun waitForViewToAppearWithTextResource(stringRes: Int) {
        IntegrationUtil.waitForElementToAppear(onView((withText(stringRes))))
    }

    fun waitForAndVerifyViewToAppear(
        element: Matcher<View>,
        userFriendlyName: String,
        timeoutInSeconds: Int = WAIT_TIME_DEFAULT
    ) {
        IntegrationUtil.waitForElementToAppearFailIfNotFound(
            elementToCheck = element,
            userFriendlyName = userFriendlyName,
            secondsToWait = timeoutInSeconds
        )
    }

    fun waitForViewToGone(
        resId: Int,
        userFriendlyName: String,
        additionalTime: Int = WAIT_TIME_DEFAULT
    ) {
        IntegrationUtil.waitForElementToGone(
            elementToCheck = onView(withId(resId)),
            userFriendlyName = userFriendlyName,
            secondsToWait = additionalTime
        )
    }

    fun waitForProgressBarToGone(resId: Int = R.id.loading) {
        waitForViewToGone(resId, "ProgressBar", WAIT_TIME_FOR_LOAD)
    }

    fun tapButtonId(resId: Int) {
        onView(withId(resId)).perform(ViewActions.click())
    }

    fun waitForSeconds(secondsToWait: Int) = runBlocking { waitFor(secondsToWait) }

    fun getItemCountFromRecyclerView(isDialog: Boolean = false): Int =
        IntegrationUtil.getCountFromRecyclerView(
            recyclerView = withId(recyclerViewResId),
            isInDialog = isDialog
        )

    /**
     * Scroll through recycler until a given condition is met.
     *
     * @param isDialog flag for getting item count. Must be true for dialogs
     * @param condition ((index) -> boolean) lambda condition
     * @return index where the condition was met, -1 if scrolled through the entire list without
     */
    fun scrollRecyclerViewUntilCondition(isDialog: Boolean, condition: (Int) -> Boolean): Int {
        val count = IntegrationUtil.getCountFromRecyclerView(
            recyclerView = withId(recyclerViewResId),
            isInDialog = isDialog
        )

        for (index in 0 until count) {
            onView(withId(recyclerViewResId)).perform(
                scrollToPosition<RecyclerView.ViewHolder>(index)
            )
            if (condition(index)) {
                return index
            }
        }

        return -1
    }

    fun waitForViewCondition(
        element: ViewInteraction,
        waitTime: Int = DEFAULT_WAIT_TIME,
        waitUntil: (View) -> Boolean
    ) {
        runBlocking {
            waitFor(waitTime) {
                element.isViewConditionTrue { !waitUntil(it) }
            }
        }
    }

    fun scrollRecyclerView(toPosition: Int) {
        onView(withId(recyclerViewResId)).perform(
            scrollToPosition<RecyclerView.ViewHolder>(
                toPosition
            )
        )
    }

    fun tapDialogButton(dialogElement: Matcher<View>) {
        onView(dialogElement)
            .inRoot(RootMatchers.isDialog())
            .perform(ViewActions.click())
    }

    fun tapDialogButtonId(resId: Int) {
        onView(withId(resId))
            .inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
    }

    fun verifyActionBarTitle(titleStringResId: Int) {
        IntegrationUtil.waitForElementToAppear(
            onView(
                allOf(
                    withId(R.id.header_title),
                    withText(titleStringResId)
                )
            )
        )
    }

    fun verifyViewsOnScreen(secondsToWait: Int = 0, vararg views: Matcher<View>) {
        IntegrationUtil.verifyElementsPresentOnPage(secondsToWait, *views)
    }

    /**
     * select item on bottom dialog.
     * @param itemStringIds select item text res id
     */
    fun clickBottomDialogItem(itemValue: String) {
        IntegrationUtil.swipeBottomSheetDialogToExpand()
        onView(withId(R.id.rv_bottom)).inRoot(RootMatchers.isDialog()).perform(
            RecyclerViewActions.scrollTo<BottomDialog.BottomDialogHolder>(
                ViewMatchers.hasDescendant(
                    withText(itemValue)
                )
            ),
            RecyclerViewActions.actionOnItem<BottomDialog.BottomDialogHolder>(
                ViewMatchers.hasDescendant(withText(itemValue)),
                ViewActions.click()
            ),
            RecyclerViewActions.actionOnItem<BottomDialog.BottomDialogHolder>(
                ViewMatchers.hasDescendant(withText(itemValue)),
                ViewActions.click()
            )
        )
    }

    /**
     * Types string into keyPad and presses 'Enter' key
     *
     * @param ID string to be typed in (partnerID/NumberOfDoses to add)
     */
    fun enterKeyPad(ID: String) {
        val keyPadChars: CharArray = ID.toCharArray()
        keyPadChars.forEach { c -> clickKeys(c) }
        IntegrationUtil.simpleClick(onView(withId(R.id.button_enter)))
        Timber.tag("TestStep").d("Enter $ID in keypad.")
    }

    /**
     * Enter the passed in string value
     *
     * @param id value to enter
     */
    fun clickButtonsOnKeyPad(id: String) = id.toCharArray().forEach { c -> clickKeys(c) }

    /**
     * Types each char on the keypad
     *
     * @param c Char to be typed in
     */
    private fun clickKeys(c: Char) {
        when (c) {
            '0' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_zero)))
            '1' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_one)))
            '2' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_two)))
            '3' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_three)))
            '4' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_four)))
            '5' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_five)))
            '6' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_six)))
            '7' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_seven)))
            '8' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_eight)))
            '9' -> IntegrationUtil.simpleClick(onView(withId(R.id.button_nine)))
            else -> throw IllegalArgumentException("Character not found on KeyPad")
        }
    }

    /**
     * Tap on an element from on a list
     *
     * @param elementToTap element to tap
     * @param position position in list. Default 0 == top of list
     */
    protected fun tapElementFromList(elementToTap: Matcher<View>, position: Int = 0) {
        Espresso.onView(Matchers.allOf(IntegrationUtil.getFirstElement(elementToTap, position)))
            .perform(ViewActions.click())
    }

    protected fun getTextFromList(element: Matcher<View>): String? {
        return IntegrationUtil.getText(Espresso.onView(IntegrationUtil.getFirstElement(element)))
    }

    /**
     * Returns a TypeSafeMatcher for a given view with an index (this will traverse the view
     * hierarchy incrementing currentIndex until the view is found. If never true, a throwable
     * will be thrown
     */
    protected fun withIndex(matcher: Matcher<View>, index: Int = 0): TypeSafeMatcher<View> {
        return object : TypeSafeMatcher<View>() {
            private var currentIndex = 0

            override fun describeTo(description: Description?) {
                description?.appendText("with index: $index")
                matcher.describeTo(description)
            }

            override fun matchesSafely(item: View?): Boolean {
                return matcher.matches(item) && currentIndex++ == index
            }
        }
    }

    /**
     * Simulate a FirebaseMessageEvent from the backend.
     * @see com.vaxcare.vaxhub.core.constant.FirebaseEventTypes
     *
     * @param eventType eventType from the FirebaseEventTypes constant
     * @param payload optional payload - some events require a payload json string
     */
    fun simulateFcmEventReceived(eventType: String, payload: String? = null) {
        if (BuildConfig.BUILD_TYPE == "local") {
            entryPoint.jobSelector().queueJob(eventType, payload)
        }
    }
}
