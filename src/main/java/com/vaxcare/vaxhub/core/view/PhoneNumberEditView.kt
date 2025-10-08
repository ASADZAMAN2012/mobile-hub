/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.hideKeyboard
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.ViewPhoneNumberEditBinding

class PhoneNumberEditView(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs) {
    private val binding: ViewPhoneNumberEditBinding =
        ViewPhoneNumberEditBinding.inflate(context.getLayoutInflater(), this, true)

    var onPhoneChanged: (phone: String) -> Unit = { }

    var phone: String
        get() {
            return binding.phoneStart.text.toString() +
                binding.phoneMid.text.toString() +
                binding.phoneEnd.text.toString()
        }
        set(phoneNumber) {
            val phoneArray = phoneNumber.split("-")
            if (phoneArray.size == 3) {
                binding.phoneStart.setText(phoneArray[0])
                binding.phoneMid.setText(phoneArray[1])
                binding.phoneEnd.setText(phoneArray[2])
            }
        }

    fun setFocus() {
        if (binding.phoneStart.text.length != 3) {
            binding.phoneStart.requestFocus()
        } else if (binding.phoneMid.text.length != 3) {
            binding.phoneMid.requestFocus()
        } else {
            binding.phoneEnd.requestFocus()
        }
    }

    private val onEditTextFocusChangeListener = OnFocusChangeListener { view, b ->
        if (b) {
            (view as EditText).setSelection(view.text.length)
        }
    }

    init {
        val styleable = context.obtainStyledAttributes(attrs, R.styleable.PhoneNumberEditView)
        val required = styleable.getBoolean(R.styleable.PhoneNumberEditView_required, false)

        if (required) {
            binding.required.visibility = View.VISIBLE
        } else {
            binding.required.visibility = View.INVISIBLE
        }

        addListeners()
    }

    private fun doOnPhoneChanged(phone: String) {
        hideInvalidState()
        onPhoneChanged(phone)
    }

    fun addTextChangedListener() {
        binding.phoneStart.doAfterTextChanged {
            if (binding.phoneStart.text.length == 3) {
                binding.phoneMid.requestFocus()
            }
            doOnPhoneChanged(phone)
        }

        binding.phoneMid.doAfterTextChanged {
            if (binding.phoneMid.text.length == 3) {
                binding.phoneEnd.requestFocus()
            } else if (binding.phoneMid.text.isEmpty()) {
                binding.phoneStart.requestFocus()
            }
            doOnPhoneChanged(phone)
        }

        binding.phoneEnd.doAfterTextChanged {
            if (binding.phoneEnd.text.length == 4) {
                hideKeyboard()
            } else if (binding.phoneEnd.text.isEmpty()) {
                if (binding.phoneMid.text.isEmpty()) {
                    binding.phoneStart.requestFocus()
                } else {
                    binding.phoneMid.requestFocus()
                }
            }
            doOnPhoneChanged(phone)
        }
    }

    fun validate(): Boolean {
        if (binding.phoneStart.text.isNotBlank() &&
            binding.phoneMid.text.isNotBlank() &&
            binding.phoneEnd.text.isNotBlank() &&
            phone.length == 10
        ) {
            return true
        } else {
            showInvalidState()
            return false
        }
    }

    fun isNotEmpty(): Boolean {
        return binding.phoneStart.text.isNotEmpty() ||
            binding.phoneMid.text.isNotEmpty() ||
            binding.phoneEnd.text.isNotEmpty()
    }

    private fun showInvalidState() {
        binding.apply {
            phoneStart.setBackgroundResource(R.drawable.bg_rounded_corner_left_error)
            phoneMid.setBackgroundResource(R.drawable.bg_rounded_corner_mid_error)
            phoneEnd.setBackgroundResource(R.drawable.bg_rounded_corner_right_error)
            errorLabel.show()
        }
    }

    private fun hideInvalidState() {
        binding.apply {
            phoneStart.setBackgroundResource(R.drawable.bg_rounded_corner_left)
            phoneMid.setBackgroundResource(R.drawable.bg_rounded_corner_mid)
            phoneEnd.setBackgroundResource(R.drawable.bg_rounded_corner_right)
            errorLabel.invisible()
        }
    }

    fun setphone(phone: String?) {
        phone?.let {
            binding.phoneStart.setText(
                phone.substring(
                    0,
                    3.coerceAtMost(phone.length)
                )
            )
            binding.phoneMid.setText(
                phone.substring(
                    3.coerceAtMost(phone.length),
                    6.coerceAtMost(phone.length)
                )
            )
            binding.phoneEnd.setText(
                phone.substring(
                    6.coerceAtMost(phone.length),
                    phone.length
                )
            )
        } ?: run {
            binding.phoneStart.setText("")
            binding.phoneMid.setText("")
            binding.phoneEnd.setText("")
        }
    }

    private fun addListeners() {
        binding.phoneStart.onFocusChangeListener = onEditTextFocusChangeListener
        binding.phoneMid.onFocusChangeListener = onEditTextFocusChangeListener
        binding.phoneEnd.onFocusChangeListener = onEditTextFocusChangeListener
        addTextChangedListener()
    }
}
