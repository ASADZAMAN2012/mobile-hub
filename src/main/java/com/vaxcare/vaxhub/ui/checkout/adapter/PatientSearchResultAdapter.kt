/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvPatientSearchEndItemBinding
import com.vaxcare.vaxhub.databinding.RvPatientSearchResultItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.SearchPatient
import com.vaxcare.vaxhub.ui.checkout.viewholder.PatientSearchEndViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.PatientSearchResultViewHolder

class PatientSearchResultAdapter(
    val type: AddPatientType
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class PatientSearchWrapper(
        val patient: SearchPatient,
        // Today's appointments
        val appointment: Appointment? = null
    )

    enum class AddPatientType {
        ADD,
        SEARCH
    }

    var keyword: String = ""

    var onAddAppointment: ((patient: SearchPatient) -> Unit)? = null
    var onStartCheckout: ((appointment: Appointment) -> Unit)? = null

    var addNewPatient: (() -> Unit)? = null
    var resultsFetched = false

    private val patientWrappers: MutableList<PatientSearchWrapper> = mutableListOf()

    override fun getItemCount() = patientWrappers.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            PatientSearchResultViewHolder(
                type = type,
                binding = RvPatientSearchResultItemBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
        } else {
            PatientSearchEndViewHolder(
                RvPatientSearchEndItemBinding.inflate(parent.getInflater(), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PatientSearchResultViewHolder) {
            if (position < patientWrappers.size) {
                val appointment = patientWrappers[position]
                holder.bind(keyword, appointment, onStartCheckout)
            }
        } else if (holder is PatientSearchEndViewHolder) {
            holder.bind(addNewPatient, type == AddPatientType.SEARCH || itemCount > 1)
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (position == patientWrappers.count()) {
            1
        } else {
            0
        }

    fun addAllItems(items: List<PatientSearchWrapper>) {
        patientWrappers.clear()
        patientWrappers.addAll(items.filter { it.appointment != null })
        notifyDataSetChanged()
    }
}
