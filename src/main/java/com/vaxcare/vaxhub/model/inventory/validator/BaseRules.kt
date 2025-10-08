/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.core.constant.RejectCodes

abstract class BaseRules<T> : ProductRules<T> {
    companion object {
        /**
         * Risk Reject Codes specific for Med D or missing information for InsurancePay
         * used in DoseNotCoveredRules
         */
        val riskRejectCodes = RejectCodes.riskRejectCodesForMissingInfoOrMedD

        /**
         * PaymentType strings where we are missing info but still want to mark doses not covered
         * used in DoseNotCoveredRules
         */
        val notInNetworkMessages by lazy {
            listOf(
                "Plan not in VaxCare's Billing Network",
                "Insurance not in VaxCare's Billing Network"
            )
        }

        /**
         * InventoryGroup covered by Med B
         * used in DoseNotCoveredRules
         */
        val coveredInventoryGroup = listOf(
            "PCV15",
            "PCV20",
            "PPSV23",
            "PCV13",
            "PCV 21",
            "Hep B (Adult)"
        )

        /**
         * Valid reject code to antigens grouping rule
         * used in DoseNotCoveredRules
         */
        val rejectCodeToAntigenGroupings: List<Pair<String, List<String>>> = listOf(
            "1252" to listOf(
                "COVID", "Dt", "DTaP", "DTaP-HepB-IPV", "DTaP-IPV/Hib", "DTaP + IPV + HIB + Hep B",
                "HepB Ped", "HIB", "Hib-HepB", "HPV", "HPV4", "HPV9", "IPV", "DTaP-IPV", "MenACWY",
                "MPSV4", "MMR", "MMRV", "PCV13", "PPSV23", "Rotavirus", "Varicella", "Zoster"
            ),
            "10019" to listOf(
                "COVID", "COVID", "Dt", "DTaP", "DTaP-HepB-IPV", "DTaP-IPV/Hib", "HepA Adult",
                "HepA-HepB", "HepB Ped", "HepB Adult", "HIB", "Hib-HepB", "HPV", "HPV4", "HPV9",
                "IPV", "DTaP-IPV", "MenB", "MenACWY", "MPSV4", "MMR", "MMRV", "PCV13", "PPSV23",
                "Rotavirus", "RSV", "TD", "Tdap", "Varicella", "Zoster"
            ),
            "10023" to listOf("Influenza", "PCV13", "PPSV23", "Rotavirus", "RSV", "Tdap", "Zoster")
        )
    }
}
