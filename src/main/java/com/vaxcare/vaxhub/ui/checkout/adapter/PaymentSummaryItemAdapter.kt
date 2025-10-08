/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvMedDSummaryBottomBinding
import com.vaxcare.vaxhub.databinding.RvMedDSummaryHeaderBinding
import com.vaxcare.vaxhub.databinding.RvMedDSummaryItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.inventory.Site
import com.vaxcare.vaxhub.ui.checkout.viewholder.MedDSummaryBottomViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.MedDSummaryHeaderViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.PaymentSummaryItemViewHolder

class PaymentSummaryItemAdapter(
    private val appointment: Appointment,
    private val items: MutableList<VaccineAdapterProductDto>,
    private var paymentInformation: PaymentInformationRequestBody?,
    private val listener: MedDSummaryBottomViewHolder.OnPaymentMethodModeChangeListener,
    private val options: VaccineSummaryItemAdapterOptions
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val HEADER = 1
        private const val BOTTOM = 1
    }

    fun updatePaymentInformation(paymentInformation: PaymentInformationRequestBody?) {
        this.paymentInformation = paymentInformation
        notifyItemRangeChanged(HEADER, itemCount - HEADER)
    }

    fun updateSite(siteNew: Site.SiteValue, item: VaccineAdapterProductDto) {
        val index = items.indexOf(item)
        if (index != -1) {
            items[index].apply {
                site = siteNew
            }

            notifyItemChanged(itemPosition(index))
        }
    }

    override fun getItemCount() = HEADER + items.size + BOTTOM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewHolderType.values()[viewType]) {
            ViewHolderType.HEADER -> MedDSummaryHeaderViewHolder(
                RvMedDSummaryHeaderBinding.inflate(parent.getInflater(), parent, false),
                appointment.patient,
                options = options
            )
            ViewHolderType.BOTTOM -> MedDSummaryBottomViewHolder(
                RvMedDSummaryBottomBinding.inflate(parent.getInflater(), parent, false),
                listener
            )
            else -> PaymentSummaryItemViewHolder(
                binding = RvMedDSummaryItemBinding.inflate(parent.getInflater(), parent, false),
                appointment = appointment,
                paymentInformation = paymentInformation
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isHeaderPosition(position) -> ViewHolderType.HEADER.ordinal
            isBottomPosition(position) -> ViewHolderType.BOTTOM.ordinal
            else -> ViewHolderType.ITEM.ordinal
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MedDSummaryHeaderViewHolder -> holder.bind()
            is PaymentSummaryItemViewHolder -> holder.bind(items[position - BOTTOM])
            is MedDSummaryBottomViewHolder -> holder.bind(items, paymentInformation)
            else -> Unit
        }
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return position == 0
    }

    private fun isBottomPosition(position: Int): Boolean {
        return position == itemCount - 1
    }

    private fun itemPosition(index: Int) = HEADER + index

    enum class ViewHolderType {
        HEADER,
        ITEM,
        BOTTOM
    }
}
