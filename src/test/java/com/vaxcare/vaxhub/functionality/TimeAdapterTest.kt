/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.web.typeadapter.TimeAdapter
import org.junit.Test

/**
 * Test for TimeAdapter flexibility
 */
class TimeAdapterTest {
    private val timeAdapter = TimeAdapter()
    private val timeStrings = listOf(
        "0000-01-01T01:01:00",
        "2022-05-19T14:32:52.0435909Z",
        "2022-06-03T14:29:36.313",
        "2022-05-19T14:22:13.8462215",
        "2022-05-19T14:32:52.0435909Z",
        "2022-05-19T14:32:53.0591656Z",
        "2022-05-19T14:22:13.1430915",
        "2022-05-19T14:32:52.0435909Z",
        "2022-06-03T14:32:17.0853052",
        "2022-05-19T14:32:17.0853052",
        "2022-05-19T14:32:52.0435909Z",
        "2022-06-03T14:32:15.100859",
        "2022-05-19T14:32:15.1321077"
    )

    @Test
    fun timeAdapterTest() {
        val convertedStrings =
            timeStrings.map { string -> timeAdapter.stringToLocalDateTime(string) }
        assert(convertedStrings.none { it == null })
    }
}
