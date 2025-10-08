/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.SoundEffectConstants
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.removeLast
import com.vaxcare.vaxhub.databinding.ViewNumberKeypadBinding

@SuppressLint("ClickableViewAccessibility", "CustomViewStyleable")
class NumberKeypad(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    var onSuccessListener: ((String) -> Unit)? = null

    private val binding: ViewNumberKeypadBinding =
        ViewNumberKeypadBinding.inflate(context.getLayoutInflater(), this, true)

    init {

        binding.buttonOne.setOnClickListener { appendNumber("1") }
        binding.buttonTwo.setOnClickListener { appendNumber("2") }
        binding.buttonThree.setOnClickListener { appendNumber("3") }
        binding.buttonFour.setOnClickListener { appendNumber("4") }
        binding.buttonFive.setOnClickListener { appendNumber("5") }
        binding.buttonSix.setOnClickListener { appendNumber("6") }
        binding.buttonSeven.setOnClickListener { appendNumber("7") }
        binding.buttonEight.setOnClickListener { appendNumber("8") }
        binding.buttonNine.setOnClickListener { appendNumber("9") }
        binding.buttonZero.setOnClickListener { appendNumber("0") }

        binding.buttonDelete.setOnClickListener {
            binding.input.removeLast()
        }

        binding.buttonEnter.setOnClickListener {
            if (binding.input.text.isEmpty()) {
                return@setOnClickListener
            }
            onSuccessListener?.let { it1 -> it1(binding.input.text.toString()) }
        }

        binding.clearText.setOnClickListener {
            binding.input.text = ""
        }

        binding.input.addTextChangedListener {
            if (it?.toString()?.isNotBlank() == true) {
                binding.clearText.visibility = View.VISIBLE
                binding.buttonDelete.visibility = View.VISIBLE
                binding.buttonEnter.isEnabled = true
            } else {
                binding.clearText.visibility = View.GONE
                binding.buttonDelete.visibility = View.INVISIBLE
                binding.buttonEnter.isEnabled = false
            }
        }
        binding.buttonEnter.isEnabled = false
    }

    @SuppressLint("SetTextI18n")
    private fun appendNumber(num: String) {
        binding.input.text = binding.input.text.toString() + num
        binding.input.playSoundEffect(SoundEffectConstants.CLICK)
    }

    fun setEnterButton(resId: Int) {
        binding.buttonEnter.setImageResource(resId)
    }
}
