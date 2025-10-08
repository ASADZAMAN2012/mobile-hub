/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.squareup.picasso.Picasso
import com.vaxcare.vaxhub.core.ui.BaseScannerFragment
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.UpdatePatient
import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.Race
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.checkout.dialog.InvalidScanMessageType
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.PatientInfoViewModel
import com.vaxcare.vaxhub.viewmodel.PatientInfoViewModel.PatientInfoState
import com.vaxcare.vaxhub.viewmodel.State

abstract class BaseScanDriverLicenseFragment<T : ViewBinding> :
    BaseScannerFragment<T, PatientInfoViewModel>() {
    protected abstract val appointmentId: Int

    protected abstract val picasso: Picasso
    override val viewModel: PatientInfoViewModel by viewModels()
    protected var appointment: Appointment? = null
    protected var driverLicense: DriverLicense? = null

    override val scanType: ScannerManager.ScanType = ScannerManager.ScanType.DRIVER_LICENSE

    override val scan2d: Boolean = false
    override val scan1d: Boolean = true

    protected abstract fun setAppointmentInfo(appointment: Appointment)

    protected abstract fun onInfoUpdated(appointment: Appointment)

    protected abstract fun onError()

    protected fun buildScanUpdatePatient(): UpdatePatient? {
        val appointment = this.appointment ?: return null
        val driverLicense = this.driverLicense ?: return null
        var zipcode = driverLicense.addressZip?.trim()
        if ((zipcode?.length ?: 0) >= 5) {
            zipcode = zipcode?.substring(0, 5)
        }
        val patient = appointment.patient
        return try {
            UpdatePatient(
                id = patient.id,
                address1 = driverLicense.addressStreet?.trim(),
                address2 = null,
                city = driverLicense.addressCity?.trim(),
                state = driverLicense.addressState?.trim(),
                zip = zipcode,
                originatorPatientId = patient.originatorPatientId,
                firstName = requireNotNull(driverLicense.firstName).trim(),
                middleInitial = patient.middleInitial,
                lastName = requireNotNull(driverLicense.lastName).trim(),
                ethnicity = patient.ethnicity?.let { Ethnicity.fromString(it) },
                race = patient.race?.let { Race.fromString(it) },
                dob = requireNotNull(driverLicense.birthDate),
                gender = if (requireNotNull(driverLicense.gender) == DriverLicense.Gender.MALE) 0 else 1,
                ssn = patient.ssn,
                phoneNumber = requireNotNull(appointment.patient.phoneNumber).replace("-", ""),
                email = patient.email,
                paymentInformation = patient.paymentInformation?.let {
                    UpdatePatient.PaymentInformation(
                        id = it.id,
                        patientId = patient.id,
                        uninsured = it.uninsured,
                        insuranceName = it.insuranceName,
                        primaryInsuranceId = it.primaryInsuranceId,
                        primaryInsurancePlanId = it.primaryInsurancePlanId,
                        portalInsuranceMappingId = it.portalInsuranceMappingId,
                        primaryMemberId = it.primaryMemberId,
                        primaryGroupId = it.primaryGroupId,
                        mbi = it.mbi,
                        insuredFirstName = requireNotNull(driverLicense.firstName).trim(),
                        insuredLastName = requireNotNull(driverLicense.lastName).trim(),
                        insuredDob = requireNotNull(driverLicense.birthDate),
                        insuredGender = if (requireNotNull(driverLicense.gender) == DriverLicense.Gender.MALE) 0 else 1,
                        appointmentId = it.appointmentId,
                        relationshipToInsured = it.relationshipToInsured,
                        paymentMode = it.paymentMode,
                        vfcFinancialClass = it.vfcFinancialClass
                    )
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun onAppointmentLoaded(appointment: Appointment?) {
        appointment?.let {
            this.appointment = it
            setAppointmentInfo(it)
        }
    }

    private fun onAppointmentLocallyUpdated(appointment: Appointment?) {
        appointment?.let {
            this.appointment = it
            onInfoUpdated(appointment)
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        viewModel.fetchUpdatedAppointment(appointmentId)
    }

    override fun handleState(state: State) {
        when (state) {
            is PatientInfoState.AppointmentLoaded -> onAppointmentLoaded(state.appointment)
            is PatientInfoState.AppointmentLocallyUpdated -> onAppointmentLocallyUpdated(state.appointment)
            PatientInfoState.Failed -> onError()
            LoadingState -> Unit
        }
    }

    override fun handleLotNumberWithProduct(
        lotNumberWithProduct: LotNumberWithProduct,
        featureFlags: List<FeatureFlag>
    ) {
        // Do nothing
    }

    override fun handleScannedProductNotAllowed(
        messageToShow: String,
        title: String,
        messageType: InvalidScanMessageType
    ) {
        // Do nothing
    }

    override fun onDestroyView() {
        driverLicense = null
        viewModel.resetState()
        super.onDestroyView()
    }
}
