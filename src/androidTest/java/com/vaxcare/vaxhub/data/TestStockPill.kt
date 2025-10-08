/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.R

sealed class TestStockPill(
    var text: String,
    val textColor: Int,
    val backgroundDrawable: Int,
    val tintColor: Int?
) {
    object PrivateStockPill : TestStockPill(
        text = "PRIVATE",
        textColor = R.color.primary_purple,
        backgroundDrawable = R.drawable.bg_rounded_corner_purple,
        tintColor = R.color.list_purple
    )

    object ThreeOneSevenPill : TestStockPill(
        text = "317",
        textColor = R.color.primary_blue,
        backgroundDrawable = R.drawable.bg_rounded_corner_purple,
        tintColor = R.color.list_blue
    )

    object VFCStockPill : TestStockPill(
        text = "VFC",
        textColor = R.color.primary_green,
        backgroundDrawable = R.drawable.bg_rounded_corner_purple,
        tintColor = R.color.list_green
    )

    object MedDTag : TestStockPill(
        text = "MED D",
        textColor = R.color.primary_black,
        backgroundDrawable = R.drawable.bg_rounded_corner_gray,
        tintColor = null
    )

    object OrderedOneDose : TestStockPill(
        text = "",
        textColor = R.color.primary_white,
        backgroundDrawable = R.drawable.bg_rounded_corner_purple,
        tintColor = R.color.primary_purple
    ) {
        fun changeText(newText: String): OrderedOneDose {
            this.text = newText
            return this
        }
    }
}
