/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.model.enums.AddPatientSource
import com.vaxcare.vaxhub.core.ui.BaseScannerFragment
import com.vaxcare.vaxhub.databinding.FragmentCurbsideAddNewPatientBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.checkout.dialog.InvalidScanMessageType
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.PatientDestination
import com.vaxcare.vaxhub.viewmodel.AddNewPatientViewModel
import com.vaxcare.vaxhub.viewmodel.LocationDataViewModel
import com.vaxcare.vaxhub.viewmodel.State
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CurbsideAddNewPatientFragment :
    BaseScannerFragment<FragmentCurbsideAddNewPatientBinding, AddNewPatientViewModel>() {
    private val locationDataViewModel: LocationDataViewModel by activityViewModels()
    override val viewModel: AddNewPatientViewModel by viewModels()

    private val screenTitle = "CurbsideAddNewPatient"

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var scannerManager: ScannerManager

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: PatientDestination

    @Inject
    lateinit var localStorage: LocalStorage

    private val args: CurbsideAddNewPatientFragmentArgs by navArgs()
    override val scanType: ScannerManager.ScanType = ScannerManager.ScanType.DRIVER_LICENSE

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_curbside_add_new_patient,
        hasMenu = false,
        hasToolbar = false,
        scannerPreview = R.id.scanner_preview
    )

    override fun bindFragment(container: View): FragmentCurbsideAddNewPatientBinding =
        FragmentCurbsideAddNewPatientBinding.bind(container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appointmentViewModel.clearAppointmentCreation()

        if (localStorage.showScannerPrompt) {
            destination.goToKeepDevice(this@CurbsideAddNewPatientFragment)

            localStorage.showScannerPrompt = false
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        logScreenNavigation(screenTitle)

        appointmentViewModel.appointmentCreation.newPatient?.let {
            binding?.patientEditView?.updatePatientInfo(it)
            binding?.patientEditView?.addTextChangedListener()
        }

        val appointmentId = args.appointmentId
        if (appointmentId > 0) {
            viewModel.loadAppointment(appointmentId)

            viewModel.appointment.observe(viewLifecycleOwner) { appointment ->
                when (AddPatientSource.fromInt(args.addPatientSource)) {
                    AddPatientSource.ADD_SUGGEST_PARENT_PATIENT -> {
                        if (appointmentViewModel.appointmentCreation.newPatient == null) {
                            binding?.patientEditView?.updateSuggestParentPatient(appointment?.patient)
                        }
                    }

                    AddPatientSource.ADD_NEW_PARENT_PATIENT -> {
                        if (appointmentViewModel.appointmentCreation.newPatient == null) {
                            binding?.patientEditView?.updateNewParentPatient(appointment?.patient)
                        }
                    }

                    else -> Unit
                }
                binding?.patientEditView?.addTextChangedListener()
            }
        } else {
            locationDataViewModel.getLocation().observe(viewLifecycleOwner) { location ->
                location?.let {
                    if (appointmentViewModel.appointmentCreation.newPatient == null) {
                        binding?.patientEditView?.updateState(it)
                    }
                }
            }
            binding?.patientEditView?.addTextChangedListener()
        }

        binding?.topBar?.onCloseAction = {
            globalDestinations.goBackToSplash(this@CurbsideAddNewPatientFragment)
        }

        binding?.patientEditView?.onPatientInfoChanged = {
            binding?.fabNext?.isEnabled = binding?.patientEditView?.isValid() == true
        }

        binding?.fabNext?.setOnClickListener {
            appointmentViewModel.appointmentCreation.patientId = null
            appointmentViewModel.appointmentCreation.newPatient =
                binding?.patientEditView?.newPatient

            destination.goToCaptureFrontDriverLicence(
                this@CurbsideAddNewPatientFragment,
                args.appointmentId,
                args.addPatientSource
            )
        }
    }

    override fun handleState(state: State) {
        // Do nothing
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
        hideKeyboard()
        super.onDestroyView()
    }

    override fun handleDriverLicenseFound(driverLicense: DriverLicense) {
        binding?.patientEditView?.updatePatientInfo(driverLicense)
    }
}
