/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvAppointmentItemListViewBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentClickListener
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentListItemViewHolder
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

class AppointmentListItemAdapter(
    private val listener: AppointmentClickListener
) : ListAdapter<Appointment, AppointmentListItemViewHolder>(ItemCallback) {
    var flags: AppointmentListFlags = AppointmentListFlags()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentListItemViewHolder {
        return AppointmentListItemViewHolder(
            RvAppointmentItemListViewBinding.inflate(
                parent.getInflater(),
                parent,
                false
            ),
            listener
        )
    }

    override fun onBindViewHolder(holder: AppointmentListItemViewHolder, position: Int) {
        holder.bind(getItem(position), flags)
    }

    fun getCurrentDatePosition(): Int {
        val now = LocalDateTime.now()
        val date = LocalDateTime.of(now.year, now.month, now.dayOfMonth, now.hour, 0)
        val index = currentList.indexOfFirst { it.appointmentTime >= date }
        return if (index >= 0) index else 0
    }

    /**
     * Find the closest appointment to current time minus one hour
     *
     * @param timeOfSelectedDay the time to compare
     * @return the index of the closest appointment
     */
    fun findClosestAppointment(timeOfSelectedDay: LocalDateTime): Int {
        // Find the closest appointment to current time minus one hour
        val theClosestAppointment = currentList.minByOrNull {
            val duration = Duration.between(it.appointmentTime, timeOfSelectedDay.minusHours(1))
            abs(duration.seconds)
        }

        return currentList.indexOf(theClosestAppointment)
    }
}

data class AppointmentListFlags(
    val isRprd: Boolean = false,
    val isPublicStock: Boolean = false
)

private object ItemCallback : DiffUtil.ItemCallback<Appointment>() {
    override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
        return oldItem == newItem
    }
}
