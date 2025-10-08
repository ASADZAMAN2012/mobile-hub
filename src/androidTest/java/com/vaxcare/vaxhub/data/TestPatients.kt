/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import java.time.LocalDate

sealed class TestPatients(
    val firstName: String,
    lastName: String,
    val eligibilityMessage: String,
    val administeredUser: String,
    age: Long,
    val primaryInsuranceId: Int? = null,
    val gender: Int,
    val primaryMemberId: String? = null,
    val clinicId: Int? = null,
    val providerId: Int? = null,
    val primaryGroupId: String? = null,
    val stock: String? = null,
    val paymentMode: String? = null,
    val ssn: String? = null
) {
    val lastName = lastName.plus("_").plus(System.currentTimeMillis().toString().substring(4, 10))
    val completePatientName = this.lastName.plus(", ").plus(firstName)
    val dateOfBirth = LocalDate.now().minusYears(age)

    class RiskFreePatientForCheckout :
        TestPatients(
            firstName = "Tammy",
            lastName = "RiskFree",
            eligibilityMessage = "Guaranteed Payment",
            administeredUser = "",
            gender = 0,
            age = 40,
            primaryInsuranceId = 1000023151,
            primaryMemberId = "abc123"
        )

    class RiskFreePatientForEditCheckout :
        TestPatients(
            firstName = "Sharon",
            lastName = "RiskFree",
            eligibilityMessage = "Guaranteed Payment",
            administeredUser = "",
            age = 40,
            gender = 0,
            primaryInsuranceId = 1000023151,
            primaryMemberId = "abc123"
        )

    class MedDPatientForCopayRequired :
        TestPatients(
            firstName = "MedD",
            lastName = "Eligible",
            eligibilityMessage = "Guaranteed Payment",
            administeredUser = "",
            age = 65,
            gender = 0,
            primaryInsuranceId = 7,
            primaryMemberId = "EG4TE5MK73"
        )

    class MedDWithSsnPatientForCopayRequired :
        TestPatients(
            firstName = "MedD",
            lastName = "Eligible",
            eligibilityMessage = "Guaranteed Payment",
            administeredUser = "",
            age = 65,
            gender = 0,
            primaryInsuranceId = 7,
            primaryMemberId = "EG4TE5MK73",
            ssn = "123121234"
        )

    class RiskFreePatientForCreatePatient :
        TestPatients(
            firstName = "Tammy",
            lastName = "RiskFree",
            eligibilityMessage = "Guaranteed Payment",
            administeredUser = "",
            age = 10,
            gender = 0,
            primaryInsuranceId = 1000023151,
            primaryMemberId = "abc123"
        )

    class QaRobotPatient :
        TestPatients(
            firstName = "Mayah",
            lastName = "Miller",
            eligibilityMessage = "Guaranteed Payment",
            administeredUser = "",
            age = 10,
            gender = 0,
            primaryInsuranceId = 1000023151,
            primaryMemberId = "abc123"
        )

    class PregnantPatient :
        TestPatients(
            firstName = "Mayah",
            lastName = "Miller",
            eligibilityMessage = "Guaranteed Payment",
            administeredUser = "",
            age = 20,
            gender = 1,
            primaryInsuranceId = 1000023151,
            primaryMemberId = "abc123"
        )

    class MissingPatientWithPayerInfo :
        TestPatients(
            firstName = "Invalid-AutoTest",
            lastName = "Payername",
            eligibilityMessage = "New Payer Info Required",
            administeredUser = "",
            age = 40,
            gender = 0,
            primaryInsuranceId = 1000023151,
            primaryMemberId = "abc123"
        )

    class MissingPatientWithAllPayerInfo :
        TestPatients(
            firstName = "Invalid",
            lastName = "Payername",
            eligibilityMessage = "New Payer Info Required",
            administeredUser = "",
            age = 40,
            gender = 0,
            primaryInsuranceId = 1000023151,
            primaryMemberId = "abc123",
            primaryGroupId = "abc"
        )

    class MissingPatientWithDemoInfo :
        TestPatients(
            firstName = "Invalid",
            lastName = "Patientinfo",
            eligibilityMessage = "Patient Info Required",
            administeredUser = "",
            age = 40,
            gender = 0,
            primaryInsuranceId = 2,
            primaryMemberId = "10742845GBHZ"
        )

    class SelfPayPatient :
        TestPatients(
            firstName = "SelfPay",
            lastName = "Patient",
            eligibilityMessage = "Payment Required",
            administeredUser = "",
            age = 40,
            gender = 0,
            paymentMode = "2"
        )

    class SelfPayPatient2 :
        TestPatients(
            firstName = "SelfPay",
            lastName = "Patient",
            eligibilityMessage = "Payment Required",
            administeredUser = "",
            age = 30,
            gender = 0,
            paymentMode = "SelfPay",
            stock = "Private",
        )

    class PartnerBillPatient :
        TestPatients(
            firstName = "PB",
            lastName = "Patient",
            eligibilityMessage = "Ready to Vaccinate",
            administeredUser = "",
            age = 40,
            gender = 0,
            primaryInsuranceId = 2,
            primaryMemberId = "10742845GBHZ",
            paymentMode = "1"
        )

    class VFCPatient :
        TestPatients(
            firstName = "VFC",
            lastName = "Eligible",
            eligibilityMessage = "Ready to Vaccinate",
            administeredUser = "",
            age = 10,
            gender = 0,
            primaryInsuranceId = 2,
            primaryMemberId = "10742845GBHZ",
            paymentMode = "4"
        )
}
