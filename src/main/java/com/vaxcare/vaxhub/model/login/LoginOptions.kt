/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.login

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoginOptions(
    val username: String,
    val titleResourceId: Int = 0,
    val loginCorrelation: Int = -1,
    val disableReset: Boolean = false
) : Parcelable
