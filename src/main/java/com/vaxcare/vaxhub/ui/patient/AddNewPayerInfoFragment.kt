/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.AndroidBug5497Workaround
import com.vaxcare.vaxhub.core.extension.hideKeyboard
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentAddNewPayerInfoBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.patient.PayerInfoUiData
import com.vaxcare.vaxhub.ui.navigation.AddPatientsDestination
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddPatientSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddNewPayerInfoFragment() : BaseFragment<FragmentAddNewPayerInfoBinding>() {
    private val addPatientSharedViewModel: AddPatientSharedViewModel by activityViewModels()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    private val screenTitle = "AddNewPayerInfo"

    @Inject
    lateinit var destination: AddPatientsDestination

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_add_new_payer_info,
        hasMenu = false,
        hasToolbar = false
    )

    private var viewAssist: AndroidBug5497Workaround? = null
    private var relationships = RelationshipToInsured.values()
    private var selectedRelationship = RelationshipToInsured.Self
    private val genders = DriverLicense.Gender.values()
    private var selectedGender: DriverLicense.Gender? = null

    override fun bindFragment(container: View): FragmentAddNewPayerInfoBinding =
        FragmentAddNewPayerInfoBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewAssist = AndroidBug5497Workaround.assistActivity(requireActivity())
        logScreenNavigation(screenTitle)

        addOnClickListeners()
        addOnFocusListeners()
    }

    override fun onResume() {
        super.onResume()

        populateData()
        toggleInsuredSectionVisibility()
    }

    override fun onDestroyView() {
        hideKeyboard()
        viewAssist?.stopAssistingActivity()
        super.onDestroyView()
    }

    private fun populateData() {
        val payerInfoUIData = addPatientSharedViewModel.extractPayerInfoUIData()
        val payer = addPatientSharedViewModel.selectedPayer

        binding?.apply {
            textViewPayerName.text = payer?.insuranceName
            roundedTextInputMemberId.setValue(payerInfoUIData.memberId)
            roundedTextInputGroupId.setValue(payerInfoUIData.groupId)
            selectedRelationship = payerInfoUIData.relationship
            textViewRelationship.text = selectedRelationship.toString()
            roundedTextInputInsuredFirstName.setValue(payerInfoUIData.insuredFirstName)
            roundedTextInputInsuredLastName.setValue(payerInfoUIData.insuredLastName)
            dateViewInsuredBirthDate.setDob(payerInfoUIData.insuredDob)
            selectedGender = payerInfoUIData.insuredGender
            selectedGender?.let {
                textViewInsuredGender.text = it.value
            }
        }
        filterRelationshipDropdown(payer)
    }

    private fun filterRelationshipDropdown(payer: Payer?) {
        val flags = payer?.extensionFlags?.split(",")?.toList() ?: listOf()
        val isSelfOnly = flags.any { it.trim() == "SelfRelationshipOnly" }
        if (isSelfOnly) {
            relationships = arrayOf(RelationshipToInsured.Self)
            selectedRelationship = RelationshipToInsured.Self
            binding?.textViewRelationship?.text = selectedRelationship.toString()
        }
    }

    private fun addOnFocusListeners() {
        val onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b) {
                (view as EditText).setSelection(view.text.length)
            }
        }
        binding?.apply {
            roundedTextInputMemberId.onFocusChangeListener = onFocusChangeListener
            roundedTextInputGroupId.onFocusChangeListener = onFocusChangeListener
            roundedTextInputInsuredFirstName.onFocusChangeListener = onFocusChangeListener
            roundedTextInputInsuredLastName.onFocusChangeListener = onFocusChangeListener
        }
    }

    private fun addOnClickListeners() {
        binding?.apply {
            textViewRelationship.setOnClickListener { view ->
                view.hideKeyboard()
                val selectedIndex = relationships.indexOf(selectedRelationship)

                val bottomDialog = BottomDialog.newInstance(
                    getString(R.string.relationship),
                    relationships.map { it.toString() },
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    selectedRelationship = relationships[index]
                    textViewRelationship.text = selectedRelationship.toString()
                    toggleInsuredSectionVisibility()
                }
                bottomDialog.show(
                    (activity as FragmentActivity).supportFragmentManager,
                    "relationshipBottomDialog"
                )
            }

            textViewInsuredGender.setOnClickListener { view ->
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
                    textViewInsuredGender.text = selectedGender?.value
                }
                bottomDialog.show(
                    (activity as FragmentActivity).supportFragmentManager,
                    "genderBottomDialog"
                )
            }

            fabNext.setOnClickListener {
                binding?.apply {
                    addPatientSharedViewModel.savePayerInfoUIData(
                        PayerInfoUiData(
                            memberId = roundedTextInputMemberId.inputValue,
                            groupId = roundedTextInputGroupId.inputValue,
                            relationship = selectedRelationship,
                            insuredFirstName = if (selectedRelationship == RelationshipToInsured.Self) {
                                null
                            } else {
                                roundedTextInputInsuredFirstName.inputValue
                            },
                            insuredLastName = if (selectedRelationship == RelationshipToInsured.Self) {
                                null
                            } else {
                                roundedTextInputInsuredLastName.inputValue
                            },
                            insuredDob = if (selectedRelationship == RelationshipToInsured.Self) {
                                null
                            } else {
                                dateViewInsuredBirthDate.getDob()
                            },
                            insuredGender = if (selectedRelationship == RelationshipToInsured.Self) {
                                null
                            } else {
                                selectedGender
                            }
                        )
                    )
                }
                destination.navigateToConfirmPatientInfoFromAddNewPayerInfo(this@AddNewPayerInfoFragment)
            }
        }
    }

    private fun toggleInsuredSectionVisibility() {
        binding?.apply {
            constraintLayoutInsuredContainer.visibility =
                if (selectedRelationship != RelationshipToInsured.Self) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }
}
