/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login.sso

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.RegexPatterns
import com.vaxcare.vaxhub.core.extension.clear
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseRestrictedFragment
import com.vaxcare.vaxhub.core.view.RoundedTextInputView
import com.vaxcare.vaxhub.databinding.FragmentSsoLoginBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.user.SessionUser
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.service.UserSessionService
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton
import com.vaxcare.vaxhub.ui.login.VerticalSpaceDecorator
import com.vaxcare.vaxhub.ui.login.adapter.SessionUserAdapter
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.LoginDestination
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.LoginViewModel
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.CachedUsersLoaded
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.UserRefreshError
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.UsersRefreshed
import com.vaxcare.vaxhub.viewmodel.LoginViewModel.LoginState.UsersRefreshing
import com.vaxcare.vaxhub.viewmodel.State
import dagger.hilt.android.AndroidEntryPoint
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : BaseRestrictedFragment<FragmentSsoLoginBinding>() {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var localStorage: LocalStorage

    @Inject
    override lateinit var sessionService: UserSessionService

    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: LoginDestination

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    companion object {
        private const val VAXCARE_DOMAIN = "@vaxcare.com"
    }

    private val args: LoginFragmentArgs by navArgs()
    private val loginViewModel: LoginViewModel by viewModels()
    private val adapter: SessionUserAdapter = SessionUserAdapter(::goToPinLogin)
    private var isConnected = true
    private var username: String? = null
    private val emailPattern = Pattern.compile(RegexPatterns.EMAIL_RFC_5322)

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_sso_login,
        hasMenu = false,
        hasToolbar = false,
        showControlPanel = false,
        showStatusBarIcons = false
    )

    override fun init(view: View, savedInstanceState: Bundle?) {
        initializeViews()
        loginViewModel.loadCachedUsers()
        loginViewModel.state.observe(viewLifecycleOwner, ::handleState)
        networkMonitor.networkStatus.observe(viewLifecycleOwner, ::handleNetworkStatus)
        observeErrorDialog()
    }

    override fun bindFragment(container: View): FragmentSsoLoginBinding = FragmentSsoLoginBinding.bind(container)

    override fun handleBack(): Boolean = false

    override fun onResume() {
        super.onResume()
        binding?.enterEmailInput?.inputField?.clear()
    }

    private fun initializeViews() {
        binding?.enterEmailInput?.inputField?.isEnabled = false
    }

    private fun handleState(state: State) {
        when (state) {
            LoadingState -> startLoading()
            UsersRefreshing -> startLoading()
            UsersRefreshed -> handleUsersSynced()
            UserRefreshError -> errorRefreshingUsers()
            is CachedUsersLoaded -> setupUi(state.users)
        }
    }

    private fun handleNetworkStatus(status: NetworkStatus) {
        isConnected = status != NetworkStatus.DISCONNECTED
        binding?.errorLabel?.isVisible = status == NetworkStatus.DISCONNECTED
    }

    private fun setupUi(cachedUsers: List<SessionUser>) {
        endLoading()
        binding?.apply {
            val emptyList = cachedUsers.isEmpty()
            itemContainer.isVisible = !emptyList
            btnClose.setOnSingleClickListener {
                destination.goBackToSplash(this@LoginFragment)
            }
            setupRecyclerViewAndItems(cachedUsers)
            enterEmailInput.setUpInputView(emptyList)
        }
    }

    private fun handleUsersSynced() {
        endLoading()
        username?.let { goToPinLogin(it) }
        username = null
    }

    private fun FragmentSsoLoginBinding.setupRecyclerViewAndItems(cachedUsers: List<SessionUser>) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LoginFragment.adapter
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    VerticalSpaceDecorator(
                        context = requireContext(),
                        heightDpDimenId = R.dimen.dp_5,
                        horizontalOffsetDpDimenId = R.dimen.dp_0
                    )
                )
            }
        }

        adapter.submitList(cachedUsers)
    }

    private fun RoundedTextInputView.setUpInputView(isListEmpty: Boolean) =
        this.apply {
            labelField.setText(
                if (isListEmpty) {
                    R.string.login_enter_email_label_empty
                } else {
                    R.string.login_enter_email_label
                }
            )

            inputField.isEnabled = true
            inputField.setOnEditorActionListener(
                TextView.OnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        if (validateEmail(inputValue)) {
                            hideKeyboard()
                            if (inputValue.containsVaxcareDomainEmail()) {
                                goToPasswordLogin(inputValue)
                            } else {
                                val doesEmailExist = loginViewModel.doesEmailExistLocally(inputValue)
                                if (!doesEmailExist) {
                                    username = inputValue
                                    refreshUsers()
                                } else {
                                    goToPinLogin(inputValue)
                                }
                            }
                        } else {
                            errorLabel.show()
                        }
                        return@OnEditorActionListener true
                    }
                    false
                }
            )
        }

    private fun refreshUsers() {
        if (isConnected) {
            loginViewModel.updateAllUsers()
        } else {
            errorRefreshingUsers()
        }
    }

    private fun errorRefreshingUsers() {
        endLoading()

        globalDestinations.goToErrorDialog(
            this@LoginFragment,
            title = R.string.login_error_title,
            body = R.string.login_error_body,
            primaryBtn = R.string.retry,
            secondaryBtn = R.string.button_cancel
        )
    }

    private fun observeErrorDialog() {
        getResultLiveData<ErrorDialogButton>(ErrorDialog.RESULT)?.observe(viewLifecycleOwner) {
            when (it) {
                ErrorDialogButton.PRIMARY_BUTTON -> {
                    refreshUsers()
                }

                ErrorDialogButton.SECONDARY_BUTTON -> {
                    username = null
                }

                else -> Unit
            }
        }
    }

    private fun goToPinLogin(username: String) {
        if (isConnected) {
            hideKeyboard()
            goToLoginPinFragmentAndGetResult(username = username)
        }
    }

    private fun goToPasswordLogin(username: String) {
        if (isConnected) {
            hideKeyboard()
            goToEnhancedPasswordAndSetResultListener(username = username)
        }
    }

    override fun onLoginSuccess(data: Int) {
        destination.goToAppointmentList(
            fragment = this,
            lookupDate = args.appointmentListDate
        )
    }

    override fun onLoginAbort(data: Int) {
        destination.goBackToSplash(this@LoginFragment)
    }

    override fun onLoginFailure(data: Int) {
        destination.goBackToSplash(this@LoginFragment)
    }

    private fun validateEmail(text: String) = emailPattern.matcher(text).matches()

    private fun String.containsVaxcareDomainEmail() = this.contains(VAXCARE_DOMAIN, true)
}
