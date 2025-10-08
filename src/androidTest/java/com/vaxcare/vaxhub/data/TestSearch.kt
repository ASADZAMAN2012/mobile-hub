/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

sealed class TestSearch(
    val letter: String,
) {
    // Johnny Vaxtest
    object FirstEnter : TestSearch("Joh")

    object SecondEnter : TestSearch("Johnny Vaxtest")
}
