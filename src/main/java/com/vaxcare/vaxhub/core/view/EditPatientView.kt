/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getActivity
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.hideKeyboard
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.ViewEditPatientBinding
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.LocationData
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.UpdatePatient
import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.Race
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.model.patient.InfoField
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EditPatientView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    var onPatientInfoChanged: () -> Unit = {}

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

    val newPatient: PatientPostBody.NewPatient
        get() {
            return PatientPostBody.NewPatient(
                firstName = binding.patientAddFirstName.text.trim().toString(),
                lastName = binding.patientAddLastName.text.trim().toString(),
                dob = binding.patientAddDateOfBirth.getDob()!!,
                gender = if (selectedGender == DriverLicense.Gender.MALE) 0 else 1,
                phoneNumber = getPhoneNumber(),
                address1 = if (binding.patientAddAddress.text.trim()
                        .isEmpty()
                ) {
                    null
                } else {
                    binding.patientAddAddress.text.trim().toString()
                },
                address2 = if (binding.patientAddAddress2.text.trim()
                        .isEmpty()
                ) {
                    null
                } else {
                    binding.patientAddAddress2.text.trim().toString()
                },
                city = if (binding.patientAddCity.text.trim()
                        .isEmpty()
                ) {
                    null
                } else {
                    binding.patientAddCity.text.trim().toString()
                },
                state = if (binding.patientAddState.text.trim()
                        .isEmpty()
                ) {
                    null
                } else {
                    binding.patientAddState.text.trim().toString()
                },
                zip = if (binding.patientAddZipcode.text.trim()
                        .isEmpty()
                ) {
                    null
                } else {
                    binding.patientAddZipcode.text.trim().toString()
                },
                race = selectedRace,
                ethnicity = selectedEthnicity,
            )
        }

    val listOfFields: List<InfoField>
        get() {
            val validateEmpty: (String) -> String? = { it.ifEmpty { null } }
            val firstName = binding.patientAddFirstName.text.trim().toString()
            val lastName = binding.patientAddLastName.text.trim().toString()
            val dob = binding.patientAddDateOfBirth.getDob()
            val gender = if (selectedGender == DriverLicense.Gender.MALE) 0 else 1
            val phoneNumber = getPhoneNumber()
            val address1 = binding.patientAddAddress.text.trim().toString()
            val address2 = binding.patientAddAddress2.text.trim().toString()
            val city = binding.patientAddCity.text.trim().toString()
            val state = binding.patientAddState.text.trim().toString()
            val zip = binding.patientAddZipcode.text.trim().toString()

            return listOfNotNull(
                if (phoneNumber.length == 10) {
                    DemographicField.Phone(phoneNumber)
                } else {
                    null
                },
                validateEmpty(firstName)?.let { DemographicField.FirstName(it) },
                validateEmpty(lastName)?.let { DemographicField.LastName(it) },
                validateEmpty(address1)?.let { DemographicField.AddressOne(it) },
                validateEmpty(address2)?.let { DemographicField.AddressTwo(it) },
                validateEmpty(city)?.let { DemographicField.City(it) },
                validateEmpty(state)?.let { DemographicField.State(it) },
                validateEmpty(zip)?.let { DemographicField.Zip(it) },
                DemographicField.Gender(Patient.PatientGender.fromInt(gender).value),
                DemographicField.DateOfBirth(dob?.toLocalDateString())
            )
        }

    private val binding: ViewEditPatientBinding =
        ViewEditPatientBinding.inflate(context.getLayoutInflater(), this, true)

    init {
        val onFocusChangeListener = OnFocusChangeListener { view, b ->
            if (b) {
                (view as EditText).setSelection(view.text.length)
            }
        }
        with(binding) {
            patientAddFirstName.onFocusChangeListener =
                onFocusChangeListener
            patientAddLastName.onFocusChangeListener = onFocusChangeListener
            patientAddPhoneStart.onFocusChangeListener =
                onFocusChangeListener
            patientAddPhoneMid.onFocusChangeListener = onFocusChangeListener
            patientAddPhoneEnd.onFocusChangeListener = onFocusChangeListener
            patientAddAddress.onFocusChangeListener = onFocusChangeListener
            patientAddAddress2.onFocusChangeListener = onFocusChangeListener
            patientAddCity.onFocusChangeListener = onFocusChangeListener
            patientAddZipcode.onFocusChangeListener = onFocusChangeListener
        }

        addListeners()
    }

    fun showMissingFieldsOnly(fieldList: List<DemographicField>) {
        binding.nonSimpleViewFields.isGone = true
        binding.patientAddFirstNameTitle.isGone =
            fieldList.none { it is DemographicField.FirstName }
        binding.patientAddFirstName.isGone = fieldList.none { it is DemographicField.FirstName }
        binding.patientAddLastNameTitle.isGone = fieldList.none { it is DemographicField.LastName }
        binding.patientAddLastName.isGone = fieldList.none { it is DemographicField.LastName }
        binding.patientAddPhoneTitle.isGone = fieldList.none { it is DemographicField.Phone }
        binding.patientAddPhoneStart.isGone = fieldList.none { it is DemographicField.Phone }
        binding.patientAddPhoneMid.isGone = fieldList.none { it is DemographicField.Phone }
        binding.patientAddPhoneEnd.isGone = fieldList.none { it is DemographicField.Phone }
        binding.patientAddGenderTitle.isGone = fieldList.none { it is DemographicField.Gender }
        binding.patientAddGender.isGone = fieldList.none { it is DemographicField.Gender }
        binding.genderDropdownIcon.isGone = fieldList.none { it is DemographicField.Gender }
        binding.patientAddDateOfBirth.isGone = fieldList.none { it is DemographicField.DateOfBirth }
    }

    fun addTextChangedListener() {
        binding.patientAddFirstName.doAfterTextChanged {
            onPatientInfoChanged()
        }

        binding.patientAddLastName.doAfterTextChanged {
            onPatientInfoChanged()
        }

        binding.patientAddLastName.setOnEditorActionListener(
            TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    hideKeyboard()
                    return@OnEditorActionListener true
                }
                false
            }
        )

        binding.patientAddDateOfBirth.onPatientInfoChanged = {
            onPatientInfoChanged()
        }

        binding.patientAddPhoneStart.doAfterTextChanged {
            if (binding.patientAddPhoneStart.text.length == 3) {
                binding.patientAddPhoneMid.requestFocus()
            }
            onPatientInfoChanged()
        }

        binding.patientAddPhoneMid.doAfterTextChanged {
            if (binding.patientAddPhoneMid.text.length == 3) {
                binding.patientAddPhoneEnd.requestFocus()
            } else if (binding.patientAddPhoneMid.text.isEmpty()) {
                binding.patientAddPhoneStart.requestFocus()
            }
            onPatientInfoChanged()
        }

        binding.patientAddPhoneEnd.doAfterTextChanged {
            if (binding.patientAddPhoneEnd.text.length == 4) {
                hideKeyboard()
            } else if (binding.patientAddPhoneEnd.text.isEmpty()) {
                if (binding.patientAddPhoneMid.text.isEmpty()) {
                    binding.patientAddPhoneStart.requestFocus()
                } else {
                    binding.patientAddPhoneMid.requestFocus()
                }
            }
            onPatientInfoChanged()
        }

        binding.patientAddPhoneEnd.setOnEditorActionListener(
            TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    hideKeyboard()
                    return@OnEditorActionListener true
                }
                false
            }
        )

        binding.patientAddZipcode.doAfterTextChanged {
            onPatientInfoChanged()
        }
    }

    private fun addListeners() {
        binding.patientAddGender.setOnClickListener {
            it.hideKeyboard()
            val selectedIndex = if (selectedGender != null) genders.indexOf(selectedGender) else -1
            val bottomDialog = BottomDialog.newInstance(
                context.getString(R.string.gender),
                genders.map { it.value },
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                selectedGender = genders[index]
                binding.patientAddGender.text = selectedGender?.value
                onPatientInfoChanged()
            }
            bottomDialog.show(
                (getActivity() as FragmentActivity).supportFragmentManager,
                "genderBottomDialog"
            )
        }

        binding.patientAddState.setOnClickListener {
            it.hideKeyboard()
            val state = selectedState
            val selectedIndex = if (state != null) states.indexOf(state) else -1
            val bottomDialog = BottomDialog.newInstance(
                context.getString(R.string.patient_select_state),
                states,
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                selectedState = states[index]
                binding.patientAddState.text = selectedState
                onPatientInfoChanged()
            }
            bottomDialog.show(
                (getActivity() as FragmentActivity).supportFragmentManager,
                "stateBottomDialog"
            )
        }
    }

    fun isValid(): Boolean {
        return (binding.patientAddFirstName.isGone || binding.patientAddFirstName.text.isNotBlank()) &&
            (binding.patientAddLastName.isGone || binding.patientAddLastName.text.isNotBlank()) &&
            (binding.patientAddDateOfBirth.isGone || binding.patientAddDateOfBirth.isValid()) &&
            (binding.patientAddGender.isGone || binding.patientAddGender.text.isNotBlank()) &&
            (binding.patientAddPhoneStart.isGone || binding.patientAddPhoneStart.text.trim().length == 3) &&
            (binding.patientAddPhoneMid.isGone || binding.patientAddPhoneMid.text.trim().length == 3) &&
            (binding.patientAddPhoneEnd.isGone || binding.patientAddPhoneEnd.text.trim().length == 4) &&
            (
                binding.patientAddZipcode.isGone || binding.patientAddZipcode.text.trim()
                    .isEmpty() ||
                    binding.patientAddZipcode.text.trim().length == 5
            )
    }

    fun updatePatientInfo(patient: PatientPostBody.NewPatient) {
        binding.patientAddFirstName.setText(patient.firstName)
        binding.patientAddLastName.setText(patient.lastName)
        binding.patientAddDateOfBirth.setDob(patient.dob)

        patient.phoneNumber.let {
            val phoneArray = it.split("-")
            if (phoneArray.size == 3) {
                binding.patientAddPhoneStart.setText(phoneArray[0])
                binding.patientAddPhoneMid.setText(phoneArray[1])
                binding.patientAddPhoneEnd.setText(phoneArray[2])
            }
        }

        patient.gender.let {
            selectedGender = if (it == 0) DriverLicense.Gender.MALE else DriverLicense.Gender.FEMALE
            binding.patientAddGender.text = selectedGender?.value
        }

        binding.patientAddAddress.setText(patient.address1)
        binding.patientAddAddress2.setText(patient.address2)
        binding.patientAddCity.setText(patient.city)

        patient.state?.let {
            selectedState = it
            binding.patientAddState.text = selectedState
        }

        binding.patientAddZipcode.setText(patient.zip)
        onPatientInfoChanged()
    }

    fun updatePatientInfo(patient: Patient, manualDob: LocalDate? = null) {
        binding.patientAddFirstName.setText(patient.firstName)
        binding.patientAddLastName.setText(patient.lastName)

        val dob = patient.getDobString()?.let {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("M/dd/yyyy"))
        } ?: manualDob
        binding.patientAddDateOfBirth.setDob(dob)

        patient.phoneNumber?.let {
            val phoneArray = it.split("-")
            if (phoneArray.size == 3) {
                binding.patientAddPhoneStart.setText(phoneArray[0])
                binding.patientAddPhoneMid.setText(phoneArray[1])
                binding.patientAddPhoneEnd.setText(phoneArray[2])
            }
        }

        patient.gender?.let {
            selectedGender = DriverLicense.Gender.fromString(it)
            binding.patientAddGender.text = selectedGender?.value
        }

        binding.patientAddAddress.setText(patient.address1)
        binding.patientAddAddress2.setText(patient.address2)
        binding.patientAddCity.setText(patient.city)

        patient.state?.let {
            selectedState = it
            binding.patientAddState.text = selectedState
        }

        binding.patientAddZipcode.setText(patient.zip)
        onPatientInfoChanged()
    }

    fun updatePatientInfo(driverLicense: DriverLicense) {
        // Reset all fields
        binding.patientAddFirstName.setText("")
        binding.patientAddLastName.setText("")
        binding.patientAddDateOfBirth.setDob(null)
        selectedGender = null
        binding.patientAddGender.text = ""
        binding.patientAddAddress.setText("")
        binding.patientAddAddress2.setText("")
        binding.patientAddCity.setText("")
        selectedState = null
        binding.patientAddState.text = ""
        binding.patientAddZipcode.setText("")

        // name
        driverLicense.firstName?.let {
            binding.patientAddFirstName.setText(it.trim())
        }
        driverLicense.lastName?.let {
            binding.patientAddLastName.setText(it.trim())
        }

        // birth
        driverLicense.birthDate?.let {
            binding.patientAddDateOfBirth.setDob(it)
        }

        // gender
        driverLicense.gender?.let {
            selectedGender = it
            binding.patientAddGender.text = selectedGender?.value
        }

        // address
        driverLicense.addressStreet?.let {
            binding.patientAddAddress.setText(it.trim())
        }

        // city
        driverLicense.addressCity?.let {
            binding.patientAddCity.setText(it.trim())
        }

        // state
        driverLicense.addressState?.let {
            selectedState = it.trim()
            binding.patientAddState.text = it.trim()
        }

        // zipcode
        driverLicense.addressZip?.let {
            binding.patientAddZipcode.setText(it.trim())
        }
        onPatientInfoChanged()
    }

    fun updatePatientInfo(patient: UpdatePatient) {
        binding.patientAddFirstName.setText(patient.firstName)
        binding.patientAddLastName.setText(patient.lastName)
        binding.patientAddDateOfBirth.setDob(patient.dob)

        patient.phoneNumber.let {
            val phoneArray = it?.split("-")
            if (phoneArray?.size == 3) {
                binding.patientAddPhoneStart.setText(phoneArray[0])
                binding.patientAddPhoneMid.setText(phoneArray[1])
                binding.patientAddPhoneEnd.setText(phoneArray[2])
            }
        }

        patient.gender.let {
            selectedGender = if (it == 0) DriverLicense.Gender.MALE else DriverLicense.Gender.FEMALE
            binding.patientAddGender.text = selectedGender?.value
        }

        binding.patientAddAddress.setText(patient.address1)
        binding.patientAddAddress2.setText(patient.address2)
        binding.patientAddCity.setText(patient.city)

        patient.state?.let {
            selectedState = it
            binding.patientAddState.text = selectedState
        }

        binding.patientAddZipcode.setText(patient.zip)
        onPatientInfoChanged()
    }

    fun updateSuggestParentPatient(patient: Patient?) {
        patient?.paymentInformation?.let { payment ->
            binding.patientAddFirstName.setText(payment.insuredFirstName)
            binding.patientAddLastName.setText(payment.insuredLastName)

            payment.insuredDob.let { dob ->
                binding.patientAddDateOfBirth.setDob(LocalDateTime.parse(dob).toLocalDate())
            }

            selectedGender = if (DriverLicense.Gender.MALE.value == payment.insuredGender) {
                DriverLicense.Gender.MALE
            } else {
                DriverLicense.Gender.FEMALE
            }
            selectedGender?.let { gender ->
                binding.patientAddGender.text = gender.value
            }

            val phoneArray = patient.phoneNumber?.split("-")
            if (phoneArray != null && phoneArray.size == 3) {
                binding.patientAddPhoneStart.setText(phoneArray[0])
                binding.patientAddPhoneMid.setText(phoneArray[1])
                binding.patientAddPhoneEnd.setText(phoneArray[2])
            }

            binding.patientAddAddress.setText(patient.address1)
            binding.patientAddAddress2.setText(patient.address2)
            binding.patientAddCity.setText(patient.city)

            selectedState = patient.state
            binding.patientAddState.text = selectedState
            binding.patientAddZipcode.setText(patient.zip)
        }
    }

    fun updateNewParentPatient(patient: Patient?) {
        val phoneArray = patient?.phoneNumber?.split("-")
        if (phoneArray != null && phoneArray.size == 3) {
            binding.patientAddPhoneStart.setText(phoneArray[0])
            binding.patientAddPhoneMid.setText(phoneArray[1])
            binding.patientAddPhoneEnd.setText(phoneArray[2])
        }
    }

    fun updateState(locationData: LocationData) {
        selectedState = locationData.state
        binding.patientAddState.text = selectedState
    }

    fun getPhoneNumber() =
        binding.patientAddPhoneStart.text.trim()
            .toString() + binding.patientAddPhoneMid.text.trim()
            .toString() + binding.patientAddPhoneEnd.text.trim().toString()

    fun getDoB(): LocalDate? = binding.patientAddDateOfBirth.getDob()
}
