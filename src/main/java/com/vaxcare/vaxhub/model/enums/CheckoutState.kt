/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

enum class CheckoutState {
    DEFAULT,
    START,
    LOADING,
    LOADED;

    fun isDefault() = this == DEFAULT

    fun isStarted() = this == START

    fun isLoading() = this == LOADING

    fun isLoaded() = this == LOADED
}
