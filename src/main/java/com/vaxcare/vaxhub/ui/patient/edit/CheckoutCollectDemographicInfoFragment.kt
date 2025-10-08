/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.makeLongToast
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentCollectDemographicInfoBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.UpdatePatientData
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.navigation.CheckoutCollectInfoDestination
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutCollectDemographicInfoViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutCollectDemographicInfoViewModel.CollectDemoInfoUIState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutCollectDemographicInfoFragment :
    BaseScanDriverLicenseFragment<FragmentCollectDemographicInfoBinding>() {
    val args: CheckoutCollectDemographicInfoFragmentArgs by navArgs()
    override val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment?.id ?: 0
    }

    companion object {
        const val FRAGMENT_TAG = "CheckoutCollectDemographicInfoFragment"
    }

    private val screenTitle = "EditPatientInfo"
    private val infoWrapper by lazy { args.infoWrapper }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var scannerManager: ScannerManager

    @Inject
    @MobilePicasso
    override lateinit var picasso: Picasso

    @Inject
    lateinit var destination: CheckoutCollectInfoDestination

    private var patientId: Int = 0
    private var originalPhone: String = ""

    private val collectDemoViewModel: CheckoutCollectDemographicInfoViewModel by viewModels()
    private val isVaxCare3: Boolean
        get() = appointmentViewModel.currentCheckout.isVaxCare3

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_collect_demographic_info,
        hasMenu = false,
        hasToolbar = false,
        scannerPreview = R.id.scanner_preview
    )

    override fun bindFragment(container: View): FragmentCollectDemographicInfoBinding =
        FragmentCollectDemographicInfoBinding.bind(container)

    override fun handleDriverLicenseFound(driverLicense: DriverLicense) {
        this.driverLicense = driverLicense
        binding?.patientEditView?.updatePatientInfo(driverLicense)
        binding?.fabNext?.isEnabled = buildScanUpdatePatient() != null
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        appointmentViewModel.currentCheckout.revertPaymentFlips()
        observeBackStack()
        logScreenNavigation(screenTitle)
        handleStateChanges()
        binding?.topBar?.setTitle(resources.getString(R.string.patient_edit_title))
        super.init(view, savedInstanceState)
    }

    override fun setAppointmentInfo(appointment: Appointment) {
        val appt = appointmentViewModel.currentCheckout.selectedAppointment ?: appointment
        patientId = appt.patient.id
        originalPhone = appt.patient.phoneNumber ?: ""
        binding?.patientEditView?.apply {
            val missingFields = appt.getAllMissingDemographicFields()
            if (missingFields.isNotEmpty() && missingFields.size < 3) {
                binding?.patientEditDriverLicenseNote?.isGone = true
                binding?.viewFinderContainer?.isGone = true
                showMissingFieldsOnly(missingFields)
            }

            onPatientInfoChanged = {
                binding?.fabNext?.isEnabled = isValid()
            }

            addTextChangedListener()
            updatePatientInfo(appt.patient, appointmentViewModel.currentCheckout.manualDob)
        }
        binding?.fabNext?.setOnSingleClickListener {
            saveEditedDemographicInfo(appt.patient)
            binding?.patientEditView?.getDoB()?.let {
                appointmentViewModel.currentCheckout.manualDob = it
            }
            buildPatientUpdateData(appt)?.updatePatient?.let {
                viewModel.updateInfoLocally(
                    patientId = appt.patient.id,
                    updatePatient = it,
                    appointmentId = appt.id
                )
            }
        }

        binding?.collectRoot?.show()
    }

    override fun onInfoUpdated(appointment: Appointment) {
        appointmentViewModel.currentCheckout.selectedAppointment = appointment
        collectDemoViewModel.determineAndEmitDestination(infoWrapper)
    }

    private fun handleStateChanges() {
        lifecycleScope.launch {
            collectDemoViewModel.collectDemoUIState.collect { state ->
                when (state) {
                    is CollectDemoInfoUIState.NavigateToNewPayer -> destination.toNewPayerScreenFromDemo(
                        fragment = this@CheckoutCollectDemographicInfoFragment,
                        infoWrapper = args.infoWrapper
                    )

                    is CollectDemoInfoUIState.NavigateToPayerSelect -> destination.toSelectPayerScreenFromDemo(
                        fragment = this@CheckoutCollectDemographicInfoFragment,
                        infoWrapper = infoWrapper
                    )

                    is CollectDemoInfoUIState.NavigateToInsuranceScan ->
                        destination.toCollectInsuranceFromDemo(this@CheckoutCollectDemographicInfoFragment)

                    is CollectDemoInfoUIState.NavigateToPhoneCollect -> toPhoneCollectionFlow()

                    is CollectDemoInfoUIState.NavigateToSummary -> destination.toSummaryFromDemo(
                        fragment = this@CheckoutCollectDemographicInfoFragment,
                        appointmentId = appointmentId
                    )
                }
                collectDemoViewModel.resetState()
            }
        }
    }

    private fun saveEditedDemographicInfo(currentPatientInfo: Patient) =
        appointmentViewModel.addEditedFields(
            tagSet = FRAGMENT_TAG,
            fields = getListOfEditedDemographicFields(currentPatientInfo).toTypedArray()
        )

    private fun getListOfEditedDemographicFields(currentPatientInfo: Patient) =
        binding?.patientEditView?.listOfFields?.let { populatedFields ->
            populatedFields.filter { field ->
                val fieldValue = field.currentValue ?: return@filter false
                when (field) {
                    is DemographicField.FirstName -> fieldValue != currentPatientInfo.firstName
                    is DemographicField.LastName -> fieldValue != currentPatientInfo.lastName
                    is DemographicField.DateOfBirth -> fieldValue != currentPatientInfo.getDobString()
                    is DemographicField.Phone -> {
                        // TODO: [Tech Debt] Remove replace logic as part of CORE-23063
                        fieldValue != currentPatientInfo.phoneNumber?.replace("-", "")
                    }

                    is DemographicField.AddressOne -> fieldValue != currentPatientInfo.address1
                    is DemographicField.AddressTwo -> fieldValue != currentPatientInfo.address2
                    is DemographicField.City -> fieldValue != currentPatientInfo.city
                    is DemographicField.State -> fieldValue != currentPatientInfo.state
                    is DemographicField.Zip -> fieldValue != currentPatientInfo.zip
                    is DemographicField.Gender -> fieldValue != currentPatientInfo.gender?.let {
                        Patient.PatientGender.fromString(it).value
                    }

                    else -> false
                }
            }
        } ?: emptyList()

    private fun buildPatientUpdateData(appointment: Appointment): UpdatePatientData? {
        val newInfo = binding?.patientEditView?.newPatient?.toUpdatePatient(appointment.patient)
        return newInfo?.let { info ->
            UpdatePatientData(
                appointmentId = appointment.id,
                updatePatient = info
            )
        }
    }

    /**
     * Handle navigation callbacks for the phone collection flow
     */
    private fun observeBackStack() {
        observePhoneCollection()
        observeNoInsuranceCardSelected()
    }

    private fun observePhoneCollection() {
        getResultLiveData<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)?.observe(
            viewLifecycleOwner
        ) { data ->
            // handle phone collection flow
            val agreed =
                appointmentViewModel.addPhoneCollectField(
                    tagSet = COLLECT_PHONE_DATA_FRAGMENT_TAG,
                    data = data,
                    PhoneContactReasons.INSURANCE_CARD
                )

            if (isVaxCare3 && !agreed) {
                appointmentViewModel.currentCheckout.flipPartnerBill(
                    reason = PaymentModeReason.RequestedMediaNotProvided
                )
            }

            removeResult<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)
            destination.toSummaryFromDemo(
                fragment = this@CheckoutCollectDemographicInfoFragment,
                appointmentId = appointmentId
            )
        }
    }

    private fun observeNoInsuranceCardSelected() {
        getResultLiveData<Payer?>(BaseEditInsuranceFragment.NO_CARD_FLOW)?.observe(
            viewLifecycleOwner
        ) {
            // handle "No Insurance Card" was selected from the Collect Insurance fragment
            removeResult<Payer>(BaseEditInsuranceFragment.NO_CARD_FLOW)
            toPhoneCollectionFlow()
        }
    }

    private fun toPhoneCollectionFlow() {
        destination.toPhoneCollectionFlow(
            fragment = this@CheckoutCollectDemographicInfoFragment,
            appointmentId = appointmentId,
            patientId = patientId,
            currentPhone = binding?.patientEditView?.getPhoneNumber()
                ?: originalPhone
        )
    }

    override fun onError() {
        context?.makeLongToast(R.string.error_body)
    }
}
