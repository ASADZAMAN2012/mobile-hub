/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.summary

import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.core.view.SignatureCaptureView
import org.hamcrest.Matchers

class SignatureCaptureRobot : BaseTestRobot() {
    fun verifyTitle() {
        waitForProgressBarToGone()
        verifyToolbarTitle(R.string.med_d_signature_title)
    }

    fun verifySubmitButtonDisabled() {
        onView(withId(R.id.med_d_signature_submit)).check { view, noViewFoundException ->
            assert(noViewFoundException == null) {
                "Submit button not found!"
            }
            assert(!view.isEnabled) {
                "Submit button was enabled when it wasn't supposed to be!"
            }
        }
    }

    fun drawSignature() {
        waitForViewToAppearAndPerformAction(
            viewInteraction = onView(withId(R.id.med_d_signature_view)),
            userFriendlyName = "Signature Canvas",
            action = DrawLineViewAction()
        )
    }

    fun tapSignatureSubmitButton() {
        IntegrationUtil.clickOnNotFullyVisibleElement(
            onView(withId(R.id.med_d_signature_submit)),
            "Signature Submit Button"
        )
    }

    private class DrawLineViewAction(
        private val touchDown: Pair<Int, Int> = 100 to 200,
        private val touchUp: Pair<Int, Int> = 250 to 450
    ) : ViewAction {
        override fun getDescription() = "Draws on the image painter"

        override fun getConstraints() = Matchers.instanceOf<View>(SignatureCaptureView::class.java)

        override fun perform(uiController: UiController, view: View) {
            if (view is SignatureCaptureView) {
                view.apply {
                    onTouchEvent(
                        motionEvent(
                            MotionEvent.ACTION_DOWN,
                            touchDown.first,
                            touchDown.second
                        )
                    )
                    onTouchEvent(
                        motionEvent(
                            MotionEvent.ACTION_MOVE,
                            touchUp.first,
                            touchUp.second
                        )
                    )
                    onTouchEvent(motionEvent(MotionEvent.ACTION_UP, touchUp.first, touchUp.second))
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
}

fun signatureCaptureScreen(block: SignatureCaptureRobot.() -> Unit) = SignatureCaptureRobot().apply(block)
