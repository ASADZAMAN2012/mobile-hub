/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login.sso

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.method.TransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
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
import com.vaxcare.vaxhub.databinding.FragmentSsoPasswordBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.LoginResult
import com.vaxcare.vaxhub.ui.admin.AdminLoginFragment
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.LoginDestination
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.LoginViewModel
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
class LoginPasswordFragment : BaseFragment<FragmentSsoPasswordBinding>() {
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
        resource = R.layout.fragment_sso_password,
        hasMenu = false,
        hasToolbar = false,
        showControlPanel = false,
        showStatusBarIcons = false
    )

    companion object {
        const val LOGIN_PIN_FRAGMENT_RESULT_KEY = "loginPinFragmentResultKey"
        const val LOGIN_PIN_FRAGMENT_BUNDLE_KEY = "loginPinFragmentBundleKey"
        const val LOGIN_PIN_FRAGMENT_BUNDLE_EXTRA_KEY = "loginPinFragmentBundleExtraKey"
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        binding?.apply {
            val titleResId = args.options.titleResourceId
            if (titleResId != 0) {
                textViewEnterPassword.setText(titleResId)
            }
            fragmentLayoutButtonClose.setOnSingleClickListener {
                setFragmentResultAndGoBack(
                    LoginResult.ABORT
                )
            }

            editTextPassword.requestFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextPassword, InputMethodManager.SHOW_IMPLICIT)

            editTextPassword.setOnClickListener {
                editTextPassword.setSelection(editTextPassword.length())
            }

            editTextPassword.addTextChangedListener {
                textViewError.hide()
                if (editTextPassword.text.toString().trim().isEmpty()) {
                    textViewPassword.visibility = View.VISIBLE
                } else {
                    textViewPassword.visibility = View.GONE
                }
            }

            editTextPassword.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            viewModel.loginUser(
                                args.options.username,
                                binding?.editTextPassword?.text?.toString() ?: ""
                            )
                            true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }

            editTextPassword.transformationMethod = object : TransformationMethod {
                override fun onFocusChanged(
                    view: View?,
                    sourceText: CharSequence?,
                    focused: Boolean,
                    direction: Int,
                    previouslyFocusedRect: Rect?
                ) {
                }

                override fun getTransformation(source: CharSequence, view: View?): CharSequence {
                    return AdminLoginFragment.AsteriskPassword(source)
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
            LoginViewModel.LoginState.Idle -> disableAll()
            LoadingState -> startLoading()
            LoginViewModel.LoginState.SuccessfulLogin -> onSuccessfulLogin()
            LoginViewModel.LoginState.BadLogin -> onBadLogin()
            LoginViewModel.LoginState.AccountLocked -> onAccountLocked()
            LoginViewModel.LoginState.OktaError -> {
                showError(R.string.login_unable_to_login)
            }
        }
    }

    override fun onLoadingStart() {
        binding?.frameLayoutLoading?.show()
        loadingJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000L)
                viewModel.checkFromLoading()
            }
        }
    }

    override fun onLoadingStop() {
        binding?.frameLayoutLoading?.hide()
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
            showError(R.string.loginPasswordFragment_incorrectPassword)
        }
    }

    private fun onAccountLocked() {
        binding?.apply {
            showError(R.string.login_too_many_attempts)
        }
    }

    private fun disableAll() {
        binding?.apply {
            fragmentLayoutButtonClose.isEnabled = false
        }
    }

    private fun enableAll() {
        binding?.apply {
            fragmentLayoutButtonClose.isEnabled = true
        }
    }

    private fun showError(resId: Int) {
        binding?.apply {
            textViewError.setText(resId)
            textViewError.show()
            textViewEnterPassword.hide()
        }
    }

    private fun setFragmentResultAndGoBack(result: LoginResult) {
        setFragmentResult(
            LoginPinFragment.LOGIN_PIN_FRAGMENT_RESULT_KEY,
            bundleOf(
                LoginPinFragment.LOGIN_PIN_FRAGMENT_BUNDLE_KEY to result.ordinal,
                LoginPinFragment.LOGIN_PIN_FRAGMENT_BUNDLE_EXTRA_KEY to args.options.loginCorrelation
            )
        )

        globalDestinations.goBack(this@LoginPasswordFragment)
    }

    override fun bindFragment(container: View): FragmentSsoPasswordBinding = FragmentSsoPasswordBinding.bind(container)
}
