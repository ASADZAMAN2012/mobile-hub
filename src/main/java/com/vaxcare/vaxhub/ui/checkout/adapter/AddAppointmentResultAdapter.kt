/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvAppointmentSearchListItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.SearchPatient
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentSearchPatientViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentSearchResultViewHolder

class AddAppointmentResultAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    sealed class ResultWrapper {
        data class PatientResult(val patient: SearchPatient) : ResultWrapper()

        data class AppointmentResult(val appointment: Appointment) : ResultWrapper()
    }

    var keyword: String = ""
    var onAddAppointment: ((patient: SearchPatient) -> Unit)? = null
    var onAppointmentSelected: ((appointment: Appointment) -> Unit)? = null
    var resultsFetched = false
    private var publicFlag = false

    private val results: MutableList<ResultWrapper> = mutableListOf()

    override fun getItemCount() = results.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            AppointmentSearchResultViewHolder(
                RvAppointmentSearchListItemBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
        } else {
            AppointmentSearchPatientViewHolder(
                RvAppointmentSearchListItemBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AppointmentSearchResultViewHolder -> {
                val appointmentResult = results[position] as ResultWrapper.AppointmentResult
                holder.bind(appointmentResult.appointment, publicFlag, onAppointmentSelected)
            }
            is AppointmentSearchPatientViewHolder -> {
                val patientResult = results[position] as ResultWrapper.PatientResult
                holder.bind(patientResult.patient, onAddAppointment)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (results[position]) {
            is ResultWrapper.PatientResult -> 1
            else -> 0
        }

    fun addAllItems(items: List<ResultWrapper>, isPublicFlag: Boolean) {
        publicFlag = isPublicFlag
        results.clear()
        results.addAll(items)
        notifyDataSetChanged()
    }
}
