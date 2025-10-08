/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.getSuffix
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.DoseSeriesViewholderBinding
import com.vaxcare.vaxhub.ui.checkout.adapter.DoseSeriesClickListener

class DoseSeriesViewHolder(
    val binding: DoseSeriesViewholderBinding,
    val listener: DoseSeriesClickListener
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(value: Int, selected: Boolean) {
        binding.patientDoseSeriesText.apply {
            typeface = if (selected) {
                binding.doseSeriesCheck.show()
                ResourcesCompat.getFont(context, R.font.graphik_bold)
            } else {
                binding.doseSeriesCheck.invisible()
                ResourcesCompat.getFont(context, R.font.graphik_regular)
            }

            val prefix = "${value}${value.getSuffix()}"
            text = context.formatString(R.string.dose_series_fmt, prefix)
            setOnClickListener { listener.onItemClicked(value) }
        }
    }
}
