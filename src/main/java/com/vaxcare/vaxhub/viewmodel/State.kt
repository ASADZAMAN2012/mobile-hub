/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.vaxcare.vaxhub.ui.idlingresource.HubIdlingResource
import timber.log.Timber

/**
 * Use the State interface and the state member variables to carry out UI changes on the View.
 * For one example see BuybackReviewViewModel's State sealed classes and BuybackReviewFragment
 * observer.
 */
interface State

/**
 * To be used to reset the state of the view model after navigation for example
 */
object Reset : State

/**
 * To be used to display spinning mark for loading
 */
object LoadingState : State

abstract class BaseViewModel : ViewModel() {
    /**
     * Recommended we observe on state LiveData and make changes on UI
     */
    private val _state = MutableLiveData<State>()
    open val state: LiveData<State>
        get() = _state.distinctUntilChanged()

    protected open val idlingResource: HubIdlingResource?
        get() = HubIdlingResource.instance

    protected val loadingStates: MutableList<State> = mutableListOf(LoadingState)

    protected fun setState(state: State) {
        _state.postValue(state)
        idlingResource?.setIdle(state !in loadingStates)
    }

    open fun resetState() {
        Timber.v("Resetting the state")
        _state.postValue(Reset)
        idlingResource?.setIdle(true)
    }

    override fun onCleared() {
        resetState()
        super.onCleared()
    }
}
