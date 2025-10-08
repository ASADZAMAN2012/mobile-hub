/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.DoseSeriesViewholderBinding
import com.vaxcare.vaxhub.ui.checkout.viewholder.DoseSeriesViewHolder

/**
 * Interface to handle item clicked
 */
interface DoseSeriesClickListener {
    /**
     * ItemClicked callback
     *
     * @param doseValue - the selected dose series value
     */
    fun onItemClicked(doseValue: Int)
}

class DoseSeriesAdapter(
    val dosesInSeries: Int,
    val doseSeries: Int,
    val listener: DoseSeriesClickListener
) :
    RecyclerView.Adapter<DoseSeriesViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoseSeriesViewHolder =
        DoseSeriesViewHolder(
            DoseSeriesViewholderBinding.inflate(
                parent.getInflater(),
                parent,
                false
            ),
            listener
        )

    override fun onBindViewHolder(holder: DoseSeriesViewHolder, position: Int) {
        val dose = position + 1
        holder.bind(dose, doseSeries == dose)
    }

    override fun getItemCount(): Int = dosesInSeries
}
