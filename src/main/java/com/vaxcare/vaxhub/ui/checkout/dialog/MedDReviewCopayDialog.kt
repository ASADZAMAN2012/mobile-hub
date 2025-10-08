/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogMedDCheckBinding
import com.vaxcare.vaxhub.model.metric.MedDCheckRunMetric
import com.vaxcare.vaxhub.ui.checkout.MedDCheckFragment
import com.vaxcare.vaxhub.ui.navigation.CheckoutPatientDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Used to check MedD review copay required
 *
 */
@AndroidEntryPoint
class MedDReviewCopayDialog : BaseDialog<DialogMedDCheckBinding>() {
    companion object {
        const val REQUEST_KEY = "medDReviewCopayDialogRequestKey"
        const val OPTION_SELECTED_BUNDLE_KEY = "optionSelectedBundleKey"
    }

    enum class Option {
        RUN_COPAY_CHECK,
        REMOVE_DOSE
    }

    private val args: MedDReviewCopayDialogArgs by navArgs()
    private val medDReviewCopayRemoveButton = "REMOVE"

    @Inject
    lateinit var destination: CheckoutPatientDestination

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(dialogSize = DialogSize.LEGACY_WRAP)

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogMedDCheckBinding =
        DialogMedDCheckBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonRun?.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.RUN_COPAY_CHECK)
            )

            dismiss()

            destination.goToCopayCheckFromMedDReview(
                medDReviewCopayDialog = this@MedDReviewCopayDialog,
                appointmentId = args.appointmentId,
                duringCheckout = MedDCheckRunMetric.CheckContext.DURING_CHECKOUT,
                copayAntigen = args.copayAntigen,
                isCheckMbi = args.isCheckMbi,
                isMedDCheckStartedAlready = args.isMedDCheckStartedAlready
            )
        }

        binding?.buttonRemove?.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.REMOVE_DOSE)
            )

            globalDestinations.goBack(
                this@MedDReviewCopayDialog,
                mapOf(MedDCheckFragment.MEDD_COPAY_CHECK_FRAGMENT_RESULT_KEY to medDReviewCopayRemoveButton)
            )
        }
    }
}
