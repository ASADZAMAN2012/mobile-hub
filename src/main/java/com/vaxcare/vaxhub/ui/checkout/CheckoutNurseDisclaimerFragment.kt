/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.ui.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentCheckoutNurseDisclaimerBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.MedDCheckoutDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutNurseDisclaimerFragment : BaseFragment<FragmentCheckoutNurseDisclaimerBinding>() {
    private val args: CheckoutNurseDisclaimerFragmentArgs by navArgs()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: MedDCheckoutDestination

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_checkout_nurse_disclaimer,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentCheckoutNurseDisclaimerBinding =
        FragmentCheckoutNurseDisclaimerBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        val (titleId, descId) = if (args.preAccepted) {
            R.string.nurse_disclaimer_optin_title to R.string.nurse_disclaimer_optin_desc
        } else {
            R.string.nurse_disclaimer_optout_title to R.string.nurse_disclaimer_optout_desc
        }

        binding?.apply {
            disclaimerTitle.setText(titleId)
            disclaimerDescription.setText(descId)
            buttonOk.setOnSingleClickListener {
                destination.goToPaymentSummaryFromDisclaimer(
                    fragment = this@CheckoutNurseDisclaimerFragment,
                    appointmentId = args.appointmentId,
                    paymentInfo = args.paymentInformation
                )
            }
        }
    }
}
