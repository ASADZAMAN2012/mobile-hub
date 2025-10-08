/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login.sso

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentSsoResetConfirmationBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.LoginDestination
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.LoginViewModel
import com.vaxcare.vaxhub.viewmodel.State
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ResetPinConfirmFragment : BaseFragment<FragmentSsoResetConfirmationBinding>() {
    private val args: ResetPinConfirmFragmentArgs by navArgs()
    private val viewModel: LoginViewModel by viewModels()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: LoginDestination

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_sso_reset_confirmation,
        hasMenu = false,
        hasToolbar = false,
        showControlPanel = false,
        showStatusBarIcons = false
    )

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        setupUi()
    }

    private fun handleState(state: State) {
        when (state) {
            LoginViewModel.LoginState.Idle, LoadingState -> showLoading()
            is LoginViewModel.LoginState.ResetResult -> {
                hideLoading()
                if (state.isSuccessful) {
                    binding?.setupSuccessUi()
                } else {
                    binding?.setupFailureUi()
                }
            }
        }
    }

    private fun showLoading() {
        binding?.resendLoadingContainer?.show()
    }

    private fun hideLoading() {
        binding?.resendLoadingContainer?.hide()
    }

    private fun setupUi() {
        binding?.apply {
            resetLogOut.setOnSingleClickListener {
                destination.goBackToSplash(this@ResetPinConfirmFragment)
            }

            resetPin.setOnSingleClickListener {
                viewModel.resetUser(args.username)
            }

            if (args.success) {
                setupSuccessUi()
            } else {
                setupFailureUi()
            }
        }
    }

    private fun FragmentSsoResetConfirmationBinding.setupSuccessUi() {
        greenCheckImage.show()
        resetTitle.setText(R.string.reset_email_sent)
        resetTooltip.setText(R.string.reset_resend_tooltip)
    }

    private fun FragmentSsoResetConfirmationBinding.setupFailureUi() {
        greenCheckImage.hide()
        resetTitle.setText(R.string.reset_unable_to_send)
        resetTooltip.setText(R.string.reset_try_again_later)
    }

    override fun handleBack(): Boolean = true

    override fun bindFragment(container: View): FragmentSsoResetConfirmationBinding =
        FragmentSsoResetConfirmationBinding.bind(container)
}
