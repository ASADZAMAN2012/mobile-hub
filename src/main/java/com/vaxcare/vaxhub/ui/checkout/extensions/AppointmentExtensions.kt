/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.extensions

import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.enums.AppointmentNavigateStatus
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus
import com.vaxcare.vaxhub.model.getInventorySource
import com.vaxcare.vaxhub.model.metric.CheckoutAppointmentOpenedMetric

fun Appointment.toAppointmentNavigateStatus(): AppointmentNavigateStatus =
    if (!checkedOut && (patient.dob.isNullOrBlank())) {
        AppointmentNavigateStatus.AppointmentNavigateToDob
    } else {
        AppointmentNavigateStatus.AppointmentNavigateToCheckout
    }

fun Appointment.checkoutStatus() =
    when {
        this.isEditable == false -> EditCheckoutStatus.VIEW_CHECKOUT
        this.checkedOut -> EditCheckoutStatus.PAST_CHECKOUT
        else -> EditCheckoutStatus.ACTIVE_CHECKOUT
    }

/**
 * Get color resId depending on Vaccine Supply
 */
fun Appointment.vaccineSupplyColor(): Int =
    when {
        isPrivate() -> R.color.primary_purple
        isVFC() -> R.color.primary_green
        isState() -> R.color.primary_magenta
        isSection317() -> R.color.primary_blue
        else -> R.color.primary_purple
    }

fun Appointment.getRiskIconColor(): Int =
    encounterState?.vaccineMessage?.getIconRes().let { icon ->
        when (icon) {
            R.drawable.ic_vax3_eligibility_self_pay -> R.color.primary_coral
            R.drawable.ic_vax3_eligibility_issue_ic -> R.color.primary_yellow
            else -> vaccineSupplyColor()
        }
    }

/**
 * Get faded color resId depending on Vaccine Supply
 */
fun Appointment.vaccineSupplyFadedColor(): Int =
    when {
        isPrivate() -> R.color.list_purple
        isVFC() -> R.color.faded_list_green
        isState() -> R.color.faded_list_magenta
        isSection317() -> R.color.lightest_blue
        else -> R.color.primary_purple
    }

fun Appointment.toCheckoutAppointmentOpenedMetric(isForceRiskFree: Boolean): CheckoutAppointmentOpenedMetric =
    CheckoutAppointmentOpenedMetric(
        patientId = patient.id,
        stock = getInventorySource(),
        patientVisitId = id,
        isCheckedOut = checkedOut,
        paymentMethod = paymentMethod.printableName,
        ssn = patient.ssn,
        mbi = patient.paymentInformation?.mbi,
        medDCta = encounterState?.medDMessage?.callToAction?.javaClass?.simpleName,
        riskAssessmentId = if (isForceRiskFree) {
            FORCED_RISK_FREE_ASSESSMENT_ID
        } else {
            encounterState?.medDMessage?.riskAssessmentId
        },
        vaccineCta = encounterState?.vaccineMessage?.callToAction?.javaClass?.simpleName,
        larcCta = encounterState?.larcMessage?.callToAction?.javaClass?.simpleName,
        editCheckoutStatus = checkoutStatus(),
        patientOrderCountTotal = orders.size,
        patientOrderCountOutstanding = orders.filter { it.patientVisitId == null }.size
    )

const val FORCED_RISK_FREE_ASSESSMENT_ID = -3
