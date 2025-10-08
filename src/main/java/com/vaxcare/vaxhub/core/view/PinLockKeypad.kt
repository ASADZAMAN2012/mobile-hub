/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.text.InputFilter
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.view.SoundEffectConstants
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.removeLast
import com.vaxcare.vaxhub.core.extension.setEmpty
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.ViewPinLockKeypadBinding

@SuppressLint("ClickableViewAccessibility")
class PinLockKeypad(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    companion object {
        private const val DEFAULT_MIN_PIN_LENGTH = 4
        private const val DEFAULT_MAX_PIN_LENGTH = 4
        private const val DEFAULT_FLASH_ANIMATION_ENABLED = false
    }

    var onCompletion: (String) -> Unit = {}

    var onBack: () -> Unit = {}

    var onEnterKeyTap: (String) -> Unit = {}

    var onInputTextChanged: (String) -> Unit = {}

    private var minPinLength: Int = DEFAULT_MIN_PIN_LENGTH
    private var maxPinLength: Int = DEFAULT_MAX_PIN_LENGTH
    private var isFlashingAnimationEnabled: Boolean = DEFAULT_FLASH_ANIMATION_ENABLED

    private val flashAnimation: Animation =
        AnimationUtils.loadAnimation(context, R.anim.flash_animation)

    private val binding: ViewPinLockKeypadBinding =
        ViewPinLockKeypadBinding.inflate(context.getLayoutInflater(), this, true)

    init {
        val styleable = context.obtainStyledAttributes(attrs, R.styleable.PinLockKeypad)
        try {
            val inputLengthMax = styleable.getInt(R.styleable.PinLockKeypad_input_length_max, DEFAULT_MAX_PIN_LENGTH)
            val showEnterKey =
                styleable.getBoolean(R.styleable.PinLockKeypad_show_enter_button, false)
            val backTint = styleable.getColor(
                R.styleable.PinLockKeypad_back_button_tint,
                ContextCompat.getColor(context, R.color.primary_coral)
            )

            minPinLength = styleable.getInt(R.styleable.PinLockKeypad_input_length_min, DEFAULT_MIN_PIN_LENGTH)
            maxPinLength = inputLengthMax
            isFlashingAnimationEnabled =
                styleable.getBoolean(R.styleable.PinLockKeypad_flash_animation_enabled, false)
            binding.textViewHiddenPin.filters += InputFilter.LengthFilter(inputLengthMax)
            binding.buttonEnter.isVisible = showEnterKey
            binding.enterButtonLine.isVisible = showEnterKey
            binding.buttonDelete.imageTintList = ColorStateList.valueOf(backTint)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            styleable.recycle()
        }

        binding.buttonOne.setOnClickListener { appendNumberToPin(1) }
        binding.buttonTwo.setOnClickListener { appendNumberToPin(2) }
        binding.buttonThree.setOnClickListener { appendNumberToPin(3) }
        binding.buttonFour.setOnClickListener { appendNumberToPin(4) }
        binding.buttonFive.setOnClickListener { appendNumberToPin(5) }
        binding.buttonSix.setOnClickListener { appendNumberToPin(6) }
        binding.buttonSeven.setOnClickListener { appendNumberToPin(7) }
        binding.buttonEight.setOnClickListener { appendNumberToPin(8) }
        binding.buttonNine.setOnClickListener { appendNumberToPin(9) }
        binding.buttonZero.setOnClickListener { appendNumberToPin(0) }

        binding.buttonDelete.setOnClickListener {
            if (isEnabled) {
                hideError()
                if (binding.textViewHiddenPin.text.isEmpty()) {
                    onBack()
                    return@setOnClickListener
                }
                binding.textViewHiddenPin.removeLast()
                if (isFlashingAnimationEnabled) {
                    binding.textViewHiddenPin.startAnimation(flashAnimation)
                }
            }
        }

        binding.textViewHiddenPin.addTextChangedListener {
            onInputTextChanged(binding.textViewHiddenPin.text.toString())
            if (it?.length == minPinLength) {
                onCompletion(binding.textViewHiddenPin.text.toString())
            }
        }

        binding.textViewHiddenPin.transformationMethod = object : TransformationMethod {
            override fun onFocusChanged(
                view: View?,
                sourceText: CharSequence?,
                focused: Boolean,
                direction: Int,
                previouslyFocusedRect: Rect?
            ) {
            }

            override fun getTransformation(source: CharSequence, view: View?): CharSequence {
                return HiddenPinCharSequence(source)
            }
        }

        binding.buttonEnter.setOnClickListener {
            if (isEnabled) {
                onEnterKeyTap(binding.textViewHiddenPin.text.toString())
            }
        }
    }

    fun showError() {
        binding.errorLabel.show()
        binding.textViewHiddenPin.setEmpty()
    }

    fun hideError() {
        binding.errorLabel.hide()
        binding.syncLabel.invisible()
    }

    fun showUserSync() {
        binding.syncLabel.show()
    }

    fun hideUserSync() {
        binding.syncLabel.invisible()
    }

    fun clearInput() {
        binding.textViewHiddenPin.setEmpty()
    }

    fun enableEnterKey() {
        binding.buttonEnter.isEnabled = true
        binding.buttonEnter.imageTintList = null
    }

    fun disableEnterKey() {
        binding.buttonEnter.isEnabled = false
        binding.buttonEnter.imageTintList =
            ColorStateList.valueOf(context.getColor(R.color.primary_dark_gray))
    }

    private fun appendNumberToPin(num: Int) {
        if (!isEnabled || binding.textViewHiddenPin.text.length >= maxPinLength) {
            return
        }
        hideError()
        val currentPin = binding.textViewHiddenPin.text.toString()
        binding.textViewHiddenPin.text = StringBuilder(currentPin).append(num.toString()).toString()
        if (isFlashingAnimationEnabled) {
            binding.textViewHiddenPin.startAnimation(flashAnimation)
        }
        binding.textViewHiddenPin.playSoundEffect(SoundEffectConstants.CLICK)
    }

    /**
     * Convert Char sequence to * sequence
     */
    private class HiddenPinCharSequence(private val source: CharSequence) : CharSequence {
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
