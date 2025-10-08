/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.AndroidBug5497Workaround
import com.vaxcare.vaxhub.core.extension.getMainThread
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.data.dao.LotNumberDao
import com.vaxcare.vaxhub.databinding.FragmentLotLookupBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.metric.ConfirmEnteredLotNumberDialogClickMetric
import com.vaxcare.vaxhub.ui.checkout.adapter.LotNumberSearchAdapter
import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog.Options.CONFIRM
import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog.Options.GO_BACK
import com.vaxcare.vaxhub.ui.navigation.LotDestination
import com.vaxcare.vaxhub.viewmodel.LotLookupViewModel
import com.vaxcare.vaxhub.viewmodel.ProductViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LotLookupFragment : BaseFragment<FragmentLotLookupBinding>() {
    companion object {
        private const val EMPTY_STRING = ""
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: LotDestination

    @Inject
    lateinit var lotNumberDao: LotNumberDao

    private val viewModel: LotLookupViewModel by viewModels()

    private val screenTitle = "LotNumberSearch"
    private val args: LotLookupFragmentArgs by navArgs()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_lot_lookup,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentLotLookupBinding = FragmentLotLookupBinding.bind(container)

    private var addingLot: Boolean = false
    private val productViewModel: ProductViewModel by activityViewModels()
    private var viewAssist: AndroidBug5497Workaround? = null

    private val lotNumberSearchAdapter: LotNumberSearchAdapter by lazy {
        LotNumberSearchAdapter(mutableListOf(), args.productId)
    }

    private val searchFilter = InputFilter { source, _, _, _, _, _ ->
        if (source.matches("[A-Z0-9a-z]*".toRegex())) {
            source
        } else {
            ""
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        viewAssist = AndroidBug5497Workaround.assistActivity(requireActivity())
        binding?.lotSearchEt?.requestFocus()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding?.lotSearchEt, InputMethodManager.SHOW_IMPLICIT)

        binding?.topBar?.onCloseAction = {
            destination.goBack(this@LotLookupFragment)
        }

        lotNumberDao.getAllWithProduct().observe(viewLifecycleOwner) {
            lotNumberSearchAdapter.setLotLookupList(it.toMutableList())
        }

        binding?.lotSearchEt?.filters = arrayOf(searchFilter, InputFilter.AllCaps())

        binding?.lotResultsRv?.layoutManager = object : LinearLayoutManager(context) {
            override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                try {
                    super.onLayoutChildren(recycler, state)
                } catch (e: IndexOutOfBoundsException) {
                    Timber.e(e)
                }
            }
        }
        binding?.lotResultsRv?.adapter = lotNumberSearchAdapter
        binding?.lotResultsRv?.hide()

        lotNumberSearchAdapter.onItemClicked = {
            context?.getMainThread {
                destination.goBack(this@LotLookupFragment)

                productViewModel.lotNumberSelection.value = it
            }
        }

        lotNumberSearchAdapter.addLotClicked = {
            addingLot = true
            val lotNumber = binding?.lotSearchEt?.text.toString()
            viewModel.findLotNumberByName(lotNumber)

            viewModel.lotNumberWithProduct.observe(viewLifecycleOwner) { lotNUmberWithProduct ->
                if (lotNUmberWithProduct != null) {
                    onLotSelected(lotNUmberWithProduct)
                } else {
                    subscribeAndShowConfirmEnteredDialog(lotNumber)
                }
            }
        }

        binding?.lotSearchEt?.doOnTextChanged { s, _, _, _ ->
            binding?.clearSearch?.setImageResource(
                if (s.toString().isBlank()) R.drawable.ic_search else R.drawable.ic_close
            )
            if (s.toString().isNotBlank() && s.toString().length > 2 && !addingLot) {
                search(s.toString())
                binding?.lotResultsRv?.show()
            } else if (!addingLot) {
                binding?.lotResultsRv?.hide()
            } else if (s.toString().isBlank()) {
                addingLot = !addingLot
                binding?.lotResultsRv?.hide()
            }
        }

        binding?.clearSearch?.setOnClickListener {
            binding?.lotSearchEt?.setText(EMPTY_STRING)
        }
    }

    private fun subscribeAndShowConfirmEnteredDialog(enteredLotNumber: String) {
        setFragmentResultListener(ConfirmEnteredLotNumberDialog.CONFIRM_LOT_NUMBER_DIALOG_RESULT_KEY) { _, bundle ->
            when (bundle.getInt(ConfirmEnteredLotNumberDialog.OPTION_SELECTED_BUNDLE_KEY)) {
                CONFIRM.ordinal -> {
                    viewModel.saveMetric(
                        ConfirmEnteredLotNumberDialogClickMetric(
                            optionSelected = CONFIRM,
                            enteredLotNumber = enteredLotNumber,
                            patientVisitId = args.appointmentId?.toInt()
                        )
                    )

                    binding?.lotSearchEt?.setText(EMPTY_STRING)

                    destination.goToAddNewLot(
                        fragment = this@LotLookupFragment,
                        lotNumber = enteredLotNumber,
                        appointmentId = args.appointmentId?.toInt(),
                        relativeDoS = args.relativeDoS
                    )
                }

                GO_BACK.ordinal -> {
                    viewModel.saveMetric(
                        ConfirmEnteredLotNumberDialogClickMetric(
                            optionSelected = GO_BACK,
                            enteredLotNumber = enteredLotNumber,
                            patientVisitId = args.appointmentId?.toInt()
                        )
                    )
                }
            }
        }

        destination.goToConfirmEnteredLotNumberDialog(
            fragment = this,
            enteredLotNumber = enteredLotNumber
        )
    }

    private fun search(s: String?) {
        lotNumberSearchAdapter.search(s)
    }

    private fun onLotSelected(lotNumber: LotNumberWithProduct) {
        productViewModel.lotNumberSelection.value = lotNumber
        destination.goBack(this@LotLookupFragment)
    }

    override fun onDestroyView() {
        binding?.lotResultsRv?.adapter = null
        hideKeyboard()
        viewAssist?.stopAssistingActivity()
        super.onDestroyView()
    }
}
