/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.RoundedTextInputBinding

class RoundedTextInputView(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs) {
    private val binding: RoundedTextInputBinding by lazy {
        RoundedTextInputBinding.inflate(
            context.getLayoutInflater(),
            this,
            true
        )
    }

    var onTextChanged: (text: String) -> Unit = { hideInvalidState() }

    val inputValue: String
        get() = binding.inputValue.text.toString()

    val labelField: TextView
        get() = binding.label

    val inputField: EditText
        get() = binding.inputValue

    val errorLabel: TextView
        get() = binding.errorLabel

    val imageViewClearButton: ImageView
        get() = binding.imageViewClearButton

    fun setValue(text: String?) {
        binding.inputValue.setText(text)
    }

    fun validate(): Boolean {
        if (binding.inputValue.text.isNotBlank()) {
            return true
        } else {
            showInvalidState()
            return false
        }
    }

    private fun showInvalidState() {
        binding.inputFrame.setBackgroundResource(R.drawable.bg_input_error)
        binding.errorLabel.show()
    }

    private fun hideInvalidState() {
        binding.inputFrame.setBackgroundResource(R.drawable.bg_input)
        binding.errorLabel.invisible()
    }

    init {
        val styleable = context.obtainStyledAttributes(attrs, R.styleable.RoundedTextInputView)
        try {
            val label = styleable.getString(R.styleable.RoundedTextInputView_label)
            val labelStyle =
                styleable.getResourceId(R.styleable.RoundedTextInputView_label_style, 0)
            val inputType = styleable.getInt(R.styleable.RoundedTextInputView_android_inputType, EditorInfo.TYPE_NULL)
            val imeOptions = styleable.getInt(R.styleable.RoundedTextInputView_android_imeOptions, EditorInfo.TYPE_NULL)
            val required = styleable.getBoolean(R.styleable.RoundedTextInputView_required, false)
            val subtext = styleable.getString(R.styleable.RoundedTextInputView_subtext)
            val errorLabel = styleable.getString(R.styleable.RoundedTextInputView_error_label)

            binding.label.apply {
                text = label
                if (labelStyle != 0) {
                    setTextAppearance(labelStyle)
                }
            }
            binding.inputValue.apply {
                if (inputType != EditorInfo.TYPE_NULL) {
                    this.inputType = inputType
                }
                if (imeOptions != EditorInfo.TYPE_NULL) {
                    this.imeOptions = imeOptions
                }
                doAfterTextChanged { onTextChanged(inputValue) }
            }
            subtext?.let {
                binding.subtext.text = it
                binding.subtext.visibility = VISIBLE
            }
            errorLabel?.let {
                binding.errorLabel.text = errorLabel
            }

            if (required) {
                binding.required.visibility = View.VISIBLE
            } else {
                binding.required.visibility = View.INVISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            styleable.recycle()
        }
    }
}
