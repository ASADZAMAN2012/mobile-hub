/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientEditPhoneCollectedViewModel @Inject constructor() : BaseViewModel() {
    companion object {
        private const val TIMEOUT = 3000L
    }

    sealed class PhoneCollectedState : State {
        object MoveForward : PhoneCollectedState()
    }

    fun startTimer() =
        viewModelScope.launch(Dispatchers.IO) {
            delay(TIMEOUT)
            setState(PhoneCollectedState.MoveForward)
        }
}
