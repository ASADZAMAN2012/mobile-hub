/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view.card

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

internal class CardNumberEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
    private var cardBrand: CardBrand = CardBrand.Unknown

    internal var errorCallback: (showError: Boolean) -> Unit = {}

    private val lengthMax: Int
        get() {
            return cardBrand.maxLengthWithSpaces
        }

    internal val cardNumber: String?
        get() = if (isCardNumberValid) {
            CardUtils.removeSpacesAndHyphens(text?.toString().orEmpty())
        } else {
            null
        }

    internal var completionCallback: () -> Unit = {}
    internal var isCardNumberValid: Boolean = false
    private var ignoreTextChanges = false

    init {
        maxLines = 1
        inputType = InputType.TYPE_CLASS_NUMBER

        listenForTextChanges()
    }

    internal val fieldText: String
        get() {
            return text?.toString().orEmpty()
        }

    internal fun updateSelectionIndex(
        newLength: Int,
        editActionStart: Int,
        editActionAddition: Int
    ): Int {
        var gapsJumped = 0
        val gapSet = cardBrand.spacePositions

        var skipBack = false

        gapSet.forEach { gap ->
            if (editActionStart <= gap && editActionStart + editActionAddition > gap) {
                gapsJumped++
            }

            if (editActionAddition == 0 && editActionStart == gap + 1) {
                skipBack = true
            }
        }

        var newPosition: Int = editActionStart + editActionAddition + gapsJumped
        if (skipBack && newPosition > 0) {
            newPosition--
        }

        return if (newPosition <= newLength) {
            newPosition
        } else {
            newLength
        }
    }

    private fun listenForTextChanges() {
        addTextChangedListener(object : TextWatcher {
            private var latestChangeStart: Int = 0
            private var latestInsertionSize: Int = 0

            private var cursorPosition: Int? = null
            private var formattedNumber: String? = null

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                if (!ignoreTextChanges) {
                    latestChangeStart = start
                    latestInsertionSize = after
                }
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (ignoreTextChanges) {
                    return
                }

                val inputText = s?.toString().orEmpty()
                if (start < 4) {
                    updateCardBrandFromNumber(inputText)
                }

                if (start > 16) {
                    return
                }

                val spaceLessNumber = CardUtils.removeSpacesAndHyphens(inputText) ?: return
                val formattedNumber =
                    createFormattedNumber(
                        CardUtils.separateCardNumberGroups(
                            spaceLessNumber,
                            cardBrand
                        )
                    )

                this.cursorPosition = updateSelectionIndex(
                    newLength = formattedNumber.length,
                    editActionStart = latestChangeStart,
                    editActionAddition = latestInsertionSize
                )
                this.formattedNumber = formattedNumber
            }

            override fun afterTextChanged(s: Editable?) {
                if (ignoreTextChanges) {
                    return
                }

                ignoreTextChanges = true
                if (formattedNumber != null) {
                    setText(formattedNumber)
                    cursorPosition?.let {
                        setSelection(it.coerceIn(0, fieldText.length))
                    }
                }
                formattedNumber = null
                cursorPosition = null

                ignoreTextChanges = false

                if (fieldText.length == lengthMax) {
                    val wasCardNumberValid = isCardNumberValid
                    isCardNumberValid = CardUtils.isValidCardNumber(fieldText)
                    errorCallback.invoke(!isCardNumberValid)
                    if (!wasCardNumberValid && isCardNumberValid) {
                        completionCallback()
                    }
                } else {
                    isCardNumberValid = CardUtils.isValidCardNumber(fieldText)
                    errorCallback.invoke(false)
                }
            }
        })
    }

    private fun updateLengthFilter() {
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(lengthMax))
    }

    private fun updateCardBrand(brand: CardBrand) {
        if (cardBrand == brand) {
            return
        }

        val oldLength = lengthMax

        cardBrand = brand
        if (oldLength != lengthMax) {
            updateLengthFilter()
        }
    }

    internal fun updateCardBrandFromNumber(partialNumber: String) {
        updateCardBrand(CardUtils.getPossibleCardBrand(partialNumber))
    }

    internal companion object {
        internal fun createFormattedNumber(cardParts: Array<String?>): String {
            return cardParts
                .takeWhile { it != null }
                .joinToString(" ")
        }
    }
}
