/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login

enum class PinLockAction {
    LOGIN;

    companion object {
        private val map = values().associateBy(PinLockAction::ordinal)

        fun fromInt(type: Int) = map[type] ?: LOGIN
    }
}
