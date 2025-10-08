/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.getActivity
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.hideKeyboard
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.ViewDateOfBirthBinding
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth

class DateOfBirthView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    var onPatientInfoChanged: () -> Unit = {}
    private var selectedMonth: String? = null
    private var selectedYear: String? = null
    private var selectedDay: String? = null
    private val formattedMonth: String?
        get() {
            val month = selectedMonth
            return if (month == null) {
                null
            } else {
                context.formatString(
                    R.string.month_format,
                    month,
                    resources.getStringArray(R.array.array_month)[month.toInt() - 1]
                )
            }
        }
    private val binding: ViewDateOfBirthBinding =
        ViewDateOfBirthBinding.inflate(context.getLayoutInflater(), this, true)

    init {
        val styleable = context.obtainStyledAttributes(attrs, R.styleable.DateOfBirthView)
        val label = styleable.getString(R.styleable.DateOfBirthView_label)
        val labelStyle =
            styleable.getResourceId(R.styleable.DateOfBirthView_label_style, 0)
        val required = styleable.getBoolean(R.styleable.DateOfBirthView_required, false)

        binding.dateOfBirthTitle.apply {
            label?.let { text = it }
            if (labelStyle != 0) {
                setTextAppearance(labelStyle)
            }
        }

        if (required) {
            binding.required.visibility = View.VISIBLE
        } else {
            binding.required.visibility = View.INVISIBLE
        }

        val onFocusChangeListener = OnFocusChangeListener { view, b ->
            if (b) {
                (view as EditText).setSelection(view.text.length)
            }
        }
        addListeners()
    }

    fun setFocus() {
        if (selectedMonth == null) {
            binding.dateOfBirthMonth.callOnClick()
        } else if (selectedDay == null) {
            binding.dateOfBirthDay.callOnClick()
        } else {
            binding.dateOfBirthYear.callOnClick()
        }
    }

    fun addTextChangedListener() {
        binding.dateOfBirthMonth.doAfterTextChanged {
            onPatientInfoChanged()
        }
        binding.dateOfBirthDay.doAfterTextChanged {
            onPatientInfoChanged()
        }
        binding.dateOfBirthYear.doAfterTextChanged {
            onPatientInfoChanged()
        }
    }

    fun isValid(): Boolean {
        return binding.dateOfBirthMonth.text.isNotBlank() &&
            binding.dateOfBirthDay.text.isNotBlank() &&
            binding.dateOfBirthYear.text.isNotBlank()
    }

    fun validate(): Boolean {
        if (binding.dateOfBirthMonth.text.isNotBlank() &&
            binding.dateOfBirthDay.text.isNotBlank() &&
            binding.dateOfBirthYear.text.isNotBlank()
        ) {
            return true
        } else {
            showInvalidState()
            return false
        }
    }

    fun isNotEmpty(): Boolean {
        return binding.dateOfBirthMonth.text.isNotEmpty() ||
            binding.dateOfBirthDay.text.isNotEmpty() ||
            binding.dateOfBirthYear.text.isNotEmpty()
    }

    private fun showInvalidState() {
        binding.apply {
            dateOfBirthMonth.setBackgroundResource(R.drawable.bg_rounded_corner_left_error)
            dateOfBirthDay.setBackgroundResource(R.drawable.bg_rounded_corner_mid_error)
            dateOfBirthYear.setBackgroundResource(R.drawable.bg_rounded_corner_right_error)
            errorLabel.show()
        }
    }

    private fun hideInvalidState() {
        binding.apply {
            dateOfBirthMonth.setBackgroundResource(R.drawable.bg_rounded_corner_left)
            dateOfBirthDay.setBackgroundResource(R.drawable.bg_rounded_corner_mid)
            dateOfBirthYear.setBackgroundResource(R.drawable.bg_rounded_corner_right)
            errorLabel.invisible()
        }
    }

    fun setDob(dob: LocalDate?) {
        dob?.let {
            selectedMonth = dob.monthValue.toString()
            selectedDay = dob.dayOfMonth.toString()
            selectedYear = dob.year.toString()
            binding.dateOfBirthMonth.text = formattedMonth
            binding.dateOfBirthDay.text = selectedDay
            binding.dateOfBirthYear.text = selectedYear
            hideInvalidState()
        } ?: run {
            binding.dateOfBirthMonth.text = ""
            binding.dateOfBirthDay.text = ""
            binding.dateOfBirthYear.text = ""
        }
    }

    fun getDob(): LocalDate? {
        val date = try {
            LocalDate.of(
                requireNotNull(selectedYear).toInt(),
                requireNotNull(selectedMonth).toInt(),
                requireNotNull(selectedDay).toInt()
            )
        } catch (e: Exception) {
            Timber.d(e, "Found null date on view")
            null
        }
        return date
    }

    private fun addListeners() {
        binding.dateOfBirthMonth.setOnClickListener {
            hideKeyboard()
            val month = selectedMonth
            val year = selectedYear
            val monthList = if (year != null && year.toInt() == LocalDate.now().year) {
                (1..LocalDate.now().monthValue).map { it.toString() }
            } else {
                (1..12).map { it.toString() }
            }
            val selectedIndex = if (month != null) monthList.indexOf(month) else -1
            val monthSimplyArray = resources.getStringArray(R.array.array_month)
            val bottomDialog = BottomDialog.newInstance(
                context.getString(R.string.patient_select_month),
                monthList.map { monthSimplyArray[it.toInt() - 1] },
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                selectedMonth = monthList[index]
                binding.dateOfBirthMonth.text = formattedMonth
                correctionDate()
                onPatientInfoChanged()
                if (selectedDay == null) {
                    binding.dateOfBirthDay.performClick()
                } else if (selectedYear == null) {
                    binding.dateOfBirthDay.performClick()
                }
                hideInvalidState()
            }

            bottomDialog.show(
                (getActivity() as FragmentActivity).supportFragmentManager,
                "monthBottomDialog"
            )
        }
        binding.dateOfBirthDay.setOnClickListener {
            hideKeyboard()
            val day = selectedDay
            val month = selectedMonth
            val year = selectedYear
            val dayList =
                if (
                    year != null &&
                    year.toInt() == LocalDate.now().year &&
                    month != null &&
                    month.toInt() == LocalDate.now().monthValue
                ) {
                    (1..LocalDate.now().dayOfMonth).map { it.toString() }
                } else {
                    if (year != null && month != null && day != null) {
                        val monthLength =
                            LocalDate.of(
                                year.toInt(),
                                month.toInt(),
                                day.toInt()
                            )
                                .lengthOfMonth()
                        (1..monthLength).map { it.toString() }
                    } else {
                        if (month != null) {
                            val now = LocalDate.now()
                            val monthLength = YearMonth
                                .of(year?.toInt() ?: now.year, month.toInt())
                                .lengthOfMonth()
                            (1..monthLength).map { it.toString() }
                        } else {
                            (1..31).map { it.toString() }
                        }
                    }
                }
            val selectedIndex = if (day != null) dayList.indexOf(day) else -1
            val bottomDialog = BottomDialog.newInstance(
                context.getString(R.string.patient_select_day),
                dayList,
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                selectedDay = dayList[index]
                binding.dateOfBirthDay.text = selectedDay
                onPatientInfoChanged()
                if (selectedYear == null) {
                    binding.dateOfBirthYear.performClick()
                }
                hideInvalidState()
            }

            bottomDialog.show(
                (getActivity() as FragmentActivity).supportFragmentManager,
                "dayBottomDialog"
            )
        }
        binding.dateOfBirthYear.setOnClickListener {
            it.hideKeyboard()
            val year = selectedYear
            val currentYear = LocalDate.now().year
            val yearList = (1900..currentYear).map { it.toString() }.reversed()
            val selectedIndex = if (year != null) yearList.indexOf(year) else -1
            val bottomDialog = BottomDialog.newInstance(
                context.getString(R.string.patient_select_year),
                yearList,
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                selectedYear = yearList[index]
                binding.dateOfBirthYear.text = selectedYear
                correctionDate()
                onPatientInfoChanged()
                hideInvalidState()
            }

            bottomDialog.show(
                (getActivity() as FragmentActivity).supportFragmentManager,
                "yearBottomDialog"
            )
        }
    }

    private fun correctionDate() {
        val day = selectedDay
        val month = selectedMonth
        val year = selectedYear
        if (year != null && year.toInt() == LocalDate.now().year &&
            month != null && month.toInt() > LocalDate.now().monthValue
        ) {
            selectedMonth = LocalDate.now().monthValue.toString()
            binding.dateOfBirthMonth.text = formattedMonth
        }
        if (year != null && year.toInt() == LocalDate.now().year &&
            month != null && month.toInt() == LocalDate.now().monthValue &&
            day != null && day.toInt() > LocalDate.now().dayOfMonth
        ) {
            selectedDay = LocalDate.now().dayOfMonth.toString()
            binding.dateOfBirthDay.text = selectedDay
        }
        // Prevent choosing illegal dates
        if (year != null &&
            month != null &&
            day != null &&
            day.toInt() > YearMonth.of(year.toInt(), month.toInt()).lengthOfMonth()
        ) {
            selectedDay = YearMonth.of(year.toInt(), month.toInt()).lengthOfMonth().toString()
            binding.dateOfBirthDay.text = selectedDay
        }
    }
}
