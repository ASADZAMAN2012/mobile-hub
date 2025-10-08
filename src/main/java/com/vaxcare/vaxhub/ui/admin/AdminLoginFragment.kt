/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.admin

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.text.method.TransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.ui.extension.show
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentAdminLoginBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.AdminDestination
import com.vaxcare.vaxhub.viewmodel.AdminViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdminLoginFragment : BaseFragment<FragmentAdminLoginBinding>() {
    private val adminViewModel: AdminViewModel by viewModels()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var localStorage: LocalStorage

    @Inject
    lateinit var destination: AdminDestination

    override val fragmentProperties = FragmentProperties(
        resource = R.layout.fragment_admin_login,
        hasToolbar = false
    )

    override fun bindFragment(container: View) = FragmentAdminLoginBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.apply {
            passwordInput.requestFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(passwordInput, InputMethodManager.SHOW_IMPLICIT)

            toolbar.onCloseAction = {
                destination.goBackToSplash(this@AdminLoginFragment)
            }

            btnLogin.setOnClickListener { doLogin() }

            passwordInput.setOnClickListener {
                passwordInput.setSelection(passwordInput.length())
            }

            passwordInput.addTextChangedListener {
                errorLabel.hide()
                if (passwordInput.text.toString().trim().isEmpty()) {
                    enterAdminPassword.visibility = View.VISIBLE
                } else {
                    enterAdminPassword.visibility = View.GONE
                }
            }

            passwordInput.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            doLogin()
                            true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }

            passwordInput.transformationMethod = object : TransformationMethod {
                override fun onFocusChanged(
                    view: View?,
                    sourceText: CharSequence?,
                    focused: Boolean,
                    direction: Int,
                    previouslyFocusedRect: Rect?
                ) {
                }

                override fun getTransformation(source: CharSequence, view: View?): CharSequence {
                    return AsteriskPassword(source)
                }
            }

            serialAndVersionInfo.text =
                getString(
                    R.string.serial_and_version_info,
                    localStorage.deviceSerialNumber,
                    BuildConfig.VERSION_NAME
                )

            validateScannerLicense.paint.flags = Paint.UNDERLINE_TEXT_FLAG
            validateScannerLicense.paint.isAntiAlias = true
            validateScannerLicense.setOnClickListener {
                destination.goToValidateLicense(this@AdminLoginFragment)
            }
        }
    }

    private fun doLogin() {
        if (binding?.passwordInput?.text?.toString()?.isEmpty() == true) {
            return
        }
        adminViewModel.validatePassword(
            binding?.passwordInput?.text?.toString() ?: ""
        ) { validated ->
            if (validated) {
                destination.goToAdminDetail(this@AdminLoginFragment)
            } else {
                binding?.errorLabel?.text = getString(R.string.admin_fragment_invalid_credentials)
                binding?.errorLabel?.show()
            }
        }
    }

    override fun onDestroyView() {
        hideKeyboard()
        super.onDestroyView()
    }

    class AsteriskPassword(private val source: CharSequence) : CharSequence {
        override val length: Int
            get() = source.length

        override fun get(index: Int): Char {
            return '*'
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return source.subSequence(startIndex, endIndex)
        }
    }
}
