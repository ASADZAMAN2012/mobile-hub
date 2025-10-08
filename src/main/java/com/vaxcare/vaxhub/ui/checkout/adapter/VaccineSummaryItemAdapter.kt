/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.formatAmount
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineSummaryBottomBinding
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineSummaryHeaderBinding
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineSummaryItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.extension.amount
import com.vaxcare.vaxhub.ui.checkout.viewholder.SummaryItemListener
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineSummaryBottomViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineSummaryHeaderViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineSummaryItemViewHolder
import java.time.LocalDate

class VaccineSummaryItemAdapter(
    var items: List<VaccineAdapterProductDto>,
    private val options: VaccineSummaryItemAdapterOptions,
    private val listener: SummaryItemListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val HEADER = 1
    }

    private var shotAdministratorName: String? = null
    private var provider: Provider? = null

    private val hasBottom = if (displayBottomSubTotal()) 1 else 0

    fun setAllItems(items: List<VaccineAdapterProductDto>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size + HEADER + hasBottom

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewHolderType.values()[viewType]) {
            ViewHolderType.HEADER -> {
                VaccineSummaryHeaderViewHolder(
                    binding = RvCheckoutVaccineSummaryHeaderBinding.inflate(
                        parent.getInflater(),
                        parent,
                        false
                    ),
                    appointment = options.appointment,
                    isEditable = options.isEditable,
                    listener = listener,
                    options = options,
                    isCheckedOut = options.isCheckedOut
                )
            }

            ViewHolderType.BOTTOM -> {
                VaccineSummaryBottomViewHolder(
                    RvCheckoutVaccineSummaryBottomBinding.inflate(
                        parent.getInflater(),
                        parent,
                        false
                    )
                )
            }

            else -> {
                VaccineSummaryItemViewHolder(
                    binding = RvCheckoutVaccineSummaryItemBinding.inflate(
                        parent.getInflater(),
                        parent,
                        false
                    )
                )
            }
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
            is VaccineSummaryHeaderViewHolder -> {
                holder.bind(provider, shotAdministratorName)
            }

            is VaccineSummaryItemViewHolder -> {
                if (position > 0) {
                    val product = items[position - 1]
                    holder.bind(product)
                }
            }

            is VaccineSummaryBottomViewHolder -> {
                holder.bind(items)
            }
        }
    }

    fun updateShotAdministratorName(shotAdministratorName: String?) {
        this.shotAdministratorName = shotAdministratorName
        notifyItemChanged(0)
    }

    fun updatePhysician(provider: Provider) {
        this.provider = provider
        notifyItemChanged(0)
    }

    /**
     * Returns the real index of the item inside the adapter
     *
     * @param itemIndex - index of item from list
     * @return - actual positions (item index with offset of HEADER)
     */
    private fun itemPosition(itemIndex: Int) = HEADER + itemIndex

    private fun isHeaderPosition(position: Int): Boolean = position == 0

    private fun isBottomPosition(position: Int): Boolean = displayBottomSubTotal() && position == itemCount - 1

    private fun displayBottomSubTotal(): Boolean =
        hasAddedDoses() && options.isEditable && items.any { it.shouldShowAmount() }

    private fun VaccineAdapterProductDto.shouldShowAmount() =
        isSelfPayDose() || paymentModeReason == PaymentModeReason.SelfPayOptOut || (hasCopay(true))

    fun shouldDisplayCollectPaymentInfo(ignoreCopaysTotal: Boolean = false): Boolean {
        val removedStates = listOf(
            DoseState.ADMINISTERED_REMOVED,
            DoseState.REMOVED
        )

        return items
            .filter { it.doseState !in removedStates }
            .any {
                it.isSelfPayAndNonZeroRate() || it.hasCopaysAndNotMissingPartnerBill(
                    ignoreCopaysTotal
                )
            }
    }

    fun checkSubTotalNotZero(): Boolean = items.amount().formatAmount(2) > 0

    fun hasAddedDoses(): Boolean {
        val removedStates = listOf(
            DoseState.ADMINISTERED_REMOVED,
            DoseState.REMOVED
        )

        return items.any { it.doseState !in removedStates }
    }

    fun existCopay(): Boolean {
        return items.any { it.hasCopay() }
    }

    fun existSelfPay(): Boolean {
        return items.any { it.paymentMode == PaymentMode.SelfPay }
    }

    enum class ViewHolderType {
        HEADER,
        ITEM,
        BOTTOM
    }
}

data class VaccineSummaryItemAdapterOptions(
    val appointment: Appointment? = null,
    val isEditable: Boolean = false,
    val manualDob: LocalDate? = null,
    val updatedFirstName: String? = null,
    val updatedLastName: String? = null,
    val isCheckedOut: Boolean = false
)
