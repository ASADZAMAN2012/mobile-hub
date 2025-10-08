/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.ui.extension.hide
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.AndroidBug5497Workaround
import com.vaxcare.vaxhub.core.extension.hideKeyboard
import com.vaxcare.vaxhub.core.extension.isAlphaNumeric
import com.vaxcare.vaxhub.core.extension.isPartialSsn
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.trimDashes
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseScannerFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentAddNewPatientBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.Race
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.metric.CreatePatientExitMetric
import com.vaxcare.vaxhub.model.metric.PatientInfoConfirmationClickMetric
import com.vaxcare.vaxhub.model.patient.AddNewPatientDemographics
import com.vaxcare.vaxhub.service.ScannerManager
import com.vaxcare.vaxhub.ui.checkout.dialog.InvalidScanMessageType
import com.vaxcare.vaxhub.ui.checkout.dialog.PatientInfoConfirmationDialog
import com.vaxcare.vaxhub.ui.navigation.AddPatientsDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AddNewPatientViewModel
import com.vaxcare.vaxhub.viewmodel.State
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddPatientSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AddNewPatientFragment :
    BaseScannerFragment<FragmentAddNewPatientBinding, AddNewPatientViewModel>() {
    private val addPatientSharedViewModel: AddPatientSharedViewModel by activityViewModels()
    override val viewModel: AddNewPatientViewModel by viewModels()

    private val screenTitle = "AddNewPatient"

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var scannerManager: ScannerManager

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var addPatientsDestination: AddPatientsDestination

    override val scanType: ScannerManager.ScanType = ScannerManager.ScanType.DRIVER_LICENSE

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_add_new_patient,
        hasMenu = false,
        hasToolbar = false,
        scannerPreview = R.id.scanner_preview
    )

    private var viewAssist: AndroidBug5497Workaround? = null
    private val genders = DriverLicense.Gender.values()
    private var selectedGender: DriverLicense.Gender? = null
    private var selectedRace: Race? = null
    private var selectedEthnicity: Ethnicity? = null
    private var selectedState: String? = null
    private val states by lazy {
        listOf(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
        )
    }

    override fun bindFragment(container: View): FragmentAddNewPatientBinding =
        FragmentAddNewPatientBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        viewAssist = AndroidBug5497Workaround.assistActivity(requireActivity())
        logScreenNavigation(screenTitle)

        binding?.topBar?.onCloseAction = { handleBack() }
        addListeners()

        addPatientSharedViewModel.newPatientLiveData.observe(viewLifecycleOwner) { existing ->
            existing?.let { populateExistingInfo(it) }
        }
    }

    override fun handleState(state: State) {}

    override fun handleBack(): Boolean {
        if (isAnyFieldPopulated()) {
            displayExitPromptAndSetListener()
        } else {
            globalDestinations.goBackToAppointmentList(this@AddNewPatientFragment)
        }
        return true
    }

    private fun displayExitPromptAndSetListener() {
        setFragmentResultListener(AddNewPatientExitDialog.ADD_NEW_PATIENT_EXIT_KEY) { _, listener ->
            run {
                val userSelection = listener.getBoolean(AddNewPatientExitDialog.ADD_NEW_PATIENT_EXIT_RESULT)
                analytics.saveMetric(CreatePatientExitMetric(userSelection))
                if (userSelection) {
                    globalDestinations.goBackToAppointmentList(this@AddNewPatientFragment)
                }
            }
        }
        addPatientsDestination.displayExitDialog(this@AddNewPatientFragment)
    }

    fun isAnyFieldPopulated(): Boolean {
        var isDataEntered = false
        binding?.apply {
            isDataEntered = roundedTextInputAddFirstName.inputValue.isNotEmpty() ||
                roundedTextInputAddLastName.inputValue.isNotEmpty() ||
                dateViewAddBirthDate.isNotEmpty() ||
                textViewAddGender.text.isNotEmpty() ||
                phoneViewAddPhone.isNotEmpty() ||
                roundedTextInputAddSsn.inputValue.isNotEmpty() ||
                roundedTextInputAddMbi.inputValue.isNotEmpty() ||
                editTextAddAddressOne.text.isNotEmpty() ||
                editTextAddAddressTwo.text.isNotEmpty() ||
                editTextAddCity.text.isNotEmpty() ||
                textViewAddState.text.isNotEmpty() ||
                editTextAddZip.text.isNotEmpty() ||
                textViewAddRace.text.isNotEmpty() ||
                textViewAddEthnicity.text.isNotEmpty()
        }
        return isDataEntered
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
        viewAssist?.stopAssistingActivity()
        super.onDestroyView()
    }

    override fun handleDriverLicenseFound(driverLicense: DriverLicense) {
        scannerManager.stopScanner()
        subscribeAndShowPatientInfoConfirmationDialog(driverLicense)
    }

    private fun populateExistingInfo(newPatient: PatientPostBody.NewPatient) {
        binding?.apply {
            dateViewAddBirthDate.setDob(newPatient.dob)
            selectedGender = DriverLicense.Gender.fromInt(newPatient.gender)
            textViewAddGender.text = selectedGender?.value
            phoneViewAddPhone.setphone(newPatient.phoneNumber)
            editTextAddAddressOne.setText(newPatient.address1?.trim())
            editTextAddAddressTwo.setText(newPatient.address2?.trim())
            editTextAddCity.setText(newPatient.city?.trim())
            selectedState = newPatient.state?.trim()
            textViewAddState.text = newPatient.state?.trim()
            editTextAddZip.setText(newPatient.zip?.trim())
            selectedRace = newPatient.race
            textViewAddRace.text = selectedRace?.value
            selectedEthnicity = newPatient.ethnicity
            textViewAddEthnicity.text = selectedEthnicity?.value
            roundedTextInputAddFirstName.setValue(newPatient.firstName)
            roundedTextInputAddLastName.setValue(newPatient.lastName)
            roundedTextInputAddSsn.setValue(newPatient.ssn)
            roundedTextInputAddMbi.setValue(newPatient.paymentInformation?.mbi)
        }
    }

    private fun subscribeAndShowPatientInfoConfirmationDialog(driverLicense: DriverLicense) {
        setFragmentResultListener(PatientInfoConfirmationDialog.REQUEST_KEY) { _, listener ->
            when (listener.getInt(PatientInfoConfirmationDialog.OPTION_SELECTED_BUNDLE_KEY)) {
                PatientInfoConfirmationDialog.Option.YES.ordinal -> {
                    updatePatientInfoFromLicense(driverLicense)
                    viewModel.saveMetric(
                        PatientInfoConfirmationClickMetric(
                            PatientInfoConfirmationDialog.Option.YES
                        )
                    )
                    scannerManager.startScanner()
                }

                PatientInfoConfirmationDialog.Option.NO.ordinal -> {
                    viewModel.saveMetric(
                        PatientInfoConfirmationClickMetric(
                            PatientInfoConfirmationDialog.Option.NO
                        )
                    )
                    scannerManager.startScanner()
                }
            }
        }

        globalDestinations.goToPatientInfoConfirmationDialog(
            this,
            firstName = driverLicense.firstName ?: "",
            lastName = driverLicense.lastName ?: "",
            dateOfBirth = driverLicense.birthDate?.format(
                DateTimeFormatter.ofPattern("M/dd/yyyy")
            ) ?: "",
            address = "${driverLicense.addressStreet}, " +
                "${driverLicense.addressCity}, " +
                "${driverLicense.addressState} " +
                "${driverLicense.addressZip}"
        )
    }

    private fun addListeners() {
        addOnFocusListeners()
        addOnClickListeners()
        addTextChangeListeners()
    }

    private fun updatePatientInfoFromLicense(driverLicense: DriverLicense) {
        binding?.apply {
            cleanFieldsIncludedInDriversLicense()

            driverLicense.firstName?.let {
                roundedTextInputAddFirstName.setValue(it.trim())
            }
            driverLicense.lastName?.let {
                roundedTextInputAddLastName.setValue(it.trim())
            }
            driverLicense.birthDate?.let {
                dateViewAddBirthDate.setDob(it)
            }
            driverLicense.gender?.let {
                selectedGender = it
                textViewAddGender.text = selectedGender?.value
                textViewAddGenderError.hide()
            }
            driverLicense.addressStreet?.let {
                editTextAddAddressOne.setText(it.trim())
            }
            driverLicense.addressCity?.let {
                editTextAddCity.setText(it.trim())
            }
            driverLicense.addressState?.let {
                selectedState = it.trim()
                textViewAddState.text = it.trim()
            }
            driverLicense.addressZip?.let {
                editTextAddZip.setText(it.trim())
            }

            validateForm()
        }
    }

    private fun FragmentAddNewPatientBinding.cleanFieldsIncludedInDriversLicense() {
        roundedTextInputAddFirstName.setValue("")
        roundedTextInputAddLastName.setValue("")
        dateViewAddBirthDate.setDob(null)
        selectedGender = null
        textViewAddGender.text = ""
        editTextAddAddressOne.setText("")
        editTextAddAddressTwo.setText("")
        editTextAddCity.setText("")
        selectedState = null
        textViewAddState.text = ""
        editTextAddZip.setText("")
    }

    private fun getNewPatientModel(): AddNewPatientDemographics? {
        return binding?.let {
            AddNewPatientDemographics(
                firstName = it.roundedTextInputAddFirstName.inputValue.trim(),
                lastName = it.roundedTextInputAddLastName.inputValue.trim(),
                dob = it.dateViewAddBirthDate.getDob()!!,
                gender = if (selectedGender == DriverLicense.Gender.MALE) 0 else 1,
                phoneNumber = it.phoneViewAddPhone.phone,
                address1 = if (it.editTextAddAddressOne.text.isBlank()) {
                    null
                } else {
                    it.editTextAddAddressOne.text.trim()
                        .toString()
                },
                address2 = if (it.editTextAddAddressTwo.text.isBlank()) {
                    null
                } else {
                    it.editTextAddAddressTwo.text.trim()
                        .toString()
                },
                city = if (it.editTextAddCity.text.isBlank()) {
                    null
                } else {
                    it.editTextAddCity.text.trim()
                        .toString()
                },
                state = if (it.textViewAddState.text.isBlank()) {
                    null
                } else {
                    it.textViewAddState.text.trim()
                        .toString()
                },
                zip = if (it.editTextAddZip.text.isBlank()) {
                    null
                } else {
                    it.editTextAddZip.text.trim()
                        .toString()
                },
                race = selectedRace,
                ethnicity = selectedEthnicity,
                ssn = it.roundedTextInputAddSsn.inputValue,
                mbi = it.roundedTextInputAddMbi.inputValue
            )
        }
    }

    private fun validateForm(): Boolean {
        var isValidationPass = true
        binding?.apply {
            if (!roundedTextInputAddFirstName.validate()) {
                scrollToView(roundedTextInputAddFirstName)
                roundedTextInputAddFirstName.requestFocus()
                showKeyboard()
                isValidationPass = false
            }
            if (!roundedTextInputAddLastName.validate()) {
                if (isValidationPass) {
                    scrollToView(roundedTextInputAddLastName)
                    roundedTextInputAddLastName.requestFocus()
                    showKeyboard()
                    isValidationPass = false
                }
            }
            if (!dateViewAddBirthDate.validate()) {
                if (isValidationPass) {
                    scrollToView(dateViewAddBirthDate)
                    isValidationPass = false
                }
            }
            if (selectedGender == null) {
                textViewAddGenderError.show()
                if (isValidationPass) {
                    scrollToView(textViewAddGender)
                    isValidationPass = false
                }
            }
            if (!phoneViewAddPhone.validate()) {
                if (isValidationPass) {
                    scrollToView(phoneViewAddPhone)
                    phoneViewAddPhone.setFocus()
                    showKeyboard()
                    isValidationPass = false
                }
            }
        }
        return isValidationPass
    }

    private fun scrollToView(view: View) {
        binding?.scrollView?.smoothScrollTo(view.scrollX, view.scrollY)
    }

    private fun addOnFocusListeners() {
        val onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b) {
                (view as EditText).setSelection(view.text.length)
            }
        }
        binding?.apply {
            roundedTextInputAddFirstName.onFocusChangeListener = onFocusChangeListener
            roundedTextInputAddLastName.onFocusChangeListener = onFocusChangeListener
            roundedTextInputAddSsn.onFocusChangeListener = onFocusChangeListener
            roundedTextInputAddMbi.onFocusChangeListener = onFocusChangeListener
            editTextAddAddressOne.onFocusChangeListener = onFocusChangeListener
            editTextAddAddressTwo.onFocusChangeListener = onFocusChangeListener
            editTextAddCity.onFocusChangeListener = onFocusChangeListener
            editTextAddZip.onFocusChangeListener = onFocusChangeListener
        }
    }

    private fun addOnClickListeners() {
        binding?.apply {
            textViewAddGender.setOnClickListener { view ->
                view.hideKeyboard()
                val selectedIndex =
                    if (selectedGender != null) genders.indexOf(selectedGender) else -1
                val bottomDialog = BottomDialog.newInstance(
                    getString(R.string.gender),
                    genders.map { it.value },
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    selectedGender = genders[index]
                    textViewAddGender.text = selectedGender?.value
                    textViewAddGenderError.hide()
                }
                bottomDialog.show(
                    (activity as FragmentActivity).supportFragmentManager,
                    "genderBottomDialog"
                )
            }

            textViewAddRace.setOnClickListener { view ->
                view.hideKeyboard()
                val raceList = Race.values().map { it.value }.toList()
                val bottomDialog = BottomDialog.newInstance(
                    getString(R.string.patient_add_race),
                    raceList,
                    selectedRace?.ordinal ?: -1
                )
                bottomDialog.onSelected = { index ->
                    selectedRace = Race.fromString(raceList[index])
                    textViewAddRace.text = raceList[index]
                }
                bottomDialog.show(
                    (activity as FragmentActivity).supportFragmentManager,
                    "raceBottomDialog"
                )
            }

            textViewAddEthnicity.setOnClickListener { view ->
                view.hideKeyboard()
                val ethnicityList = Ethnicity.values().map { it.value }.toList()
                val bottomDialog = BottomDialog.newInstance(
                    getString(R.string.patient_add_ethnicity),
                    ethnicityList,
                    selectedEthnicity?.ordinal ?: -1
                )
                bottomDialog.onSelected = { index ->
                    selectedEthnicity = Ethnicity.fromString(ethnicityList[index])
                    textViewAddEthnicity.text = ethnicityList[index]
                }
                bottomDialog.show(
                    (activity as FragmentActivity).supportFragmentManager,
                    "ethnicityBottomDialog"
                )
            }

            textViewAddState.setOnClickListener {
                it.hideKeyboard()
                val state = selectedState
                val selectedIndex = if (state != null) states.indexOf(state) else -1
                val bottomDialog = BottomDialog.newInstance(
                    getString(R.string.patient_select_state),
                    states,
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    selectedState = states[index]
                    textViewAddState.text = selectedState
                }
                bottomDialog.show(
                    (activity as FragmentActivity).supportFragmentManager,
                    "stateBottomDialog"
                )
            }

            fabNext.setOnClickListener {
                if (validateForm()) {
                    addPatientSharedViewModel.newPatient = getNewPatientModel()
                    addPatientsDestination.navigateToSelectPayer(this@AddNewPatientFragment)
                }
            }
        }
    }

    private fun addTextChangeListeners() {
        binding?.apply {
            roundedTextInputAddSsn.inputField.addTextChangedListener(ssnTextChangeListener)
            roundedTextInputAddMbi.inputField.addTextChangedListener(mbiTextChangeListener)
        }
    }

    private val ssnFilterLength by lazy { InputFilter.LengthFilter(11) }
    private val ssnFilterChars by lazy {
        InputFilter { source, _, _, _, _, _ -> if (source.isAlphaNumeric()) "" else null }
    }
    private val ssnTextChangeListener = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {}

        override fun onTextChanged(
            text: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            onSsnTextChanged(text, binding?.roundedTextInputAddSsn?.inputField, count, this)
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun shouldAppendHyphenSsn(rawSsnText: String) =
        rawSsnText.lastOrNull() != '-' &&
            rawSsnText.trimDashes().isPartialSsn() &&
            (rawSsnText.length == 3 || rawSsnText.length == 6)

    private fun onSsnTextChanged(
        text: CharSequence?,
        inputField: EditText?,
        count: Int,
        textListener: TextWatcher
    ) {
        with(text.toString()) {
            inputField?.filters = arrayOf(ssnFilterChars, ssnFilterLength)
            when {
                lastOrNull() == '-' && count == 0 ->
                    inputField?.setAndSelectEndOfText(this.trimLastChar(), textListener)

                count == 0 -> Unit
                shouldAppendHyphenSsn(this) -> inputField?.setAndSelectEndOfText(
                    value = "$this-",
                    textListener = textListener
                )

                else -> Unit
            }
        }
    }

    private val mbiFilterLength by lazy { InputFilter.LengthFilter(13) }
    private val mbiFilterChars by lazy {
        InputFilter { source, _, _, _, _, _ -> if (source.isAlphaNumeric()) "" else null }
    }
    private val mbiTextChangeListener = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {}

        override fun onTextChanged(
            text: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            onMbiTextChanged(text, binding?.roundedTextInputAddMbi?.inputField, count, this)
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun shouldAppendHyphenMbi(rawMbiText: String) =
        rawMbiText.lastOrNull() != '-' &&
            !rawMbiText.trimDashes().isPartialSsn() &&
            (rawMbiText.length == 4 || rawMbiText.length == 8)

    private fun onMbiTextChanged(
        text: CharSequence?,
        inputField: EditText?,
        count: Int,
        textListener: TextWatcher
    ) {
        with(text.toString()) {
            inputField?.filters = arrayOf(InputFilter.AllCaps(), mbiFilterChars, mbiFilterLength)
            when {
                lastOrNull() == '-' && count == 0 ->
                    inputField?.setAndSelectEndOfText(this.trimLastChar(), textListener)

                count == 0 -> Unit
                shouldAppendHyphenMbi(this) -> inputField?.setAndSelectEndOfText(
                    value = "$this-",
                    textListener = textListener
                )

                else -> Unit
            }
        }
    }

    private fun EditText.setAndSelectEndOfText(value: String, textListener: TextWatcher) {
        removeTextChangedListener(textListener)
        setText(value)
        setSelection(value.length)
        addTextChangedListener(textListener)
    }

    private fun String.trimLastChar() = substring(0, (length - 1).coerceAtLeast(0))
}
