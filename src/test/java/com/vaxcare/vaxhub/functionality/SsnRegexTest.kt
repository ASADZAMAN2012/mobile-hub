/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.core.extension.isFullSsnAndNotNull
import com.vaxcare.vaxhub.ui.checkout.MedDCheckFragment
import org.junit.Test

class SsnRegexTest {
    @Test
    fun `Legacy SSN Regex Test`() {
        val pattern = MedDCheckFragment.SSN_REGEX_PATERN
        val ssns = listOf(
            "000-00-0000",
            "100120204",
            "51198-4652",
            "123450123",
            "321-991234",
            ""
        )
        val regex = pattern.toRegex()
        assert(ssns.all { regex.matches(it) })
    }

    @Test
    fun `SSN Extension Regex Test`() {
        val ssns = listOf(
            "000-00-0000",
            "100120204",
            "51198-4652",
            "123450123",
            "321-991234"
        )
        val badSsns = listOf(
            "123-E3-9999",
            "123E39999",
            "123-E39999",
            "123E3-9999",
            "123E38"
        )

        assert(ssns.all { it.isFullSsnAndNotNull() })
        assert(badSsns.none { it.isFullSsnAndNotNull() })
    }
}
