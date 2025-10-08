/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto

abstract class BaseVaccineSummaryItemViewHolder<T : ViewBinding>(
    protected val binding: T,
    protected val listener: SummaryItemListener? = null
) : RecyclerView.ViewHolder(binding.root) {
    protected var appointmentIsEditable = true

    fun bind(item: VaccineAdapterProductDto) {
        setupUI(item)
    }

    abstract fun setupUI(item: VaccineAdapterProductDto)

    protected fun displayAgeIndication(context: Context, item: VaccineAdapterProductDto): String {
        return context.formatString(
            R.string.age_indication_display,
            when (item.ageIndicated) {
                false -> "Yes"
                true -> "No"
            }
        )
    }
}

/**
 * Interface for edit button clicks
 */
interface SummaryItemListener {
    fun onEditAdministeredByClick()

    fun onEditPhysicianClick()
}
