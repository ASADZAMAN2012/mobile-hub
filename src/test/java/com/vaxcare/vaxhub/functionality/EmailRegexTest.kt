/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.core.constant.RegexPatterns
import org.junit.Test
import java.util.regex.Pattern

class EmailRegexTest {
    private val validEmails = listOf(
        "TEST@TEST.COM",
        "simple@example.com",
        "very.common@example.com",
        "disposable.style.email.with+symbol@example.com",
        "other.email-with-hyphen@example.com",
        "x@example.com",
        "example-indeed@strange-example.com",
        "\"john..doe\"@example.org",
        "\" \"@example.org",
        "t'chanda@g.com",
        "\" jessie_pinkman_ \"@yahooo.com"
    )

    private val invalidEmails = listOf(
        "this\\ is\\\"not\\\\allowed@example.com",
        "this is\"not\\allowed@example.com",
        "Abc.example.com",
        "A@b@c@example.com"
    )

    @Test
    fun validEmailTest() {
        validEmails.forEach { email ->
            val valid = Pattern.compile(RegexPatterns.EMAIL_RFC_5322).matcher(email).matches()
            assert(valid) { "Email $email not valid" }
        }
    }

    @Test
    fun invalidEmailTest() {
        invalidEmails.forEach { email ->
            val invalid = !Pattern.compile(RegexPatterns.EMAIL_RFC_5322).matcher(email).matches()
            assert(invalid) { "Email $email is valid" }
        }
    }
}
