/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvDoseReasonBinding
import com.vaxcare.vaxhub.databinding.RvDoseReasonHeaderBinding
import com.vaxcare.vaxhub.model.inventory.DoseReasonContext
import com.vaxcare.vaxhub.model.inventory.OrderDose
import com.vaxcare.vaxhub.ui.checkout.viewholder.DoseReasonHeaderViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.DoseReasonViewHolder

class DoseReasonAdapter(
    val items: List<OrderDose>,
    val listener: DoseReasonListener
) : RecyclerView.Adapter<BaseDoseReasonViewHolder>() {
    private var mode: DoseReasonContext = DoseReasonContext.ORDER_UNFILLED

    fun newSetup(newMode: DoseReasonContext): DoseReasonAdapter {
        mode = newMode
        notifyItemsChanged()
        return this
    }

    private fun getDisplayItems() = items.filter { it.reasonContext == mode }

    override fun getItemViewType(position: Int): Int =
        when (position) {
            0 -> 0
            else -> 1
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseDoseReasonViewHolder =
        when (viewType) {
            0 -> DoseReasonHeaderViewHolder(
                RvDoseReasonHeaderBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
            else -> DoseReasonViewHolder(
                RvDoseReasonBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
        }

    override fun onBindViewHolder(holder: BaseDoseReasonViewHolder, position: Int) {
        when (holder) {
            is DoseReasonHeaderViewHolder -> {
                val titleId = when (mode) {
                    DoseReasonContext.DOSES_NOT_ORDERED -> R.string.orders_review_header_title_unordered
                    DoseReasonContext.ORDER_UNFILLED -> R.string.orders_review_header_title_not_admin
                    else -> R.string.orders_reason
                }

                holder.bind(
                    titleId = titleId,
                    hideSubtitle = mode == DoseReasonContext.ORDER_UNFILLED
                )
            }
            is DoseReasonViewHolder -> {
                val item = getDisplayItems()[position - 1]
                holder.bind(item, listener)
            }
        }
    }

    override fun getItemCount(): Int = getDisplayItems().size + 1

    // notifyItemChanged did not work correctly. Maybe we need to upgrade the recyclerview lib?
    fun notifyItemsChanged() {
        notifyDataSetChanged()
        listener.onRefresh(getDisplayItems().all { it.selectedReason != null })
    }
}
