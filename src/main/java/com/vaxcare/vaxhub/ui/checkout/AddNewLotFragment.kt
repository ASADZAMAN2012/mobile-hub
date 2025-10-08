/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.work.WorkManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ProductSelectionMetric
import com.vaxcare.core.report.model.checkout.RelativeDoS
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.makeLongToast
import com.vaxcare.vaxhub.core.extension.safeContext
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentLotAddNewBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.args.InsertLotNumbersJobArgs
import com.vaxcare.vaxhub.model.enums.LotNumberSources
import com.vaxcare.vaxhub.model.enums.ProductPresentation
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.ProductAndPackage
import com.vaxcare.vaxhub.ui.checkout.dialog.NotInCOVIDAssistDialog
import com.vaxcare.vaxhub.ui.navigation.LotDestination
import com.vaxcare.vaxhub.viewmodel.ProductViewModel
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@AndroidEntryPoint
class AddNewLotFragment : BaseFragment<FragmentLotAddNewBinding>() {
    private val screenTitle = "LotNumberCreation"
    private val args: AddNewLotFragmentArgs by navArgs()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: LotDestination
    private val productViewModel: ProductViewModel by activityViewModels()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_lot_add_new,
        hasToolbar = false
    )

    private var selectedAntigen = ""
    private var selectedProduct = ""
    private var selectedPresentation = ""

    private var selectedMonth: String? = null
    private var selectedDay: String? = null
    private var selectedYear: String? = null

    private var product: ProductAndPackage? = null

    private val formattedMonth: String?
        get() {
            val month = selectedMonth
            return if (month == null) {
                null
            } else {
                requireContext().formatString(
                    R.string.month_format,
                    month,
                    resources.getStringArray(R.array.array_month)[month.toInt() - 1]
                )
            }
        }

    override fun bindFragment(container: View): FragmentLotAddNewBinding = FragmentLotAddNewBinding.bind(container)

    /**
     * IF Partner-level “COVID Assist Opt-In” feature flag is OFF, AND partner is COVID, will display the COVID popup
     */
    private fun onProductVerify(antigen: String, vararg productVerifiedCallback: () -> Unit) {
        GlobalScope.safeLaunch {
            if (antigen.uppercase() == "COVID" &&
                locationViewModel.getFeatureFlagByName(FeatureFlagConstant.FeatureCovidAssist) == null
            ) {
                withContext(Dispatchers.Main) {
                    destination.goToToNotInCOVIDAssist(
                        this@AddNewLotFragment,
                        NotInCOVIDAssistDialog.CheckCOVIDBehavior.CREATE_PRODUCT.ordinal
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    productVerifiedCallback.forEach { it.invoke() }
                }
            }
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        binding?.lotHeader?.onCloseAction = { goBack() }
        binding?.lotHeader?.setSubTitle(args.lotNumber)

        binding?.buttonConfirm?.setOnClickListener { addLot() }

        productViewModel.getAllVaccines().observe(viewLifecycleOwner) { vaccines ->
            binding?.selectProduct?.isEnabled = false
            binding?.selectPresentation?.isEnabled = false
            binding?.selectProduct?.alpha = 0.3f
            binding?.selectPresentation?.alpha = 0.3f

            binding?.selectAntigen?.setOnClickListener {
                val antigens = HashSet(vaccines.map { it.antigen }).toList().sortedBy { it }
                val selectedIndex = antigens.indexOf(selectedAntigen)
                val bottomDialog = BottomDialog.newInstance(
                    requireContext().getString(R.string.select_antigen_placeholder)
                        .uppercase(),
                    antigens,
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    onProductVerify(
                        antigens[index],
                        {
                            selectedAntigen = antigens[index]
                            binding?.selectAntigen?.text = selectedAntigen
                            binding?.selectProduct?.text =
                                resources.getString(R.string.select_product_placeholder)
                            binding?.selectPresentation?.text =
                                resources.getString(R.string.select_presentation_placeholder)
                            selectedProduct = ""
                            selectedPresentation = ""
                            binding?.presentationIcon?.setImageDrawable(null)
                            binding?.selectProduct?.isEnabled = true
                            binding?.selectPresentation?.isEnabled = false
                            binding?.selectProduct?.alpha = 1f
                            binding?.selectPresentation?.alpha = 0.3f
                            product = null
                            checkCanEnableConfirm()
                        }
                    )
                }
                bottomDialog.show(
                    childFragmentManager,
                    "antigenBottomDialog"
                )
            }

            binding?.selectProduct?.setOnClickListener {
                val products =
                    HashSet(
                        vaccines.filter { it.antigen == selectedAntigen }
                            .map {
                                if (it.isFlu()) {
                                    it.displayName
                                } else {
                                    val (openIndex, closeIndex) =
                                        it.displayName.indexOf("(") + 1 to it.displayName.indexOf(")")
                                    if (openIndex == -1) {
                                        it.displayName
                                    } else {
                                        it.displayName.substring(openIndex, closeIndex)
                                    }
                                }
                            }
                    ).toList().sortedBy { it }

                val selectedIndex = products.indexOf(selectedProduct)
                val bottomDialog = BottomDialog.newInstance(
                    requireContext().getString(R.string.select_product_placeholder)
                        .uppercase(),
                    products,
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    selectedProduct = products[index]
                    binding?.selectProduct?.text = selectedProduct
                    binding?.selectPresentation?.text =
                        resources.getString(R.string.select_presentation_placeholder)
                    selectedPresentation = ""
                    binding?.presentationIcon?.setImageDrawable(null)
                    binding?.selectPresentation?.isEnabled = true
                    binding?.selectPresentation?.alpha = 1f
                    product = null
                    checkCanEnableConfirm()
                }
                bottomDialog.show(
                    childFragmentManager,
                    "productBottomDialog"
                )
            }

            binding?.selectPresentation?.setOnClickListener {
                val presentations = HashSet(
                    vaccines.filter {
                        it.antigen == selectedAntigen && it.displayName.contains(selectedProduct)
                    }.map {
                        it.presentation.longName
                    }
                ).toList()

                val selectedIndex = presentations.indexOf(selectedPresentation)
                val bottomDialog = BottomDialog.newInstance(
                    requireContext().getString(R.string.select_presentation_placeholder)
                        .uppercase(),
                    presentations,
                    selectedIndex,
                    presentations.map { presentation ->
                        ProductPresentation.values().find { it.longName == presentation }!!.icon
                    }
                )
                bottomDialog.onSelected = { index ->
                    selectedPresentation = presentations[index]
                    binding?.selectPresentation?.text = selectedPresentation

                    ProductPresentation.values().find { it.longName == selectedPresentation }?.let {
                        binding?.presentationIcon?.setImageResource(it.icon)
                    }

                    product = vaccines.find {
                        it.antigen == selectedAntigen && it.displayName.contains(selectedProduct) &&
                            it.presentation.longName == selectedPresentation
                    }
                    checkCanEnableConfirm()
                }
                bottomDialog.show(
                    childFragmentManager,
                    "presentationBottomDialog"
                )
            }

            binding?.expirationMonth?.setOnClickListener {
                val month = selectedMonth
                val year = selectedYear
                val monthList =
                    if (year != null && year.toInt() == LocalDate.now().year) {
                        (LocalDate.now().monthValue..12).map { it.toString() }
                    } else {
                        (1..12).map { it.toString() }
                    }
                val selectedIndex =
                    if (month != null) monthList.indexOf(month) else -1
                val monthSimplyArray = resources.getStringArray(R.array.array_month)
                val bottomDialog = BottomDialog.newInstance(
                    requireContext().getString(R.string.select_expiration_month_placeholder)
                        .uppercase(),
                    monthList.map { monthSimplyArray[it.toInt() - 1] },
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    selectedMonth = monthList[index]
                    binding?.expirationMonth?.text = formattedMonth
                    correctionDate()
                    checkCanEnableConfirm()
                    if (selectedDay == null) {
                        binding?.expirationDay?.performClick()
                    } else if (selectedYear == null) {
                        binding?.expirationDay?.performClick()
                    }
                }
                bottomDialog.show(
                    childFragmentManager,
                    "monthBottomDialog"
                )
            }

            binding?.expirationDay?.setOnClickListener {
                val month = selectedMonth
                val year = selectedYear
                val day = selectedDay
                val dayList =
                    if (year != null && month != null && day != null) {
                        val monthLength =
                            LocalDate.of(
                                year.toInt(),
                                month.toInt(),
                                day.toInt()
                            ).lengthOfMonth()

                        if (year.toInt() == LocalDate.now().year && month.toInt() == LocalDate.now().monthValue) {
                            (LocalDate.now().dayOfMonth + 1..monthLength).map { it.toString() }
                        } else {
                            (1..monthLength).map { it.toString() }
                        }
                    } else {
                        if (month != null) {
                            val now = LocalDate.now()
                            val monthLength = YearMonth
                                .of(year?.toInt() ?: now.year, month.toInt())
                                .lengthOfMonth()
                            (1..monthLength).map { it.toString() }
                        } else {
                            (1..31).map { it.toString() }
                        }
                    }
                val selectedIndex = if (day != null) dayList.indexOf(day) else -1
                val bottomDialog = BottomDialog.newInstance(
                    requireContext().getString(R.string.select_expiration_day_placeholder)
                        .uppercase(),
                    dayList,
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    selectedDay = dayList[index]
                    binding?.expirationDay?.text = selectedDay
                    correctionDate()
                    checkCanEnableConfirm()
                    if (selectedYear == null) {
                        binding?.expirationYear?.performClick()
                    }
                }
                bottomDialog.show(
                    childFragmentManager,
                    "dayBottomDialog"
                )
            }

            binding?.expirationYear?.setOnClickListener {
                val year = selectedYear
                val currentYear = LocalDate.now().year
                val yearList = (currentYear..(currentYear + 5)).map { it.toString() }
                val selectedIndex =
                    if (year != null) yearList.indexOf(year) else -1
                val bottomDialog = BottomDialog.newInstance(
                    requireContext().getString(R.string.select_expiration_year_placeholder)
                        .uppercase(),
                    yearList,
                    selectedIndex
                )
                bottomDialog.onSelected = { index ->
                    selectedYear = yearList[index]
                    binding?.expirationYear?.text = selectedYear
                    correctionDate()
                    checkCanEnableConfirm()
                }
                bottomDialog.show(
                    childFragmentManager,
                    "yearBottomDialog"
                )
            }
        }
    }

    private fun correctionDate() {
        val day = selectedDay
        val month = selectedMonth
        val year = selectedYear

        if (year != null && year.toInt() == LocalDate.now().year &&
            month != null && month.toInt() < LocalDate.now().monthValue
        ) {
            selectedMonth = LocalDate.now().monthValue.toString()
            binding?.expirationMonth?.text = formattedMonth
        }

        if (year != null && year.toInt() == LocalDate.now().year &&
            month != null && month.toInt() == LocalDate.now().monthValue &&
            day != null && day.toInt() <= LocalDate.now().dayOfMonth
        ) {
            selectedDay = (LocalDate.now().dayOfMonth + 1).toString()
            binding?.expirationDay?.text = selectedDay
        }

        // Prevent choosing illegal dates
        if (year != null &&
            month != null &&
            day != null &&
            day.toInt() > YearMonth.of(year.toInt(), month.toInt()).lengthOfMonth()
        ) {
            selectedDay = YearMonth.of(year.toInt(), month.toInt()).lengthOfMonth().toString()
            binding?.expirationDay?.text = selectedDay
        }
    }

    private fun addLot() {
        val expiration = LocalDate.of(
            requireNotNull(selectedYear).toInt(),
            requireNotNull(selectedMonth).toInt(),
            requireNotNull(selectedDay).toInt()
        )

        val product = product
        product?.let {
            val lotName = requireNotNull(args.lotNumber)
            if (expiration > LocalDate.now()) {
                Timber.d("Confirm $lotName ${product.id} $expiration")

                OneTimeWorker.buildOneTimeUniqueWorker(
                    wm = WorkManager.getInstance(requireContext()),
                    parameters = OneTimeParams.InsertLotNumbers(
                        InsertLotNumbersJobArgs(
                            lotNumberName = lotName,
                            epProductId = product.id,
                            expiration = expiration,
                            source = LotNumberSources.ManualEntry.id
                        )
                    )
                )

                GlobalScope.safeLaunch {
                    val mapping = productViewModel.findMapping(product.id)

                    val prodMetric = ProductSelectionMetric(
                        screenSource = "Add New Lot",
                        scannerType = "Manual",
                        productSource = "Lot Creation",
                        hubModel = "Mobile Hub",
                        salesProductId = mapping?.id ?: -1,
                        productName = product.displayName,
                        ndc = it.productNdc ?: "",
                        lotNumber = lotName,
                        expirationDate = expiration.toLocalDateString(),
                        relativeDoS = args.relativeDoS?.let { RelativeDoS.valueOf(it) },
                        patientVisitId = args.appointmentId?.toInt()
                    )

                    analytics.saveMetric(prodMetric)

                    safeContext(Dispatchers.Main) {
                        productViewModel.lotNumberSelection.value = LotNumberWithProduct(
                            expiration,
                            0,
                            lotName,
                            product.id,
                            -1,
                            mapping?.id ?: -1,
                            product.ageIndications,
                            product.cptCvxCodes,
                            product.toProduct(),
                            mapping?.dosesInSeries ?: 1
                        )
                    }
                }

                destination.goBackToCheckoutVaccine(this@AddNewLotFragment)
            } else {
                context?.makeLongToast(R.string.date_expired)
            }
        }
    }

    private fun goBack() {
        destination.goBack(this@AddNewLotFragment)
    }

    private fun checkCanEnableConfirm() {
        binding?.buttonConfirm?.isEnabled = product != null &&
            binding?.expirationMonth?.text?.isNotBlank() == true &&
            binding?.expirationDay?.text?.isNotBlank() == true &&
            binding?.expirationYear?.text?.isNotBlank() == true
    }
}
