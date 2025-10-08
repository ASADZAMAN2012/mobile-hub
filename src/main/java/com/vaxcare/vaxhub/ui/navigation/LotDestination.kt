/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.ui.checkout.AddNewLotFragmentDirections
import com.vaxcare.vaxhub.ui.checkout.LotLookupFragmentDirections

interface LotDestination {
    fun goBack(fragment: Fragment?)

    fun goToAddNewLot(
        fragment: Fragment?,
        lotNumber: String,
        appointmentId: Int? = null,
        relativeDoS: String? = null
    )

    fun goBackToCheckoutVaccine(fragment: Fragment?)

    fun goToToNotInCOVIDAssist(fragment: Fragment, behavior: Int)

    fun goToConfirmEnteredLotNumberDialog(fragment: Fragment, enteredLotNumber: String)
}

class LotDestinationImpl(private val navCommons: NavCommons) : LotDestination {
    override fun goBack(fragment: Fragment?) {
        navCommons.goBack(fragment)
    }

    override fun goToAddNewLot(
        fragment: Fragment?,
        lotNumber: String,
        appointmentId: Int?,
        relativeDoS: String?
    ) {
        val directions =
            LotLookupFragmentDirections.actionLotLookupFragmentToAddNewLotFragment(
                lotNumber = lotNumber,
                appointmentId = appointmentId?.toString(),
                relativeDoS = relativeDoS
            )

        navCommons.goToFragment(fragment, directions)
    }

    override fun goBackToCheckoutVaccine(fragment: Fragment?) {
        navCommons.goBackPopTo(fragment, R.id.checkoutPatientFragment)
    }

    override fun goToToNotInCOVIDAssist(fragment: Fragment, behavior: Int) {
        val action =
            AddNewLotFragmentDirections.actionAddNewLotFragmentToNotInCOVIDAssistDialog(
                checkCovidAction = behavior
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToConfirmEnteredLotNumberDialog(fragment: Fragment, enteredLotNumber: String) {
        val action =
            LotLookupFragmentDirections.actionLotLookupFragmentToConfirmEnteredLotNumberDialog(
                enteredLotNumber
            )

        navCommons.goToFragment(fragment, action)
    }
}
