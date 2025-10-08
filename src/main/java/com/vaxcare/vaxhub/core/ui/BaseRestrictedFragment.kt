/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.os.Bundle
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.viewbinding.ViewBinding
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getEnum
import com.vaxcare.vaxhub.model.enums.LoginResult
import com.vaxcare.vaxhub.service.UserSessionService
import com.vaxcare.vaxhub.ui.login.sso.LoginPinFragment
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import timber.log.Timber

abstract class BaseRestrictedFragment<VB : ViewBinding> : BaseFragment<VB>() {
    protected abstract val localStorage: LocalStorage
    protected abstract val sessionService: UserSessionService
    protected abstract val globalDestinations: GlobalDestinations

    /**
     * Verifies if the current session is valid, authenticated and not locked out
     */
    protected fun isSessionAuthenticated() = sessionService.isSessionAuthenticated()

    /**
     * Check if the session is authenticated. If it is not, navigate to the LoginPinFragment and
     * run the pin in flow. Otherwise, invoke the actionLoginSuccess callback
     *
     * @param username the username to pass to LoginPinFragment
     * @param titleResId the title resource id for LoginPinFragment
     */
    protected fun checkSessionNavigation(
        username: String = localStorage.userName,
        titleResId: Int,
        correlation: Int
    ) {
        if (isSessionAuthenticated()) {
            onLoginSuccess(correlation)
        } else {
            goToLoginPinFragmentAndGetResult(
                username = username,
                titleResId = titleResId,
                correlation = correlation,
                disableReset = true
            )
        }
    }

    protected open fun onLoginSuccess(data: Int) {
        Timber.d("User Logged in successfully")
    }

    protected open fun onLoginAbort(data: Int) {
        Timber.d("User aborted login")
    }

    protected open fun onLoginFailure(data: Int) {
        Timber.e("There was a problem logging back in from ${this::class.java.simpleName}")
        globalDestinations.goBackToSplash(this)
    }

    protected fun goToLoginPinFragmentAndGetResult(
        username: String = localStorage.userName,
        titleResId: Int = R.string.loginPinFragment_enterYourPin,
        correlation: Int = -1,
        disableReset: Boolean = false
    ) {
        setFragmentResultListener(
            requestKey = LoginPinFragment.LOGIN_PIN_FRAGMENT_RESULT_KEY,
            listener = ::onLoginPinFragmentResult
        )
        globalDestinations.goToEnhancedPinIn(
            fragment = this,
            username = username,
            titleResId = titleResId,
            correlation = correlation,
            disableReset = disableReset
        )
    }

    protected fun goToEnhancedPasswordAndSetResultListener(
        username: String = localStorage.userName,
        titleResId: Int = R.string.loginPinFragment_enterYourPassword,
        correlation: Int = -1,
        disableReset: Boolean = false
    ) {
        setFragmentResultListener(
            requestKey = LoginPinFragment.LOGIN_PIN_FRAGMENT_RESULT_KEY,
            listener = ::onLoginPinFragmentResult
        )
        globalDestinations.goToEnhancedPassword(
            fragment = this,
            username = username,
            titleResId = titleResId,
            correlation = correlation,
            disableReset = disableReset
        )
    }

    private fun onLoginPinFragmentResult(action: String, result: Bundle) {
        clearFragmentResult(action)
        val loginResult =
            result.getEnum(LoginPinFragment.LOGIN_PIN_FRAGMENT_BUNDLE_KEY, LoginResult.DEFAULT)
        val extraData = result.getInt(LoginPinFragment.LOGIN_PIN_FRAGMENT_BUNDLE_EXTRA_KEY)
        when (loginResult) {
            LoginResult.SUCCESS -> onLoginSuccess(extraData)
            LoginResult.ABORT -> onLoginAbort(extraData)
            LoginResult.FAILURE -> onLoginFailure(extraData)
            LoginResult.DEFAULT -> return
        }
    }
}
