/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.content.Context
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.HasBackgroundMatcher
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.matchers.WaitingNotMatchViewException
import com.vaxcare.vaxhub.common.matchers.waitMatches
import com.vaxcare.vaxhub.common.matchers.withDrawableByInstrumentation
import com.vaxcare.vaxhub.common.matchers.withDrawableWithTintColor
import com.vaxcare.vaxhub.common.matchers.withNameCheckEligibilityHasBackInAppointmentList
import com.vaxcare.vaxhub.common.matchers.withNameInAppointmentList
import com.vaxcare.vaxhub.common.matchers.withTint
import com.vaxcare.vaxhub.core.extension.toStandardDate
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.core.view.SignatureCaptureView
import com.vaxcare.vaxhub.core.view.calendar.CalendarAdapter
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentListItemViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineItemProductViewHolder
import com.vaxcare.vaxhub.ui.idlingresource.HubIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.isA
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
class IntegrationUtil {
    companion object {
        const val WAIT_TIME_DEFAULT: Int = 5
        const val WAIT_TIME_FOR_INIT_SCANNER: Int = 10
        const val WAIT_TIME_FOR_LOAD: Int = 30

        /**
         * If the UI updated need some time, here can be used to delay on View
         *
         * @param secondsToDelayed
         */
        fun waitUIWithDelayed(secondsToDelayed: Long = 2000) {
            val seconds = (secondsToDelayed / 1000).toInt()
            runBlocking { waitFor(seconds) { !HubIdlingResource.instance.isNotNullAndIdle() } }
        }

        /**
         * check if a unique view is displayed. This will fail if there are multiple views that matches the expression
         * Use it in cases where you know that there is only one matching element on screen
         *
         * @param elementToCheck Element to check
         * @return returns true if element is displayed
         */
        fun isElementDisplayed(elementToCheck: ViewInteraction): Boolean =
            elementToCheck.isViewConditionTrue { it.isVisible }

        /**
         * check the view for a specific condition
         *
         * @param viewCondition callback taking the associated view for evaluation
         * @return result of callback
         */
        fun ViewInteraction.isViewConditionTrue(viewCondition: (View) -> Boolean): Boolean =
            try {
                var result: Boolean? = null
                while (result == null) {
                    this
                        .withFailureHandler { error, _ ->
                            Timber
                                .tag("TestStep")
                                .d("CustomCheckElement - There was an error: ${error.message}")
                            throw error
                        }
                        .check { view, noViewFoundException ->
                            if (noViewFoundException != null) {
                                throw noViewFoundException
                            }
                            result = viewCondition(view)
                        }
                }
                result == true
            } catch (e: Exception) {
                false
            }

        /**
         * check if a unique view is gone. This will fail if there are multiple views that matches the expression
         * Use it in cases where you know that there is only one matching element on screen
         *
         * @param elementToCheck Element to check
         * @return returns true if element is displayed
         */
        fun isElementGone(elementToCheck: ViewInteraction): Boolean =
            try {
                var result = true
                elementToCheck.withFailureHandler { error, _ ->
                    result = false
                    throw error
                }.check { view, noViewFoundException ->
                    if (noViewFoundException != null) {
                        throw noViewFoundException
                    }
                    result = view.visibility == View.GONE
                }
                result
            } catch (e: Exception) {
                false
            }

        /**
         * check if a unique view is displayed when there are multiple views that matches the expression.
         * When there are multiple views it takes first element found in to consideration
         *
         * @param elementToCheck Element to check
         *
         * @return returns true if element is displayed
         */
        fun isElementDisplayedWhenMultipleViews(elementToCheck: Matcher<View>): Boolean {
            val firstElement: Matcher<View>
            return try {
                firstElement = allOf(getFirstElement(elementToCheck, 0))
                onView(firstElement).check(matches(isDisplayed()))
                true
            } catch (e: NoMatchingViewException) {
                false
            } catch (e: Exception) {
                throw Exception(e.message)
            }
        }

        /**
         * case where there is only one view on page that matches the expression.
         * wait for an element to be displayed until timeout.This will throw an exception if element is not found.
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToAppearFailIfNotFound(
            elementToCheck: Matcher<View>,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val res =
                runBlocking { waitFor(secondsToWait) { !isElementDisplayed(onView(elementToCheck)) } }
            Timber.tag("TestStep")
                .d(
                    "$userFriendlyName Waited for ${res.second} seconds. Timed out: " +
                        "${res.first}"
                )
            if (res.first) {
                throw Exception(
                    "Element $userFriendlyName, was expected to be visible but was not found. " +
                        "Waited ${res.second} seconds"
                )
            }
        }

        /**
         * case where there are multiple views that match the expression.
         * wait for the very first element to be displayed until timeout.This will throw an exception if element is not found.
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToAppearWhenMultipleViewsFailIfNotFound(
            elementToCheck: Matcher<View>,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val firstElement: Matcher<View> = allOf(getFirstElement(elementToCheck, 0))
            val res =
                runBlocking { waitFor(secondsToWait) { !isElementDisplayed(onView(firstElement)) } }
            if (res.first) {
                throw Exception(
                    "Element $userFriendlyName, was expected to be visible but was not found. " +
                        "Waited ${res.second} seconds"
                )
            }
        }

        /**
         * case where there are multiple views that match the expression.
         * wait to get visibility
         * @param viewMatcher
         * @param waitToVisible
         * @param waitTime
         * @return
         */
        private fun waitForViewToVisibility(
            viewMatcher: Matcher<View>,
            waitToVisible: Boolean = true,
            waitTime: Long = 2000,
            startTime: Long = System.currentTimeMillis()
        ): Boolean {
            var visibility = false
            val endTime = startTime + waitTime
            try {
                onView(viewMatcher).perform(object : ViewAction {
                    override fun getConstraints(): Matcher<View> {
                        return isA(View::class.java)
                    }

                    override fun getDescription(): String {
                        return "wait for a specific view with id $viewMatcher; wait $waitTime millis."
                    }

                    override fun perform(uiController: UiController, rootView: View) {
                        uiController.loopMainThreadUntilIdle()
                        while (System.currentTimeMillis() < endTime) {
                            visibility = when (rootView) {
                                is RecyclerView -> {
                                    rootView.visibility == View.VISIBLE && (
                                        rootView.adapter?.itemCount
                                            ?: 0
                                    ) > 0
                                }

                                is TextView -> {
                                    rootView.visibility == View.VISIBLE && rootView.text.isNotEmpty()
                                }

                                else -> {
                                    rootView.visibility == View.VISIBLE
                                }
                            }
                            if (waitToVisible && visibility) {
                                break
                            }
                            uiController.loopMainThreadForAtLeast(200)
                        }
                    }
                })
            } catch (e: Exception) {
                if (waitToVisible && System.currentTimeMillis() < endTime) {
                    return waitForViewToVisibility(viewMatcher, true, waitTime, startTime)
                }
                return false
            }
            return visibility
        }

        /**
         * case where there are multiple views that match the expression.
         * wait for the very first element to be displayed until timeout.This will throw an exception if element is found.
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        private fun waitForElementToAppearWhenMultipleViewsFailIfFound(
            elementToCheck: Matcher<View>,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val firstElement: Matcher<View> = getFirstElement(elementToCheck, 0)
            val res = // wait for X seconds or until element is displayed V
                runBlocking {
                    waitForViewToVisibility(
                        firstElement,
                        waitToVisible = false,
                        secondsToWait * 1000L
                    )
                }
            if (res) {
                throw Exception("Element $userFriendlyName, was not expected to be visible. but was Visibility")
            }
        }

        /**
         * wait for a 'viewInteraction' to be displayed until timeout and clicks if found
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToAppearAndClick(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            waitForElementToAppear(elementToCheck, userFriendlyName, secondsToWait)
            elementToCheck.check(matches(isDisplayed())).perform(click())
        }

        /**
         * wait for a 'view' to be displayed until timeout and clicks if found
         *
         * @param elementToCheck Element to be check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToDisplayedAndClick(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            waitForElementToDisplayedAndEnable(elementToCheck, userFriendlyName, secondsToWait)
            simpleClick(elementToCheck, userFriendlyName)
        }

        /**
         * clicks on View
         *
         * @param elementToClick Element to be clicked
         */
        fun simpleClick(elementToClick: ViewInteraction, userFriendlyName: String = elementToClick.toString()) {
            elementToClick.perform(click())
            Timber.tag("TestStep").d("Click view: $userFriendlyName")
        }

        /**
         * delayed click on View
         *
         * @param elementToClick Element to be clicked
         */
        fun delayedClick(elementToClick: Matcher<View>, millisToDelay: Long = 1000) {
            waitUIWithDelayed(millisToDelay)
            onView(elementToClick).perform(click())
        }

        /**
         * ViewAction Click method requires element to be at least 90% visible for click to be successful.Use this overridden method where
         * that constraint is removed instead.
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun clickOnNotFullyVisibleElement(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            waitForElementToAppear(elementToCheck, userFriendlyName, secondsToWait)
            elementToCheck.check(matches(allOf(isEnabled(), isClickable())))
                .perform(object : ViewAction {
                    override fun getConstraints(): Matcher<View> {
                        return isEnabled() // no constraints, they are checked above
                    }

                    override fun getDescription(): String {
                        return userFriendlyName
                    }

                    override fun perform(uiController: UiController?, view: View) {
                        view.performClick()
                    }
                })
        }

        /**
         * cases where multiple views were found with the matcher so espresso cannot make a decision.This Custom Matcher will select the first matching view found.
         *
         * @param parentMatcher Matching View
         * @param position Position of the element you are looking for ( always zero if its the first element)
         *
         * @return returns the matching view found at position 0
         */
        fun getFirstElement(parentMatcher: Matcher<View>, position: Int = 0): Matcher<View> {
            return object : TypeSafeMatcher<View>() {
                var currentIndex = 0

                override fun describeTo(description: Description) {
                    description.appendText("with index: ")
                    description.appendValue(position)
                    parentMatcher.describeTo(description)
                }

                public override fun matchesSafely(view: View): Boolean {
                    return parentMatcher.matches(view) && currentIndex++ == position
                }
            }
        }

        /**
         * verify following elements are present on Page and fails if not found.
         *
         * @param elems list of elements to check
         */
        fun verifyElementsPresentOnPage(secondsToWait: Int = 0, vararg elems: Matcher<View>) {
            elems.forEach { elem ->
                waitForElementToAppearWhenMultipleViewsFailIfNotFound(
                    elem,
                    elem.toString(),
                    secondsToWait
                )
            }
        }

        /**
         * verify following elements are NOT present on Page and fails if found.
         *
         * @param elems list of elements to check
         */
        fun verifyElementsNotPresentOnPage(secondsToWait: Int = 0, vararg elems: Matcher<View>) {
            elems.forEach { elem ->
                waitForElementToAppearWhenMultipleViewsFailIfFound(
                    elem,
                    elem.toString(),
                    secondsToWait
                )
            }
        }

        /**
         * returns text in an element
         *
         * @param viewInteraction element
         *
         * @return text in the element
         */
        fun getText(viewInteraction: ViewInteraction): String? {
            val stringHolder = arrayOf<String?>(null)
            viewInteraction.perform(object : ViewAction {
                override fun getConstraints() = isAssignableFrom(TextView::class.java)

                override fun getDescription() = "Get text from View: ${stringHolder[0]}"

                override fun perform(uiController: UiController, view: View) {
                    val tv = view as TextView
                    stringHolder[0] = tv.text.toString()
                }
            })
            return stringHolder[0]
        }

        /**
         * Types text in a view
         *
         * @param elem view to type in
         * @param inputText text to type
         */
        fun typeText(elem: ViewInteraction, inputText: String) {
            elem.perform(ViewActions.typeText(inputText))
        }

        /**
         * Types text in a view and close soft Keyboard
         *
         * @param elem view to type in
         * @param inputText text to type
         */
        fun typeTextWithCloseSoftKeyboard(elem: ViewInteraction, inputText: String) {
            elem.perform(ViewActions.typeText(inputText), ViewActions.closeSoftKeyboard())
        }

        /**
         * Clear text in a view
         *
         * @param elem view to clear
         */
        fun clearText(elem: ViewInteraction) {
            elem.perform(ViewActions.clearText())
        }

        /**
         * Verifies 'text' is displayed in a view
         *
         * @param elem  elem that contains the text
         * @param textToVerify text to verify
         */
        fun verifyTextDisplayed(elem: Matcher<View>, textToVerify: String) {
            waitForElementToAppearFailIfNotFound(allOf(elem, withText(textToVerify)))
        }

        /**
         * Performs the Swipe Left Action on an element
         *
         * @param elem element
         */
        fun swipeLeft(elem: ViewInteraction) {
            waitForElementToAppear(elem, elem.toString(), 3)
            elem.perform(ViewActions.swipeLeft())
        }

        /**
         * Performs the Swipe Right Action on an element
         *
         * @param elem element
         */
        fun swipeRight(elem: ViewInteraction) {
            waitForElementToAppear(elem, elem.toString(), 3)
            elem.perform(ViewActions.swipeRight())
        }

        /**
         * Performs the Swipe Down Action on an element
         *
         * @param elem element
         */
        fun swipeDown(elem: ViewInteraction) {
            waitForElementToAppear(elem, elem.toString(), 3)
            elem.perform(swipeDownFromTopToCenter())
        }

        private fun swipeDownFromTopToCenter(): ViewAction {
            val startPoint: CoordinatesProvider = GeneralLocation.TOP_LEFT
            val endPoint: CoordinatesProvider = GeneralLocation.CENTER_LEFT
            return ViewActions.actionWithAssertions(
                GeneralSwipeAction(
                    Swipe.FAST,
                    startPoint,
                    endPoint,
                    Press.FINGER
                )
            )
        }

        /**
         * returns the element at a position
         *
         * @param position position of element
         * @param itemMatcher Matching View
         *
         * @return the matching view at position
         */
        fun atPosition(
            position: Int,
            @NonNull itemMatcher: Matcher<View?>
        ): BoundedMatcher<View?, RecyclerView> {
            return object : BoundedMatcher<View?, RecyclerView>(
                RecyclerView::class.java
            ) {
                override fun describeTo(description: Description) {
                    description.appendText("has item at position $position: ")
                    itemMatcher.describeTo(description)
                }

                override fun matchesSafely(view: RecyclerView): Boolean {
                    val viewHolder = view.findViewHolderForAdapterPosition(position)
                        ?: // has no item on such position
                        return false
                    return itemMatcher.matches(viewHolder.itemView)
                }
            }
        }

        /**
         * Returns the total count of rows in a recycler View
         *
         * @param recyclerView Recycler View identifier
         *
         * @return total no:of rows
         */
        fun getCountFromRecyclerView(recyclerView: Matcher<View>, isInDialog: Boolean = false): Int {
            var count = 0
            val matcher: Matcher<*> = object : TypeSafeMatcher<View>() {
                override fun matchesSafely(item: View): Boolean {
                    count = (item as RecyclerView).adapter!!.itemCount
                    return true
                }

                override fun describeTo(description: Description?) {
                    description?.appendText("No Recycler View Items Count found")
                }
            }
            if (isInDialog) {
                onView(
                    allOf(
                        recyclerView,
                        isDisplayed()
                    )
                ).inRoot(isDialog()).check(matches(matcher as Matcher<in View>?))
            } else {
                onView(
                    allOf(
                        recyclerView,
                        isDisplayed()
                    )
                ).check(matches(matcher as Matcher<in View>?))
            }

            val result = count
            count = 0
            return result
        }

        /**
         * returns the total number of rows in a recycler view
         *
         * @param recyclerView Id of recyclerView
         * @param recyclerViewUserFriendlyName Friendly Name to identify the recyclerView
         *
         * @return the number of rows in the recycler view
         */
        fun getTotalRowsInRecyclerView(
            recyclerView: Matcher<View>,
            recyclerViewUserFriendlyName: String,
            isInDialog: Boolean = false
        ): Int {
            val numberOfRecyclerViewItems: Int = getCountFromRecyclerView(recyclerView, isInDialog)
            if (numberOfRecyclerViewItems == 0) {
                throw Exception(
                    "No Lots were found on '$recyclerViewUserFriendlyName' recycler view list"
                )
            }
            return numberOfRecyclerViewItems
        }

        /**
         * Wait for recyclerView to display contents
         *
         * @param recyclerView
         * @param userFriendlyName
         * @param isInDialog
         * @param needDisplayMinCount The list needs to display the minimum count of the number of data, Because some recyclerView have multiple type layouts.
         */
        fun waitForRowsToRecyclerView(
            recyclerView: Matcher<View>,
            userFriendlyName: String,
            isInDialog: Boolean = false,
            needDisplayMinCount: Int = 0
        ) {
            val res = runBlocking {
                waitFor(WAIT_TIME_DEFAULT) {
                    getTotalRowsInRecyclerView(
                        recyclerView, userFriendlyName, isInDialog
                    ) > needDisplayMinCount
                }
            }
            if (res.first) {
                throw Exception(
                    "Data: $userFriendlyName, was expected to be visible in " +
                        "RecyclerView but was not found. Waited ${res.second} seconds"
                )
            }
        }

        /**
         * Scroll to a specific text in the recycler view and click it
         *
         * @param recyclerView RecyclerView identifier
         * @param elemInRecyclerView Matching element that holds text
         * @param text Search text
         * @param friendlyName Friendly name of matching element looking for for logging
         * @param clickChildViewID Click on child in RecyclerView, if id is null, will click item
         */
        fun scrollToElementInRecyclerViewAndClick(
            recyclerView: Matcher<View>,
            recyclerViewName: String,
            elemInRecyclerView: Matcher<View>,
            friendlyName: String,
            clickChildViewID: Int? = null
        ) {
            val numberOfRecyclerViewItems: Int =
                getTotalRowsInRecyclerView(recyclerView, recyclerViewName)
            var foundText = false

            for (i in 0 until numberOfRecyclerViewItems) {
                onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                if (isElementDisplayedWhenMultipleViews(elemInRecyclerView)) {
                    foundText = true
                    clickChildViewOnRecyclerView(recyclerView, clickChildViewID, i)
                    break
                }
            }
            if (!foundText) throw Exception("Expected $friendlyName was not found in $recyclerViewName")
        }

        /**
         * Scroll to a specific text in the recycler view
         *
         * @param recyclerView RecyclerView identifier
         * @param elemInRecyclerView Matching element that holds text
         * @param text Search text
         * @param friendlyName Friendly name of matching element looking for for logging
         */
        fun verifyAndScrollToElementInRecyclerView(
            recyclerView: Matcher<View>,
            recyclerViewName: String,
            elemInRecyclerView: Matcher<View>,
            friendlyName: String
        ) {
            waitUIWithDelayed()
            val numberOfRecyclerViewItems: Int =
                getTotalRowsInRecyclerView(recyclerView, recyclerViewName)
            var foundText = false

            for (i in 0 until numberOfRecyclerViewItems) {
                onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                if (isElementDisplayedWhenMultipleViews(elemInRecyclerView)) {
                    foundText = true
                    break
                }
            }
            if (!foundText) {
                throw Exception(
                    "Could not scroll to $friendlyName with $elemInRecyclerView. " +
                        "It was not found on recycler view list- " +
                        "$recyclerView with $numberOfRecyclerViewItems items."
                )
            }
        }

        /**
         * auto scrolls and clicks on child in RecyclerView
         *
         * @param recyclerView
         * @param childViewID
         * @param position
         */
        fun clickChildViewOnRecyclerView(
            recyclerView: Matcher<View>,
            childViewID: Int?,
            position: Int
        ) {
            onView(recyclerView).perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    if (childViewID != null) clickOnViewChild(childViewID) else click()
                )
            )
        }

        /**
         * Click on a child view with specified id.
         *
         * @param id ChildView ID
         *
         * @return View that was clicked on
         */
        fun clickOnViewChild(viewId: Int) =
            object : ViewAction {
                override fun getConstraints() = null

                override fun getDescription() = "Click on a child view with specified id."

                override fun perform(uiController: UiController, view: View) {
                    if (view.findViewById<TextView>(viewId) != null) {
                        click().perform(uiController, view.findViewById(viewId))
                    }
                }
            }

        /**
         * This method returns current date values
         *
         * @return returns current date values
         */
        fun getCurrentDateValues(): Array<String> {
            return with(LocalDate.now()) {
                arrayOf(
                    month.value.toString(),
                    dayOfMonth.toString(),
                    year.toString(),
                    month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .toString(),
                    dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .toString()
                )
            }
        }

        /**
         * This method returns next month and year, eg: December 2021
         *
         * @return returns next month and year, eg: December 2021
         */
        fun getNextMonthYear(): String {
            return with(LocalDate.now().plusMonths(1)) {
                month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .toString() + " " + year.toString()
            }
        }

        /**
         * This method returns next day's date
         *
         * @return returns next day's date
         */
        fun getNextDay(): String {
            return with(LocalDate.now().plusDays(1)) {
                dayOfMonth.toString()
            }
        }

        /**
         * This method returns next day's day and date eg, Friday, 12th
         *
         * @return returns next day's day and date eg, Friday, 12th
         */
        fun getNextDate(): String {
            return with(LocalDate.now().plusDays(1)) {
                dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .toString() + ", " + LocalDateTime.now().plusDays(1).toStandardDate()
                    .split(',')[0]
            }
        }

        /**
         * Get a random day of a month.The Calendar is shown as 6*7
         */
        fun getRandomDayForCalendar(): Int = (0..41).random()

        fun verifyElementsGoneOnPage(secondsToWait: Int = 0, vararg elems: Matcher<View>) {
            elems.forEach { elem ->
                waitForElementToGone(
                    onView(elem),
                    elem.toString(),
                    secondsToWait
                )
            }
        }

        /**
         * wait for a item in Calendar to be displayed until timeout and clicks if found
         *
         * @param elementToClick Element to be clicked
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         * @param position position to click
         */
        fun waitForCalendarViewElementToAppearAndClick(
            elementToClick: Matcher<View>,
            userFriendlyName: String = elementToClick.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT,
            position: Int
        ) {
            waitForElementToDisplayed(onView(elementToClick), userFriendlyName, secondsToWait)
            onView(elementToClick).perform(
                actionOnItemAtPosition<CalendarAdapter.ViewHolder>(position, click())
            )
        }

        /**
         * wait for a 'viewInteraction' to be displayed until timeout and clicks if found
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToGone(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val res = runBlocking { waitFor(secondsToWait) { !isElementGone(elementToCheck) } }
            Timber.tag("TestStep")
                .d("$userFriendlyName waited for ${res.second} seconds to gone. Timed out: ${res.first}")
        }

        /**
         * wait for a 'viewInteraction' to be not visible until timeout
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait time in seconds to wait before timeout
         */
        fun waitForElementToNotVisible(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val res = runBlocking { waitFor(secondsToWait) { isElementDisplayed(elementToCheck) } }
            Timber.tag("TestStep")
                .d("$userFriendlyName Waited for ${res.second} seconds to not be visible. Timed out: ${res.first}")
        }

        /**
         * case where there is only one view on page that matches the expression.
         * wait for an element to be displayed until timeout.This wont throw an exception if element is not found,it just waits.
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToAppear(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val wt = runBlocking { waitFor(secondsToWait) { !isElementDisplayed(elementToCheck) } }
            Timber.tag("TestStep")
                .d("$userFriendlyName Waited for ${wt.second} seconds to appear. Timed out: ${wt.first}")
        }

        fun waitForElementToDisplayed(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ): Boolean {
            val wt = runBlocking {
                waitFor(secondsToWait) {
                    !waitElementToViewMatcher(
                        elementToCheck,
                        isDisplayed()
                    )
                }
            }
            Timber.tag("TestStep")
                .d("$userFriendlyName Waited for ${wt.second} seconds to displayed. Timed out: ${wt.first}")
            return wt.first
        }

        fun waitForElementToDisplayedAndEnable(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val wt = runBlocking {
                waitFor(secondsToWait) {
                    !waitElementToViewMatcher(
                        elementToCheck,
                        allOf(isDisplayed(), isEnabled(), isClickable())
                    )
                }
            }
            Timber.tag("TestStep")
                .d("$userFriendlyName Waited for ${wt.second} seconds to displayed. Timed out: ${wt.first}")
        }

        /**
         * case where there is one view on RecyclerView that matches the expression.
         * wait for an element to be displayed until timeout.This wont throw an exception if element is not found,it just waits.
         *
         * @param recyclerView
         * @param userFriendlyName
         * @param elemInRecyclerView
         * @param isInDialog
         * @param secondsToWait
         */
        fun waitForElementInRecyclerView(
            recyclerView: Matcher<View>,
            userFriendlyName: String,
            elemInRecyclerView: Matcher<View>,
            isInDialog: Boolean = false,
            secondsToWait: Int = 15
        ): Boolean {
            val wt = runBlocking {
                waitFor(secondsToWait) {
                    !isElementInRecyclerView(
                        recyclerView,
                        elemInRecyclerView,
                        isInDialog
                    )
                }
            }
            Timber.tag("TestStep")
                .d("$userFriendlyName recyclerView waited for ${wt.second} seconds to appear. Timed out: ${wt.first}")
            return wt.first
        }

        /**
         * check if a view is show in RecyclerView.
         * Use it in cases where you know that there is a matching element on RecyclerView
         *
         * @param recyclerView
         * @param elemInRecyclerView
         * @param isInDialog
         * @return returns true if element is displayed
         */
        fun isElementInRecyclerView(
            recyclerView: Matcher<View>,
            elemInRecyclerView: Matcher<View>,
            isInDialog: Boolean = false
        ): Boolean {
            return try {
                val numberOfRecyclerViewItems: Int =
                    getCountFromRecyclerView(recyclerView, isInDialog)
                Timber.tag("TestStep")
                    .d("RecyclerView ViewItems= $numberOfRecyclerViewItems to appear")
                var foundText = false
                for (i in 0 until numberOfRecyclerViewItems) {
                    onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                    waitForOperationComplete(1)
                    if (isElementDisplayedWhenMultipleViews(elemInRecyclerView)) {
                        foundText = true
                        break
                    }
                }
                foundText
            } catch (e: Exception) {
                false
            } catch (e: Error) {
                false
            }
        }

        /**
         * an appt is created on portal and it takes  couple of seconds to fetch eligibilty
         *
         * @param recyclerView
         * @param testPatient
         * @param secondsToWait
         */
        fun waitForEligibilityHasBackInAppointmentList(
            recyclerView: Matcher<View>,
            testPatient: TestPatients,
            secondsToWait: Int = 10
        ): Boolean {
            val wt = runBlocking {
                waitFor(secondsToWait) {
                    !isEligibilityHasBackInAppointmentList(
                        recyclerView,
                        testPatient
                    )
                }
            }
            Timber.tag("TestStep")
                .d("AppointmentList recyclerView waited for ${wt.second} seconds to appear. Timed out: ${wt.first}")
            return wt.first
        }

        fun isEligibilityHasBackInAppointmentList(recyclerView: Matcher<View>, testPatient: TestPatients): Boolean {
            return try {
                val numberOfRecyclerViewItems: Int = getCountFromRecyclerView(recyclerView)
                Timber.tag("TestStep")
                    .d("AppointmentList ViewItems= $numberOfRecyclerViewItems to appear")
                var foundText = false
                for (i in 0 until numberOfRecyclerViewItems) {
                    onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                    waitUIWithDelayed(500)
                    if (checkEligibilityHasBackInAppointmentList(
                            recyclerView,
                            testPatient,
                            i
                        )
                    ) {
                        foundText = true
                        break
                    }
                }
                foundText
            } catch (e: Throwable) {
                false
            }
        }

        /**
         * Check a block of work for X seconds
         *
         * @param maxWaitTimeInSeconds maximum time to wait in seconds
         * @param waitCondition function to keep invoking
         * @return X seconds - total time waited for waitCondition to return false
         */
        fun waitFor(maxWaitTimeInSeconds: Int, waitCondition: () -> Boolean = { true }) =
            run {
                var timeUp = false

                fun isWaiting() = !HubIdlingResource.instance.isNotNullAndIdle() || (waitCondition() && !timeUp)

                val maxWait = maxWaitTimeInSeconds * 1000
                val timeStamp = System.currentTimeMillis()

                while (isWaiting()) {
                    timeUp = System.currentTimeMillis() - timeStamp > maxWait
                }
                timeUp to TimeUnit.MILLISECONDS
                    .toSeconds(System.currentTimeMillis() - timeStamp)
                    .toInt()
            }

        /**
         * Wait for some operation complete, for example:animation, action after calculation.
         */
        fun waitForOperationComplete(secondsToWait: Int = 2) {
            waitFor(secondsToWait) { true }
        }

        /**
         * case where there is only one view on page that matches the expression.
         * wait for an element to be displayed and isEnabled until timeout.This wont throw an exception if element is not found,it just waits.
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToAppearAndEnabled(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            waitForElementToAppear(elementToCheck, userFriendlyName, secondsToWait)
            elementToCheck.check(matches(allOf(isDisplayed(), isEnabled())))
        }

        /**
         * wait for element to be displayed until timeout if found
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun returnSecondToWaitElementFromAppearToDisappear(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ): Int {
            Timber.tag("TestStep").d("waiting for $userFriendlyName to appear")
            val waitAppearTime = runBlocking {
                waitFor(WAIT_TIME_DEFAULT) {
                    !waitElementToViewMatcher(
                        elementToCheck,
                        isDisplayed()
                    )
                }
            }
            return if (waitAppearTime.first) {
                // the element is disappear
                0
            } else {
                val waitDisappearTime = runBlocking {
                    val maxWait = secondsToWait * 1000
                    val stamp = System.currentTimeMillis()
                    var waitTime = System.currentTimeMillis()
                    var timeUp = false
                    while (!timeUp) {
                        val waitCondition = waitElementToViewMatcher(elementToCheck, isDisplayed())
                        if (waitCondition) {
                            waitTime = System.currentTimeMillis()
                        } else {
                            break
                        }
                        timeUp = System.currentTimeMillis() - stamp > maxWait
                    }
                    timeUp to TimeUnit.MILLISECONDS.toSeconds(waitTime - stamp).toInt()
                }
                if (waitDisappearTime.first) {
                    throw Exception(
                        "Expected to find following element on page-$userFriendlyName. " +
                            "Waited for ${waitDisappearTime.second} seconds"
                    )
                }
                waitDisappearTime.second
            }
        }

        /**
         * case where there is only one view on page that matches the expression.
         * wait for an element to be isEnabled until timeout.This wont throw an exception if element is not found,it just waits.
         *
         * @param elementToCheck Element to check
         * @param userFriendlyName User friendly name for logging
         * @param secondsToWait max sec to wait
         */
        fun waitForElementToEnabled(
            elementToCheck: ViewInteraction,
            userFriendlyName: String = elementToCheck.toString(),
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ) {
            val wt = runBlocking {
                waitFor(secondsToWait) {
                    !waitElementToViewMatcher(elementToCheck, allOf(isEnabled()))
                }
            }
            Timber.tag("TestStep")
                .d("$userFriendlyName Waited for ${wt.second} seconds to appear. Timed out: ${wt.first}")
            if (wt.first) {
                throw Exception(
                    "Expected to find following element on page-$userFriendlyName .Waited for ${wt.second} seconds"
                )
            }
        }

        /**
         * check if a unique view's Visibility. This will fail if there are multiple views that matches the expression
         * Use it in cases where you know that there is only one matching element on screen
         *
         * @param elementToCheck Element to check
         * @param viewMatcher  Check element
         * @return returns true if element now is match
         */
        fun waitElementToViewMatcher(elementToCheck: ViewInteraction, viewMatcher: Matcher<View>): Boolean {
            try {
                val elementList = mutableListOf(elementToCheck.check(waitMatches(viewMatcher)))
                if (elementList.count() == 1) return true
            } catch (e: NoMatchingViewException) {
                return false
            } catch (e: WaitingNotMatchViewException) {
                return false
            } catch (e: Exception) {
                throw Exception(e.message)
            }
            return false
        }

        /**
         * wait for an element to be Matcher until timeout.
         * This wont throw an exception if element is not Matcher return false, or return true
         *
         * @param elementToCheck Element to check
         * @param viewMatcher  Check element
         * @param secondsToWait max sec to wait
         * @return returns true if element now is match
         */
        fun isElementToViewMatcher(
            elementToCheck: ViewInteraction,
            viewMatcher: Matcher<View>,
            secondsToWait: Int = WAIT_TIME_DEFAULT
        ): Boolean {
            var i = 0
            var elementMatch = false
            while (i <= secondsToWait || elementMatch) {
                if (waitElementToViewMatcher(elementToCheck, viewMatcher)) {
                    elementMatch = true
                    break
                }
                Thread.sleep(1000)
                i++
            }
            Thread.sleep(1000)
            return elementMatch
        }

        /**
         * Verify destination screen by ToolbarTitle
         */
        fun verifyDestinationScreenByToolbarTitle(toolbarTitleText: String) {
            waitForElementToDisplayed(
                elementToCheck = onView(withText(toolbarTitleText)),
                userFriendlyName = "Toolbar Title",
                secondsToWait = 15
            )
            verifyElementsPresentOnPage(
                WAIT_TIME_DEFAULT,
                allOf(
                    withId(R.id.toolbar_title),
                    withText(toolbarTitleText)
                )
            )
        }

        /**
         * Check an element to be displayed by the given root matcher and click if found
         *
         * @param elementToCheck Element to check
         * @param rootMatcher Makes this ViewInteraction scoped to the root selected by the given root matcher.
         */
        fun checkElementAppearAndClickWithRoot(elementToCheck: Matcher<View>, rootMatcher: Matcher<Root>) {
            onView(elementToCheck).inRoot(rootMatcher).check(matches(isDisplayed()))
                .perform(click())
        }

        /**
         * Matches the specific patient name in appointment list.
         * @param nameString matched patient name
         */
        fun matchNameInAppointmentList(nameString: String): Matcher<AppointmentListItemViewHolder> {
            return object : TypeSafeMatcher<AppointmentListItemViewHolder>() {
                override fun matchesSafely(customHolder: AppointmentListItemViewHolder): Boolean {
                    customHolder
                    return nameString.equals(customHolder.binding.patientName.text as String?, true)
                }

                override fun describeTo(description: Description) {
                    description.appendText("patient name text equal to match string")
                }
            }
        }

        /**
         * Matches the specific patient name in appointment list.
         * @param lotNumberString matched patient name
         */
        fun matchNameInVaccinationList(lotNumberString: String): Matcher<VaccineItemProductViewHolder> {
            return object : TypeSafeMatcher<VaccineItemProductViewHolder>() {
                override fun matchesSafely(customHolder: VaccineItemProductViewHolder): Boolean {
                    return TextUtils.equals(
                        customHolder.binding.checkoutVaccineLotNumber.text,
                        lotNumberString
                    )
                }

                override fun describeTo(description: Description) {
                    description.appendText("checkout vaccine lot number equal to match string")
                }
            }
        }

        /**
         * check a view without specific drawable.
         * @param resourceId resource id to check
         */
        fun notWithDrawable(resourceId: Int): Matcher<View> {
            return not(HasBackgroundMatcher(resourceId))
        }

        /**
         * draw for signature
         */
        fun drawForSignature(element: Matcher<View>) {
            val down = Pair(100, 200)
            val up = Pair(250, 450)
            onView(element).perform(draw(down, up))
        }

        private fun draw(down: Pair<Int, Int>, up: Pair<Int, Int>) =
            object : ViewAction {
                override fun getDescription() = "Draws on the image painter"

                override fun getConstraints() = instanceOf<View>(SignatureCaptureView::class.java)

                override fun perform(uiController: UiController, view: View) {
                    if (view is SignatureCaptureView) {
                        view.apply {
                            onTouchEvent(
                                motionEvent(
                                    MotionEvent.ACTION_DOWN,
                                    down.first,
                                    down.second
                                )
                            )
                            onTouchEvent(motionEvent(MotionEvent.ACTION_MOVE, up.first, up.second))
                            onTouchEvent(motionEvent(MotionEvent.ACTION_UP, up.first, up.second))
                        }
                    }
                }

                private fun motionEvent(
                    action: Int,
                    x: Int,
                    y: Int
                ): MotionEvent {
                    return MotionEvent.obtain(1L, 1L, action, x.toFloat(), y.toFloat(), 0)
                }
            }

        /**
         * Get the number of patients by patientName in the list based on patient name
         *
         * @param recyclerView
         * @param recyclerViewName
         * @param patientName
         */
        fun getNumberOfNameInAppointmentList(
            recyclerView: Matcher<View>,
            recyclerViewName: String,
            testPatient: TestPatients
        ): Int {
            var filterCount = 0
            val count = getTotalRowsInRecyclerView(recyclerView, recyclerViewName)
            if (count == 0) return 0
            for (i in 0 until count) {
                onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                if (checkNameInAppointmentList(recyclerView, testPatient, i)) {
                    filterCount++
                }
            }
            return filterCount
        }

        /**
         * Click first element in appointment list by patient Name
         *
         * @param recyclerView
         * @param recyclerViewName
         * @param patientName
         */
        fun clickFirstElementInAppointmentListByPatientName(
            recyclerView: Matcher<View>,
            recyclerViewName: String,
            testPatient: TestPatients
        ) {
            val count = getTotalRowsInRecyclerView(recyclerView, recyclerViewName)
            if (count == 0) {
                throw Exception(
                    "Expected to find following element on $recyclerViewName-${testPatient.completePatientName}"
                )
            }
            for (i in 0 until count) {
                onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                if (checkNameInAppointmentList(recyclerView, testPatient, i)) {
                    onView(recyclerView).perform(
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(i, click())
                    )
                    break
                }
            }
        }

        /**
         * Click last element in appointment list by patient Name
         *
         * @param recyclerView
         * @param recyclerViewName
         * @param patientName
         */
        fun clickLastElementInAppointmentListByPatientName(
            recyclerView: Matcher<View>,
            recyclerViewName: String,
            testPatient: TestPatients
        ) {
            val count = getTotalRowsInRecyclerView(recyclerView, recyclerViewName)
            if (count == 0) {
                throw Exception(
                    "Expected to find following element on $recyclerViewName-${testPatient.completePatientName}"
                )
            }
            for (i in count - 1 downTo 0) {
                onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                if (checkNameInAppointmentList(recyclerView, testPatient, i)) {
                    onView(recyclerView).perform(
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(i, click())
                    )
                    break
                }
            }
        }

        /**
         * Check if the patient exists on the AppointmentList
         *
         * @param recyclerView
         * @param testPatient
         * @param index
         */
        fun checkNameInAppointmentList(
            recyclerView: Matcher<View>,
            testPatient: TestPatients,
            index: Int
        ): Boolean {
            return try {
                onView(recyclerView).check(
                    matches(
                        withNameInAppointmentList(
                            testPatient,
                            index
                        )
                    )
                )
                true
            } catch (e: Throwable) {
                false
            }
        }

        private fun checkEligibilityHasBackInAppointmentList(
            recyclerView: Matcher<View>,
            testPatient: TestPatients,
            index: Int
        ): Boolean {
            return try {
                onView(recyclerView).check(
                    matches(
                        withNameCheckEligibilityHasBackInAppointmentList(
                            testPatient,
                            index
                        )
                    )
                )
                true
            } catch (e: Throwable) {
                false
            }
        }

        /**
         * verify RiskIcon that gets displayed for patients
         *
         * @param riskIconView
         * @param resourceId
         * @param isInstrumentation if resourceId is from folder: androidTest/res/drawable isInstrumentation = true, otherwise false
         */
        fun checkRiskIconForPatient(
            riskIconView: Matcher<View>,
            resourceId: Int,
            isInstrumentation: Boolean = false,
            tintColor: Int? = null
        ) {
            onView(riskIconView).check(
                matches(
                    if (isInstrumentation) {
                        withDrawableByInstrumentation(
                            resourceId,
                            tintColor
                        )
                    } else {
                        withDrawableWithTintColor(resourceId, tintColor)
                    }
                )
            )
        }

        /**
         * Expand bottomSheet Dialog
         */
        fun swipeBottomSheetDialogToExpand() {
            onView(withId(R.id.rv_bottom)).inRoot(RootMatchers.isDialog()).perform(
                RecyclerViewActions.actionOnItemAtPosition<BottomDialog.BottomDialogHolder>(
                    0,
                    ViewActions.swipeUp()
                )
            )
            waitUIWithDelayed()
        }

        /**
         * Expand bottomSheet Dialog
         */
        fun swipeBottomSheetDialogPayerSearchToExpand() {
            onView(withId(R.id.rv_payer_search_results)).inRoot(RootMatchers.isDialog()).perform(
                RecyclerViewActions.actionOnItemAtPosition<BottomDialog.BottomDialogHolder>(
                    0,
                    ViewActions.swipeUp()
                )
            )
            waitUIWithDelayed()
        }

        /**
         * Check item on Appointment list
         *
         * @param recyclerView
         * @param recyclerViewName
         * @param testPatients
         * @param matcher
         */
        fun checkAppointmentListItemByPatientName(
            recyclerView: Matcher<View>,
            recyclerViewName: String,
            testPatients: TestPatients,
            matcher: Matcher<View>
        ) {
            val count = getTotalRowsInRecyclerView(recyclerView, recyclerViewName)
            var hasPatientNameItem = false
            if (count == 0) {
                throw Exception(
                    "Expected to find following element on $recyclerViewName-" +
                        "${testPatients.lastName},${testPatients.firstName}"
                )
            }
            for (i in 0 until count) {
                onView(recyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(i))
                if (checkNameInAppointmentList(recyclerView, testPatients, i)) {
                    hasPatientNameItem = true
                    onView(recyclerView).check(matches(atPositionOnView(i, matcher)))
                    break
                }
            }
            if (!hasPatientNameItem) {
                throw Exception(
                    "Expected to find element ${testPatients.lastName}, " +
                        "${testPatients.firstName} on $recyclerViewName"
                )
            }
        }

        /**
         * Custom match is used to check item on Appointment list
         * @param position position on Recyclerview
         * @param targetViewId target view to check
         * @param itemMatcher item matcher
         */
        private fun atPositionOnView(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
            return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
                override fun describeTo(description: Description) {
                    description.appendText("has view id $itemMatcher at position $position")
                }

                override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                    return viewHolder?.let { itemMatcher.matches(it.itemView) } ?: false
                }
            }
        }

        /**
         * verify an element to be displayed
         *
         * @param elementToCheck
         */
        fun verifyElementToAppear(elementToCheck: ViewInteraction) {
            elementToCheck.check(matches(isDisplayed()))
        }

        fun confirmAlertDialog(
            titleTextResId: Int? = null,
            titleTextString: String = "",
            processingTextResId: Int? = null,
            processingTextString: String = "",
            userFriendlyName: String,
            positiveViewResId: Int = R.id.button_ok,
            positiveTextResId: Int? = null,
            positiveTextString: String = "",
            positiveTintColor: Int? = null,
            negativeViewResId: Int = R.id.button_cancel,
            negativeTextResId: Int? = null,
            negativeTextString: String = "",
            negativeTintColor: Int? = null,
            selectedStringResId: Int? = null,
            selectedString: String = ""
        ) {
            val resource = ApplicationProvider.getApplicationContext<Context>().resources
            val titleText = titleTextResId?.let { resource.getString(it) } ?: titleTextString
            waitUIWithDelayed(1000)
            if (titleText.isNotBlank()) {
                onView(withText(titleText)).inRoot(isDialog()).check(matches(isDisplayed()))
            }

            val processingText =
                processingTextResId?.let { resource.getString(it) } ?: processingTextString
            if (processingText.isNotBlank()) {
                onView(withText(processingText)).inRoot(isDialog()).check(matches(isDisplayed()))
            }

            val positiveText =
                positiveTextResId?.let { resource.getString(it) } ?: positiveTextString
            if (positiveText.isNotBlank()) {
                onView(withText(positiveText)).inRoot(isDialog()).check(matches(isDisplayed()))
            }
            positiveTintColor?.let {
                onView(withId(positiveViewResId)).inRoot(isDialog()).check(matches(withTint(it)))
            }

            val negativeText =
                negativeTextResId?.let { resource.getString(it) } ?: negativeTextString
            if (negativeText.isNotBlank()) {
                onView(withText(negativeText)).inRoot(isDialog()).check(matches(isDisplayed()))
            }
            negativeTintColor?.let {
                onView(withId(negativeViewResId)).inRoot(isDialog()).check(matches(withTint(it)))
            }

            val selectText = selectedStringResId?.let { resource.getString(it) } ?: selectedString
            if (selectText.isNotBlank()) {
                waitUIWithDelayed()
                onView(withText(selectText)).inRoot(isDialog()).perform(click())
            }
        }

        /**
         * Verify bottom dialog items.
         */
        fun verifyBottomDialogItems(
            recyclerView: Matcher<View>,
            recyclerViewName: String,
            itemList: List<String>
        ) {
            waitUIWithDelayed()
            val numberOfRecyclerViewItems: Int =
                getTotalRowsInRecyclerView(recyclerView, recyclerViewName, true)
            assert(itemList.size == numberOfRecyclerViewItems) {
                "Expected size of $recyclerViewName  is ${itemList.size}. But it was $numberOfRecyclerViewItems."
            }

            for (i in 0 until numberOfRecyclerViewItems) {
                onView(recyclerView).inRoot(isDialog())
                    .perform(scrollToPosition<RecyclerView.ViewHolder>(i)).check(
                        matches(
                            atPosition(
                                i,
                                hasDescendant(allOf(withId(R.id.label), withText(itemList[i])))
                            )
                        )
                    )
            }
        }

        /**
         * select item on bottom dialog.
         * @param itemStringIds select item text res id
         */
        fun clickBottomDialogItem(itemStringIds: Int) {
            swipeBottomSheetDialogToExpand()
            onView(withId(R.id.rv_bottom)).inRoot(isDialog()).perform(
                scrollTo<BottomDialog.BottomDialogHolder>(
                    hasDescendant(
                        withText(itemStringIds)
                    )
                ),
                actionOnItem<BottomDialog.BottomDialogHolder>(
                    hasDescendant(withText(itemStringIds)),
                    click()
                )
            )
        }

        /**
         * select item on bottom dialog.
         * @param itemValue select item text
         */
        fun clickBottomDialogItem(itemValue: String) {
            swipeBottomSheetDialogToExpand()
            onView(withId(R.id.rv_bottom)).inRoot(RootMatchers.isDialog()).perform(
                scrollTo<BottomDialog.BottomDialogHolder>(
                    hasDescendant(
                        withText(itemValue)
                    )
                ),
                actionOnItem<BottomDialog.BottomDialogHolder>(
                    hasDescendant(withText(itemValue)),
                    click()
                )
            )
        }

        /**
         * generate a string of length "length".
         */
        fun getRandomString(length: Int): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..length).map { allowedChars.random() }.joinToString("")
        }
    }
}

fun HubIdlingResource?.isNotNullAndIdle(): Boolean = this?.isIdleNow() ?: false
