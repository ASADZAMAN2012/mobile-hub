/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

sealed class TestPartners(
    val partnerID: String,
    val clinicID: String,
    val pin: String,
    val adminPwd: String,
    val partnerName: String,
    val clinicName: String
) {
    object RprdCovidPartner :
        TestPartners(
            partnerID = "178764",
            clinicID = "89534",
            pin = "6115",
            adminPwd = "vxc3",
            partnerName = "QA Automation",
            clinicName = "QA One"
        )

    object InvalidPartner :
        TestPartners(
            partnerID = "111111",
            clinicID = "55555",
            pin = "",
            adminPwd = "vxc3",
            partnerName = "",
            clinicName = "Invalid Clinic"
        )

    object QALARCOnlyAutomationPartner :
        TestPartners(
            partnerID = "179001",
            clinicID = "90775",
            pin = "9198",
            adminPwd = "vxc3",
            partnerName = "QA LARC Automation",
            clinicName = "LARC Only QA"
        )
}
