/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import android.os.Parcelable

/**
 * Class for handling flows dealing with correcting missing or invalid info fields from either
 * patient demographics info or payer info
 */
interface InfoField : Parcelable {
    /**
     * String value of the field
     */
    var currentValue: String?

    /**
     * @return The path parameter for the Patch endpoint
     */
    fun getPatchPath(): String
}
