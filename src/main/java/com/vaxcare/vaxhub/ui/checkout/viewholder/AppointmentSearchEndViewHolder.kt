/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.RvAppointmentSearchEndItemBinding

class AppointmentSearchEndViewHolder(val binding: RvAppointmentSearchEndItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(onClick: (() -> Unit)? = null) {
        onClick?.let { click ->
            binding.lotRowText.text =
                itemView.context.getString(R.string.appointment_search_not_found_add)
            binding.lotRowLink.text =
                itemView.context.getString(R.string.appointment_search_add_new)
            binding.lotRowLink.show()
            binding.lotRowLink.setOnClickListener { click.invoke() }
        }
    }
}
