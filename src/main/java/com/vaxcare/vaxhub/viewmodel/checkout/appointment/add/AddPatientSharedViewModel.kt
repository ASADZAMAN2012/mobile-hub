/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel.checkout.appointment.add

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.patient.AddNewPatientDemographics
import com.vaxcare.vaxhub.model.patient.NewPatientBuilder
import com.vaxcare.vaxhub.model.patient.PayerInfoUiData
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AddPatientSharedViewModel @Inject constructor() : ViewModel() {
    private var builder = NewPatientBuilder()
    var newPatient: AddNewPatientDemographics? = null
        set(value) {
            field = value
            newPatientLiveData.postValue(updateNewPatientPostBody())
        }
    val newPatientLiveData = MutableLiveData(builder.build())

    var selectedPayer: Payer? = null
        set(value) {
            field = value
            builder.applyNewPayer(value)
        }

    fun clearData() {
        Timber.i("Data cleared")
        newPatient = null
        selectedPayer = null
        builder = NewPatientBuilder()
    }

    private fun NewPatientBuilder.applyNewPayer(payer: Payer?) {
        insuranceName(payer?.insuranceName)
        primaryInsuranceId(payer?.insuranceId)
        primaryInsurancePlanId(payer?.insurancePlanId)
    }

    fun extractPayerInfoUIData(): PayerInfoUiData {
        val paymentInfo = builder.build()?.paymentInformation
        val insuredDob = paymentInfo?.insuredDob?.let {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("M/dd/yyyy"))
        }

        return PayerInfoUiData(
            memberId = paymentInfo?.primaryMemberId,
            groupId = paymentInfo?.primaryGroupId,
            relationship = paymentInfo?.relationshipToInsured ?: RelationshipToInsured.Self,
            insuredFirstName = paymentInfo?.insuredFirstName,
            insuredLastName = paymentInfo?.insuredLastName,
            insuredDob = insuredDob,
            insuredGender = DriverLicense.Gender.fromString(paymentInfo?.insuredGender)
        )
    }

    fun savePayerInfoUIData(payerInfoUiData: PayerInfoUiData) {
        builder
            .primaryMemberId(payerInfoUiData.memberId)
            .primaryGroupId(payerInfoUiData.groupId)
            .relationshipToInsured(payerInfoUiData.relationship)
            .insuredFirstName(payerInfoUiData.insuredFirstName)
            .insuredLastName(payerInfoUiData.insuredLastName)
            .insuredDob(payerInfoUiData.insuredDob?.toLocalDateString())
            .insuredGender(payerInfoUiData.insuredGender?.value)
    }

    private fun updateNewPatientPostBody(): PatientPostBody.NewPatient? =
        newPatient?.let {
            builder
                .firstName(it.firstName)
                .lastName(it.lastName)
                .dob(it.dob)
                .gender(it.gender)
                .phoneNumber(it.phoneNumber)
                .address1(it.address1)
                .address2(it.address2)
                .city(it.city)
                .state(it.state)
                .zip(it.zip)
                .race(it.race)
                .ethnicity(it.ethnicity)
                .ssn(it.ssn)
                .mbi(it.mbi)
                .build()
        }

    fun buildAndGetNewPatient(): PatientPostBody.NewPatient? = builder.build()
}
