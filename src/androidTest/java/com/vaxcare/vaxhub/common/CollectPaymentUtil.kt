/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.data.TestCards
import org.hamcrest.Matchers
import java.time.LocalDate

class CollectPaymentUtil {
    /**
     * Verify texts displayed on 'Collect Payment Info' screen.
     */
    fun verifyToCollectPaymentInfoFragmentScreen() {
        IntegrationUtil.waitUIWithDelayed()
        val toolbarTitleText =
            ApplicationProvider.getApplicationContext<Context>().resources.getString(R.string.med_d_copay_title)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
    }

    /**
     * Enter CC Info:
     * Card Number: 4111 1111 1111 1111
     * Expiration: any month/year in the future
     * Name on Card: test patient’s name
     * Phone Number: should be auto-populated from test patient info
     * Email Address: enter fake email with ______@___.com
     *
     */
    fun enterCollectPaymentInfo(card: TestCards) {
        IntegrationUtil.delayedClick(withId(R.id.med_d_copay_card_number))
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.med_d_copay_card_number)),
            card.cardNumber
        )

        val month = card.expiration.substringBefore(",")
        val year = (LocalDate.now().plusYears(2).year).toString().substring(2)
        IntegrationUtil.delayedClick(withId(R.id.med_d_copay_expiration_month))
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(Matchers.containsString(month)),
            RootMatchers.isDialog()
        )
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(Matchers.containsString(year)),
            RootMatchers.isDialog()
        )

        IntegrationUtil.delayedClick(withId(R.id.med_d_copay_card_name))
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.med_d_copay_card_name)),
            card.nameOnCard
        )

        IntegrationUtil.delayedClick(withId(R.id.med_d_copay_email))
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.med_d_copay_email)),
            card.emailAddress
        )
    }

    /**
     * Tap “Save Payment Info”
     */
    fun tapSavePaymentInfoButton() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.med_d_copay_save)),
            "Save Payment Info"
        )
        IntegrationUtil.delayedClick(withId(R.id.med_d_copay_save))
    }
}
