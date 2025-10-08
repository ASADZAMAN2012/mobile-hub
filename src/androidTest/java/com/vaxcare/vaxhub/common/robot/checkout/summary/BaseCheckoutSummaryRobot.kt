/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.summary

import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.model.MedDCheckResponse

abstract class BaseCheckoutSummaryRobot : BaseTestRobot() {
    abstract fun verifyTitle()

    /**
     * Returns CoPay amount based on dose administered
     *
     * @param medDCheckResponse - medD response object for that appt
     * @param testProducts - products administered
     * @return copay amount for the dose administered.
     */
    protected fun getCopayTotalValue(medDCheckResponse: MedDCheckResponse, testProducts: List<TestProducts>): String {
        var copays = medDCheckResponse.copays
        val sum: Double = testProducts.sumOf { product ->
            copays.filter { it.antigen == product.antigen }.sumOf { it.copay.toDouble() }
        }
        return sum.toString()
    }
}
