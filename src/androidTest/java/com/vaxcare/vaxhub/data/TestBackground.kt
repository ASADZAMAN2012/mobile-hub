/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.R

sealed class TestBackground(val backgroundRes: Int, val tintColor: Int? = null) {
    object None : TestBackground(0, null)

    object PrimaryPurple : TestBackground(R.drawable.bg_rounded_corner_purple, R.color.list_purple)

    object HighLightPurple : TestBackground(R.drawable.bg_rounded_corner_purple, null)

    object PrimaryWhite : TestBackground(R.drawable.bg_rounded_corner_white, R.color.primary_white)
}
