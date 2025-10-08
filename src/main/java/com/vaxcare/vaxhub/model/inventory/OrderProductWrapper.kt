/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory

import android.content.Context
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.order.OrderReasons
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderProductWrapper(
    val reasonContexts: Set<DoseReasonContext>,
    val products: List<OrderDose>,
    val appointmentId: Int
) : Parcelable

@Parcelize
data class OrderDose(
    private val rawDisplay: String,
    val salesProductId: Int,
    val orderNumber: String?,
    val iconResId: Int,
    val reasonContext: DoseReasonContext,
    var selectedReason: OrderReasons? = null
) : Parcelable {
    fun getDisplayName(context: Context): Spannable {
        val index = rawDisplay.indexOf("(")
        val sb = SpannableStringBuilder(rawDisplay)
        val style = TextAppearanceSpan(context, R.style.H6BoldBlack)
        val style2 = TextAppearanceSpan(context, R.style.H6RegBlack)

        if (index == -1) {
            sb.setSpan(
                style,
                0,
                rawDisplay.length - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        } else {
            sb.setSpan(
                style,
                0,
                index - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            sb.setSpan(
                style2,
                index,
                rawDisplay.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
        return sb
    }
}

@Parcelize
enum class DoseReasonContext : Parcelable {
    DOSES_NOT_ORDERED,
    ORDER_UNFILLED,
    NONE
}
