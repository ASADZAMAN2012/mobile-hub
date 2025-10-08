/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login.sso

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.work.WorkManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentSsoPinBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.LoginResult
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.LoginDestination
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.LoginViewModel
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.AccountLocked
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.BadLogin
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.Idle
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.OktaError
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.ResetResult
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.SuccessfulLogin
import com.vaxcare.vaxhub.viewmodel.State
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginPinFragment : BaseFragment<FragmentSsoPinBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: LoginDestination

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var hiltWorkManagerListener: HiltWorkManagerListener

    private val args: LoginPinFragmentArgs by navArgs()
    private val viewModel: LoginViewModel by viewModels()
    private var loadingJob: Job? = null

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_sso_pin,
        hasMenu = false,
        hasToolbar = false,
        showControlPanel = false,
        showStatusBarIcons = false
    )

    companion object {
        private const val MINIMUM_PIN_LENGTH = 6
        const val LOGIN_PIN_FRAGMENT_RESULT_KEY = "loginPinFragmentResultKey"
        const val LOGIN_PIN_FRAGMENT_BUNDLE_KEY = "loginPinFragmentBundleKey"
        const val LOGIN_PIN_FRAGMENT_BUNDLE_EXTRA_KEY = "loginPinFragmentBundleExtraKey"
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        binding?.apply {
            val titleResId = args.options.titleResourceId
            if (titleResId != 0) {
                enterPinLabel.setText(titleResId)
            }
            resetPin.setOnSingleClickListener { onResetPinClick() }
            resetPin.isGone = args.options.disableReset
            btnClose.setOnSingleClickListener { setFragmentResultAndGoBack(LoginResult.ABORT) }
            lockKeypad.apply {
                disableEnterKey()
                onBack = { destination.goBackToSplash(this@LoginPinFragment) }
                onEnterKeyTap = { pin -> viewModel.loginUser(args.options.username, pin) }
                onInputTextChanged = { pin ->
                    if (pin.length >= MINIMUM_PIN_LENGTH) {
                        enableEnterKey()
                    } else {
                        disableEnterKey()
                    }
                }
            }
        }
    }

    private fun handleState(state: State) {
        if (state != LoadingState) {
            endLoading()
        }

        stopLoadingJob()

        when (state) {
            Idle -> disableAll()
            LoadingState -> startLoading()
            SuccessfulLogin -> onSuccessfulLogin()
            BadLogin -> onBadLogin()
            AccountLocked -> onAccountLocked()
            OktaError -> {
                showError(R.string.login_unable_to_login)
                binding?.resetPin?.text = getText(R.string.loginPinFragment_changePin)
            }
            is ResetResult -> onResetSuccess(state.isSuccessful)
        }
    }

    override fun onLoadingStart() {
        binding?.loadingBackground?.show()
        loadingJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000L)
                viewModel.checkFromLoading()
            }
        }
    }

    override fun onLoadingStop() {
        binding?.loadingBackground?.hide()
        enableAll()
    }

    override fun handleBack(): Boolean = true

    private fun stopLoadingJob() {
        loadingJob?.cancel()
        loadingJob = null
    }

    private fun onSuccessfulLogin() {
        context?.let {
            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = WorkManager.getInstance(it),
                parameters = OneTimeParams.PingJob,
                listener = hiltWorkManagerListener
            )
        }

        setFragmentResultAndGoBack(LoginResult.SUCCESS)
    }

    private fun onBadLogin() {
        binding?.apply {
            showError(R.string.login_incorrect_pin)
            binding?.resetPin?.text = getText(R.string.loginPinFragment_changePin)
        }
    }

    private fun onAccountLocked() {
        binding?.apply {
            lockKeypad.isEnabled = false
            lockKeypad.alpha = 0.5f
            lockKeypad.clearInput()
            showError(R.string.login_too_many_attempts)
            resetTooltip.show()
            resetPin.show()
            resetPin.setText(R.string.login_reset_pin)
        }
    }

    private fun disableAll() {
        binding?.apply {
            lockKeypad.isEnabled = false
            btnClose.isEnabled = false
            resetPin.isEnabled = false
        }
    }

    private fun enableAll() {
        binding?.apply {
            lockKeypad.isEnabled = true
            btnClose.isEnabled = true
            resetPin.isEnabled = true
        }
    }

    private fun showError(resId: Int) {
        binding?.apply {
            lockKeypad.clearInput()
            errorLabel.setText(resId)
            errorLabel.show()
            enterPinLabel.hide()
        }
    }

    private fun onResetPinClick() {
        viewModel.resetUser(args.options.username)
    }

    private fun onResetSuccess(isSuccessful: Boolean) {
        destination.goToResetPinConfirmFromPinFragment(this, args.options.username, isSuccessful)
    }

    private fun setFragmentResultAndGoBack(result: LoginResult) {
        setFragmentResult(
            LOGIN_PIN_FRAGMENT_RESULT_KEY,
            bundleOf(
                LOGIN_PIN_FRAGMENT_BUNDLE_KEY to result.ordinal,
                LOGIN_PIN_FRAGMENT_BUNDLE_EXTRA_KEY to args.options.loginCorrelation
            )
        )

        globalDestinations.goBack(this@LoginPinFragment)
    }

    override fun bindFragment(container: View): FragmentSsoPinBinding = FragmentSsoPinBinding.bind(container)
}
