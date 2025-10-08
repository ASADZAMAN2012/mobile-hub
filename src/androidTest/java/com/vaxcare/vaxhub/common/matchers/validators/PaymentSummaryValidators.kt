package com.vaxcare.vaxhub.common.matchers.validators

import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.utility.ViewUtils.drawableMatches
import com.vaxcare.vaxhub.data.PaymentModals
import com.vaxcare.vaxhub.data.TestCards
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.ui.checkout.viewholder.MedDSummaryBottomViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.MedDSummaryHeaderViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.PaymentSummaryItemViewHolder

class PaymentSummaryHeaderValidator(private val recyclerView: RecyclerView) :
    ItemValidator<TestPatients> {
    override fun validateAtIndex(index: Int, arg: TestPatients): Boolean {
        val holder =
            recyclerView.findViewHolderForAdapterPosition(index) as MedDSummaryHeaderViewHolder
        val patientName = holder.binding.medDPatientName.text.toString()
        return patientName.equals(arg.completePatientName, true)
    }
}

class PaymentSummaryItemValidator(
    private val recyclerView: RecyclerView,
    private val testProducts: List<TestProducts>,
    private val copayValue: String
) : ItemValidator<PaymentModals> {
    override fun validateAtIndex(index: Int, arg: PaymentModals): Boolean {
        val holder =
            recyclerView.findViewHolderForAdapterPosition(index) as PaymentSummaryItemViewHolder
        val vaccineNameTextView =
            holder.itemView.findViewById<TextView>(R.id.med_d_summary_vaccine_name)
        val vaccineLotNumberTextView =
            holder.itemView.findViewById<TextView>(R.id.med_d_summary_vaccine_lot_number)
        val vaccineCopayIssuesTextView =
            holder.itemView.findViewById<TextView>(R.id.med_d_summary_vaccine_copay_title)
        val vaccineCopayValueTextView =
            holder.itemView.findViewById<TextView>(R.id.med_d_summary_vaccine_copay_value)

        val vaccineName = vaccineNameTextView.text.toString()
        val vaccineLotNumber = vaccineLotNumberTextView.text.toString()
        val vaccineCopayIssues = vaccineCopayIssuesTextView.text.toString()
        val vaccineCopayValue = vaccineCopayValueTextView.text.toString()
        val associatedProduct = testProducts.firstOrNull {
            it.antigenProductName.equals(vaccineName, true) &&
                it.lotNumber.equals(vaccineLotNumber, true)
        }
        return associatedProduct != null && vaccineCopayIssues.equals(
            when {
                arg == PaymentModals.PaymentCashOrCheck -> "Partner Bill"
                associatedProduct.hasCopay -> "Copay${arg.summaryDisplay}"
                else -> ""
            },
            true
        ) && (
            arg == PaymentModals.PaymentCashOrCheck || !associatedProduct.hasCopay || vaccineCopayValue.equals(
                "$$copayValue",
                true
            )
        )
    }
}

class PaymentSummaryFooterValidator(
    private val recyclerView: RecyclerView,
    private val copayValue: String,
    private val cardInfo: TestCards? = null
) : ItemValidator<PaymentModals> {
    override fun validateAtIndex(index: Int, arg: PaymentModals): Boolean {
        val holder =
            recyclerView.findViewHolderForAdapterPosition(index) as MedDSummaryBottomViewHolder
        val copayTotal = holder.binding.medDSummaryCopayTotal.text.toString()
        val copayValueSubtotal = holder.binding.medDSummaryCopaySubtotal.text.toString()
        val isCashOrCheckLabelVisible = holder.binding.medDSummaryCashLayout.isVisible
        val copayCardInfo = holder.binding.medDSummaryCopayCardInfo.text.toString()
        val (totalToCollectExpectText, copayCardInfoExpectText) = when (arg) {
            is PaymentModals.PaymentDebitOrCredit -> getDebitOrCreditTotalsText(arg)
            PaymentModals.PaymentCashOrCheck ->
                recyclerView.context.getString(R.string.med_d_checkout_vaccine_copay_total) to
                    recyclerView.context.getString(R.string.med_d_summary_copay_without_card)

            else ->
                recyclerView.context.getString(R.string.med_d_checkout_vaccine_copay_total) to
                    recyclerView.context.getString(R.string.patient_checkout_base_exclusion_continue)
        }

        return copayTotal.equals(
            totalToCollectExpectText,
            true
        ) && isCashOrCheckLabelVisible == (arg == PaymentModals.PaymentCashOrCheck) && copayCardInfo.equals(
            copayCardInfoExpectText,
            true
        ) && (
            arg == PaymentModals.PaymentCashOrCheck || copayValueSubtotal.equals(
                "$$copayValue",
                true
            )
        ) && verifyCardInfo(holder)
    }

    private fun verifyCardInfo(holder: MedDSummaryBottomViewHolder): Boolean =
        cardInfo?.let {
            val lastFourNumbers =
                it.cardNumber.substring(it.cardNumber.length - 4)

            holder.binding.medDSummaryCopaySubtotal.text.isNotEmpty() &&
                holder.binding.medDSummaryCopayCardNumber.text == lastFourNumbers &&
                holder.binding.medDSummaryCopayCardIcon.drawableMatches(it.cardIconResId)
        } ?: true

    private fun getDebitOrCreditTotalsText(arg: PaymentModals.PaymentDebitOrCredit) =
        when {
            arg.cardOnFile -> recyclerView.context.getString(R.string.med_d_checkout_vaccine_copay_total) to ""
            arg.isCopay -> recyclerView.context.getString(R.string.med_d_checkout_vaccine_copay_total) to
                recyclerView.context.getString(R.string.med_d_summary_copay_with_card)

            else -> recyclerView.context.getString(R.string.med_d_checkout_vaccine_copay_total) to
                recyclerView.context.getString(R.string.self_pay_one_touch_checkout_with_card)
        }
}
