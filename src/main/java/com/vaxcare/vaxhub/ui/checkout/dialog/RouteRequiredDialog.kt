/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize.LEGACY_WRAP
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogRouteRequiredBinding
import com.vaxcare.vaxhub.model.enums.RouteCode

class RouteRequiredDialog : BaseDialog<DialogRouteRequiredBinding>() {
    val args: RouteRequiredDialogArgs by navArgs()
    private val routeRequiredViewModel: RouteRequiredViewModel by activityViewModels()

    override val baseDialogProperties = DialogProperties(
        dialogSize = LEGACY_WRAP
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            @Deprecated("Deprecated in Java")
            override fun onBackPressed() {
                // Do nothing
            }
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.let {
            it.buttonSubcutaneous.setOnClickListener {
                doOnRouteSelected(RouteCode.SC)
            }
            it.buttonIntramuscular.setOnClickListener {
                doOnRouteSelected(RouteCode.IM)
            }
            it.textViewVaccine.text = args.productName.formatVaccineName()
        }
    }

    private fun doOnRouteSelected(routeCode: RouteCode) {
        setFragmentResultAndDismiss(routeCode)
        routeRequiredViewModel.saveRouteCodeSelectionMetric(
            appointmentId = args.appointmentId,
            lotNumber = args.lotNumber,
            routeSelectionName = routeCode.name
        )
    }

    private fun String.formatVaccineName(): SpannableString {
        val indexOfParenthesis = this.indexOf("(")

        return if (indexOfParenthesis != -1) {
            val spannableString = SpannableString(this)
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                indexOfParenthesis,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString
        } else {
            SpannableString(this)
        }
    }

    private fun setFragmentResultAndDismiss(selectedRouteCode: RouteCode) {
        setFragmentResult(
            ROUTE_REQUIRED_DIALOG_FRAGMENT_RESULT_KEY,
            bundleOf(
                PRODUCT_ID_BUNDLE_KEY to args.productId,
                ROUTE_SELECTED_BUNDLE_KEY to selectedRouteCode.ordinal
            )
        )
        dismiss()
    }

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogRouteRequiredBinding =
        DialogRouteRequiredBinding.inflate(inflater, container, true)

    companion object {
        const val ROUTE_REQUIRED_DIALOG_FRAGMENT_RESULT_KEY = "routeRequiredDialogFragmentResultKey"
        const val PRODUCT_ID_BUNDLE_KEY = "productIdBundleKey"
        const val ROUTE_SELECTED_BUNDLE_KEY = "routeSelectedNameBundleKey"
    }
}
