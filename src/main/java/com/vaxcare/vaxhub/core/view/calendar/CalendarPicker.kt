/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view.calendar

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.databinding.ViewCalendarPickerBinding
import java.time.LocalDate

class CalendarPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var calendarAdapter: CalendarAdapter

    companion object {
        const val RANGE = 90L
        const val COLUMN_SIZE = 7
        const val WEEK_SIZE = 6
    }

    private val defaultDate = LocalDate.now()
    private val minDate = defaultDate.minusDays(RANGE)
    private val maxDate = defaultDate.plusDays(RANGE)
    private val currentMonth = MutableLiveData(defaultDate)

    private val binding: ViewCalendarPickerBinding =
        ViewCalendarPickerBinding.inflate(context.getLayoutInflater(), this, true)

    init {
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.CalendarPicker, 0, 0)
        val singleChoice = attributes.getBoolean(R.styleable.CalendarPicker_singleChoice, true)
        val maxChoices = attributes.getInt(R.styleable.CalendarPicker_maxChoices, 1)
        val allowWeekends = attributes.getBoolean(R.styleable.CalendarPicker_allowWeekends, true)
        attributes.recycle()

        binding?.rvMonth?.layoutManager = GridLayoutManager(context, 7)
        binding?.rvMonth?.addItemDecoration(CalendarItemDecoration(context))
        calendarAdapter =
            CalendarAdapter(context, minDate, maxDate, singleChoice, maxChoices, allowWeekends)
        binding?.rvMonth?.adapter = calendarAdapter

        binding?.navigateBack?.setOnClickListener {
            currentMonth.postValue(currentMonth.value?.minusMonths(1))
        }
        binding?.navigateForward?.setOnClickListener {
            currentMonth.postValue(currentMonth.value?.plusMonths(1))
        }

        val lifecycleOwner = when (context) {
            is LifecycleOwner -> context
            else -> (context as ContextWrapper).baseContext as LifecycleOwner
        }
        currentMonth.observe(lifecycleOwner, {
            binding?.currentMonth?.text = it.toLocalDateString("MMMM yyyy")
            binding?.navigateBack?.isEnabled = it.month != minDate.month || it.year != minDate.year
            binding?.navigateForward?.isEnabled =
                it.month != maxDate.month || it.year != maxDate.year
            calendarAdapter.updateMonth(it)
        })
    }

    fun setOnDateSelectedListener(onDateSelectedListener: OnDateSelectedListener) {
        calendarAdapter.onDateSelectedListener = onDateSelectedListener
    }

    fun setAvailableDays(availableDayOfWeeks: List<Int>, scheduledDays: List<LocalDate>? = null) {
        calendarAdapter.availableDayOfWeeks = availableDayOfWeeks
        calendarAdapter.scheduledDays = scheduledDays
        calendarAdapter.notifyDataSetChanged()
    }

    fun setDatesWithData(datesWithData: Set<LocalDate>) {
        calendarAdapter.datesWithData = datesWithData
    }

    fun setSelectedDates(dates: MutableList<LocalDate>) {
        calendarAdapter.selectedDates = dates
        calendarAdapter.notifyDataSetChanged()
    }

    fun setSelectedDateAndUpdateMonth(date: LocalDate) {
        calendarAdapter.selectedDates = mutableListOf(date)
        currentMonth.postValue(date)
    }

    fun getSelectedDates(): MutableList<LocalDate> {
        return calendarAdapter.selectedDates.sorted().toMutableList()
    }
}

interface OnDateSelectedListener {
    fun onDateSelected(dates: List<LocalDate>)
}
