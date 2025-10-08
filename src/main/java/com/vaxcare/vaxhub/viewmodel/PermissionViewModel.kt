/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import com.vaxcare.core.storage.preference.LocalStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val localStorage: LocalStorage
) : BaseViewModel() {
    fun saveSecret(
        serial: String,
        imei: String,
        iccid: String
    ) {
        localStorage.deviceSerialNumber = serial
        localStorage.imei = imei
        localStorage.iccid = iccid
    }
}
