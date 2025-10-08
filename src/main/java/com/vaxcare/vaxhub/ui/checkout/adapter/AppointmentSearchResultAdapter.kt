/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvAppointmentSearchEndItemBinding
import com.vaxcare.vaxhub.databinding.RvAppointmentSearchListItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentSearchEndViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentSearchResultViewHolder

class AppointmentSearchResultAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var keyword: String = ""
    var onAddAppointment: (() -> Unit)? = null
    var onStartCheckout: ((appointment: Appointment) -> Unit)? = null
    var resultsFetched = false
    private var publicFlag = false

    private val appointments: MutableList<Appointment> = mutableListOf()

    override fun getItemCount() = if (resultsFetched) appointments.size + 1 else appointments.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            return AppointmentSearchResultViewHolder(
                binding = RvAppointmentSearchListItemBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
        } else {
            AppointmentSearchEndViewHolder(
                RvAppointmentSearchEndItemBinding.inflate(parent.getInflater(), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppointmentSearchResultViewHolder) {
            if (position < appointments.size) {
                val appointment = appointments[position]
                holder.bind(appointment, publicFlag, onStartCheckout)
            }
        } else if (holder is AppointmentSearchEndViewHolder) {
            holder.bind(onAddAppointment)
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (position == appointments.count()) {
            1
        } else {
            0
        }

    fun addAllItems(items: List<Appointment>, isPublicFlag: Boolean) {
        publicFlag = isPublicFlag
        appointments.clear()
        appointments.addAll(items)
        notifyDataSetChanged()
    }
}
