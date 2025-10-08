/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view.calendar

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.inflate
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import java.time.LocalDate

class CalendarAdapter(
    val context: Context,
    private val minDate: LocalDate,
    private val maxDate: LocalDate,
    private val singleChoice: Boolean = true,
    private val maxChoices: Int = 1,
    private val allowWeekends: Boolean = true
) :
    RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {
    var selectedDates = mutableListOf<LocalDate>()
    lateinit var month: LocalDate
    var onDateSelectedListener: OnDateSelectedListener? = null
    var datesWithData: Set<LocalDate> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var availableDayOfWeeks: List<Int> = listOf()
    var scheduledDays: List<LocalDate>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.view_calendar_day))
    }

    override fun getItemCount(): Int {
        return CalendarPicker.WEEK_SIZE * CalendarPicker.COLUMN_SIZE
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (!this::month.isInitialized) {
            return
        }

        val offset = position - indexOfFirstDay()
        val date = month.withDayOfMonth(1).plusDays(offset.toLong())
        holder.date.text = date.dayOfMonth.toString()

        holder.itemView.setOnSingleClickListener {
            if (isDateInScope(date).not()) {
                return@setOnSingleClickListener
            }

            if (singleChoice) {
                selectedDates.clear()
                selectedDates.add(date)
            } else {
                if (!selectedDates.contains(date)) {
                    if (selectedDates.size < maxChoices) {
                        selectedDates.add(date)
                    }
                } else {
                    selectedDates.remove(date)
                }
            }
            onDateSelectedListener?.onDateSelected(selectedDates)
            notifyDataSetChanged()
        }

        val isCurrentMonth = month.monthValue == date.monthValue && month.year == date.year
        if (isCurrentMonth) {
            val isSelectableInSingleChoiceMode = singleChoice && datesWithData.contains(date)
            if (isSelectableInSingleChoiceMode) {
                holder.date.setTextAppearance(R.style.BodyBoldBlack)
            } else {
                holder.date.setTextAppearance(R.style.BodyRegBlack)
            }
            holder.date.alpha = 1.0f
        } else {
            holder.date.setTextAppearance(R.style.BodyRegBlack)
            holder.date.alpha = 0.5f
        }

        val find = selectedDates.find { it == date }
        if (find != null) {
            holder.indicator.show()
            holder.date.setTextColor(ContextCompat.getColor(context, R.color.primary_white))
        } else {
            holder.indicator.hide()
            holder.date.setTextColor(ContextCompat.getColor(context, R.color.primary_black))
        }
    }

    private fun indexOfFirstDay(): Int {
        return month.withDayOfMonth(1).dayOfWeek.value.rem(7)
    }

    private fun isDateInScope(date: LocalDate): Boolean {
        return date.atStartOfDay() >= minDate.atStartOfDay() &&
            date.atStartOfDay() <= maxDate.atStartOfDay()
    }

    fun updateMonth(month: LocalDate) {
        this.month = month
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.date)
        val indicator: ImageView = view.findViewById(R.id.indicator)
    }
}
