/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.core.extension.isMbiFormatAndNotNull
import org.junit.Test

class MbiRegexTest {
    @Test
    fun `MBI Regex Test`() {
        val mbis = listOf(
            "1EG4-TE5-MK79",
            "1EG4TE5MK79",
            "1EG4TE5-MK79",
            "1EG4-TE5MK79",
            "1eg4te5-mk79",
        )
        val badMbis = listOf(
            "1SG4-TE5-MK79",
            "AEG4TE5MK79",
            "1EG4TE5-LK79",
            "1EG4TEM5K79",
            "1EG4-TE5BK79",
            "0eg4te5-mk79",
        )

        assert(mbis.all { it.isMbiFormatAndNotNull() })
        assert(badMbis.none { it.isMbiFormatAndNotNull() })
    }
}
