/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.adapter.AppointmentListFlags

/**
 * Callback interface for handling touch events in appointment list
 */
interface AppointmentClickListener {
    /**
     * On appointment item clicked
     *
     * @param appointment the appointment
     * @param flags the features flags related to the appointment
     */
    fun onItemClicked(appointment: Appointment, flags: AppointmentListFlags)
}
