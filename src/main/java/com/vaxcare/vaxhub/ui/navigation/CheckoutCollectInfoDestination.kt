/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.model.patient.InvalidInfoWrapper
import com.vaxcare.vaxhub.ui.patient.edit.CheckoutCollectDemographicInfoFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.CheckoutCollectInsuranceFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.CheckoutCollectPayerFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.CheckoutCollectPayerInfoFragmentDirections
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditHandToPatientFragmentArgs

interface CheckoutCollectInfoDestination {
    fun toSummaryFromDemo(fragment: Fragment, appointmentId: Int)

    fun toNewPayerScreenFromDemo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper)

    fun toSelectPayerScreenFromDemo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper)

    fun toCollectInsuranceFromDemo(fragment: Fragment?)

    fun toSelectPayerFromPayerInfo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper)

    fun toSummaryFromPayerInfo(fragment: Fragment, appointmentId: Int)

    fun toPhoneCollectionFlow(
        fragment: Fragment,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    )

    fun toPhoneCollectFlowFromCollectPayerGa(
        fragment: Fragment,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    )

    fun toCollectInsuranceFromCollectPayorInfo(fragment: Fragment, payer: Payer?)

    fun toCollectInsuranceFromSelectPayor(fragment: Fragment, payer: Payer)

    fun toSummaryFromPayer(fragment: Fragment, appointmentId: Int)

    fun toSummaryFromInsurance(fragment: Fragment, appointmentId: Int)
}

class CheckoutCollectInfoDestinationImpl(private val navCommons: NavCommons) :
    CheckoutCollectInfoDestination {
    override fun toSummaryFromDemo(fragment: Fragment, appointmentId: Int) {
        val action = CheckoutCollectDemographicInfoFragmentDirections
            .actionCollectDemographicsFragmentToCheckoutSummaryFragment(appointmentId = appointmentId)

        navCommons.goToFragment(fragment, action)
    }

    override fun toNewPayerScreenFromDemo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper) {
        val action =
            CheckoutCollectDemographicInfoFragmentDirections
                .actionCollectDemographicsFragmentToCollectPayerInfoFragment(
                    infoWrapper = infoWrapper
                )
        navCommons.goToFragment(fragment, action)
    }

    override fun toSelectPayerScreenFromDemo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper) {
        val action =
            CheckoutCollectDemographicInfoFragmentDirections
                .actionCollectDemographicsFragmentToCollectPayerFragment(infoWrapper)
        navCommons.goToFragment(fragment, action)
    }

    override fun toSelectPayerFromPayerInfo(fragment: Fragment?, infoWrapper: InvalidInfoWrapper) {
        val action = CheckoutCollectPayerInfoFragmentDirections
            .actionCollectPayerInfoFragmentToCollectPayerFragment(infoWrapper)

        navCommons.goToFragment(fragment, action)
    }

    override fun toCollectInsuranceFromDemo(fragment: Fragment?) {
        val action = CheckoutCollectDemographicInfoFragmentDirections
            .actionCollectDemographicsFragmentToCollectInsuranceFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun toSummaryFromPayerInfo(fragment: Fragment, appointmentId: Int) {
        val action = CheckoutCollectPayerInfoFragmentDirections
            .actionCollectPayerInfoFragmentToCheckoutSummaryFragment(appointmentId = appointmentId)

        navCommons.goToFragment(fragment, action)
    }

    override fun toPhoneCollectFlowFromCollectPayerGa(
        fragment: Fragment,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    ) {
        val action = CheckoutCollectPayerInfoFragmentDirections
            .actionCollectPayerInfoFragmentToPatientNoCardFragment(
                flow = NoInsuranceCardFlow.CHECKOUT_PATIENT,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = currentPhone
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun toPhoneCollectionFlow(
        fragment: Fragment,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    ) {
        navCommons.goToFragment(
            fragment,
            R.id.patientHandToPatientFragment,
            PatientEditHandToPatientFragmentArgs(
                flow = NoInsuranceCardFlow.CHECKOUT_PATIENT,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = currentPhone
            ).toBundle()
        )
    }

    override fun toCollectInsuranceFromCollectPayorInfo(fragment: Fragment, payer: Payer?) {
        val action = CheckoutCollectPayerInfoFragmentDirections
            .actionCollectPayerInfoFragmentToCollectInsuranceFragment(payer = payer)

        navCommons.goToFragment(fragment, action)
    }

    override fun toCollectInsuranceFromSelectPayor(fragment: Fragment, payer: Payer) {
        val action = CheckoutCollectPayerFragmentDirections
            .actionCollectPayerFragmentToCollectInsuranceFragment(payer = payer)

        navCommons.goToFragment(fragment, action)
    }

    override fun toSummaryFromPayer(fragment: Fragment, appointmentId: Int) {
        val action = CheckoutCollectPayerFragmentDirections
            .actionCollectPayerFragmentToCheckoutSummaryFragment(appointmentId = appointmentId)

        navCommons.goToFragment(fragment, action)
    }

    override fun toSummaryFromInsurance(fragment: Fragment, appointmentId: Int) {
        val action = CheckoutCollectInsuranceFragmentDirections
            .actionCollectInsuranceFragmentToCheckoutSummaryFragment(appointmentId = appointmentId)

        navCommons.goToFragment(fragment, action)
    }
}
