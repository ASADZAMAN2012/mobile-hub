/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

sealed class AppointmentNavigateStatus {
    object AppointmentNavigateToCheckout : AppointmentNavigateStatus()

    object AppointmentNavigateToDob : AppointmentNavigateStatus()
}
