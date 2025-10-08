/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.EligibilityUiOptions
import com.vaxcare.vaxhub.model.PaymentMode

fun ImageView.setEligibilityIcon(icon: Int, color: Int) {
    setImageResource(icon)
    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
    show()
}

fun ImageView.setEligibilityIcon(options: EligibilityUiOptions) {
    show()
    imageTintList = null
    val appointment = options.appointment

    options.overridePaymentMode?.let { override ->
        when (override) {
            PaymentMode.SelfPay -> {
                setImageResource(R.drawable.ic_one_touch_self_pay)
            }
            PaymentMode.PartnerBill -> {
                setImageResource(R.drawable.ic_oval_blue)
            }
            else -> Unit
        }
    }

    appointment.encounterState?.vaccineMessage?.getIconRes()?.let { icon ->
        setImageResource(icon)
        imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                appointment.getRiskIconColor()
            )
        )
        show()
    } ?: run {
        hide()
        return
    }
}

fun TextView.setEligibilityContent(context: Context, appointment: Appointment) {
    val colorPrimaryBlack = ContextCompat.getColor(context, R.color.primary_black)
    when {
        appointment.isVFC() -> {
            setBackgroundColor(ContextCompat.getColor(context, R.color.primary_light_green))
            text = resources.getString(R.string.vfc_risk)
            setTextColor(colorPrimaryBlack)
            show()
        }
        appointment.isSection317() -> {
            setBackgroundColor(ContextCompat.getColor(context, R.color.primary_light_yellow))
            text = resources.getString(R.string.section317_risk)
            setTextColor(colorPrimaryBlack)
            show()
        }
        else -> Unit
    }
}
