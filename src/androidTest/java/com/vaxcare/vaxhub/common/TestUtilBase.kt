/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.vaxcare.vaxhub.HiltEntryPoint
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.simpleClick

open class TestUtilBase : HiltEntryPoint {
    /**
     * Types string into keyPad and presses 'Enter' key
     *
     * @param ID string to be typed in (partnerID/NumberOfDoses to add)
     */
    fun enterKeyPad(ID: String) {
        val keyPadChars: CharArray = ID.toCharArray()
        keyPadChars.forEach { c -> clickKeys(c) }
        simpleClick(onView(withId(R.id.button_enter)))
    }

    /**
     * Enter the passed in string value
     *
     * @param ID value to enter
     */
    fun clickButtonsOnKeyPad(ID: String) {
        val keyPadChars: CharArray = ID.toCharArray()
        keyPadChars.forEach { c -> clickKeys(c) }
    }

    /**
     * Types each char on the keypad
     *
     * @param c Char to be typed in
     */
    private fun clickKeys(c: Char) {
        when (c) {
            '0' -> simpleClick(onView(withId(R.id.button_zero)))
            '1' -> simpleClick(onView(withId(R.id.button_one)))
            '2' -> simpleClick(onView(withId(R.id.button_two)))
            '3' -> simpleClick(onView(withId(R.id.button_three)))
            '4' -> simpleClick(onView(withId(R.id.button_four)))
            '5' -> simpleClick(onView(withId(R.id.button_five)))
            '6' -> simpleClick(onView(withId(R.id.button_six)))
            '7' -> simpleClick(onView(withId(R.id.button_seven)))
            '8' -> simpleClick(onView(withId(R.id.button_eight)))
            '9' -> simpleClick(onView(withId(R.id.button_nine)))
            else -> throw IllegalArgumentException("Character not found on KeyPad")
        }
    }
}
