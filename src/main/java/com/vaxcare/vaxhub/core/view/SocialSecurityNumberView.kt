/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.databinding.ViewSocialSecurityNumberBinding

class SocialSecurityNumberView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val binding: ViewSocialSecurityNumberBinding =
        ViewSocialSecurityNumberBinding.inflate(context.getLayoutInflater(), this, true)

    var onSsnChanged: (ssn: String) -> Unit = {}

    val ssn: String
        get() {
            return binding.ssnStart.text.toString() + binding.ssnMid.text.toString() + binding.ssnEnd.text.toString()
        }

    private val onEditTextFocusChangeListener = OnFocusChangeListener { view, b ->
        if (b) {
            (view as EditText).setSelection(view.text.length)
        }
    }

    init {
        addListeners()
    }

    fun addTextChangedListener() {
        binding.ssnStart.doAfterTextChanged {
            if (binding.ssnStart.text.length == 3) {
                binding.ssnMid.requestFocus()
            }
            onSsnChanged(ssn)
        }

        binding.ssnMid.doAfterTextChanged {
            if (binding.ssnMid.text.length == 2) {
                binding.ssnEnd.requestFocus()
            } else if (binding.ssnMid.text.isEmpty()) {
                binding.ssnStart.requestFocus()
            }
            onSsnChanged(ssn)
        }

        binding.ssnEnd.doAfterTextChanged {
            if (binding.ssnEnd.text.isEmpty()) {
                if (binding.ssnMid.text.isEmpty()) {
                    binding.ssnStart.requestFocus()
                } else {
                    binding.ssnMid.requestFocus()
                }
            }
            onSsnChanged(ssn)
        }
    }

    fun isValid(): Boolean {
        return binding.ssnStart.text.isNotBlank() &&
            binding.ssnMid.text.isNotBlank() &&
            binding.ssnEnd.text.isNotBlank() &&
            ssn.length == 9
    }

    private fun addListeners() {
        binding.ssnStart.onFocusChangeListener = onEditTextFocusChangeListener
        binding.ssnMid.onFocusChangeListener = onEditTextFocusChangeListener
        binding.ssnEnd.onFocusChangeListener = onEditTextFocusChangeListener
    }
}
