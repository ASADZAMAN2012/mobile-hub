/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.core.extension.to
import com.vaxcare.vaxhub.data.MockCopays
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import org.junit.Test

class MedDCopayTest {
    @Test
    fun `MedD Copay Display String Test`() {
        val coveredAntigen = MedDVaccines.ZOSTER.value
        val mockResponse = MockCopays.responseZosterOnly
        val noCopayFound = "No Copay Found!"
        val displayStrings = mockResponse.copays.map { it.antigen to it.getDisplayString { noCopayFound } to it.copay }
        displayStrings.forEach { (antigen, displayString, copay) ->
            val expected = if (antigen.value == coveredAntigen) {
                "$${copay.setScale(2)}"
            } else {
                noCopayFound
            }

            assert(expected == displayString)
        }
    }
}
