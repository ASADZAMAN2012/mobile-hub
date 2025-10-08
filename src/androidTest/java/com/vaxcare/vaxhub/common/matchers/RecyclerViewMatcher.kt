/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.matchers

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.arraySetOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.matchers.validators.PaymentSummaryFooterValidator
import com.vaxcare.vaxhub.common.matchers.validators.PaymentSummaryHeaderValidator
import com.vaxcare.vaxhub.common.matchers.validators.PaymentSummaryItemValidator
import com.vaxcare.vaxhub.data.PaymentModals
import com.vaxcare.vaxhub.data.TestBackground
import com.vaxcare.vaxhub.data.TestCards
import com.vaxcare.vaxhub.data.TestOrderDose
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.data.TestStockPill
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.adapter.PaymentSummaryItemAdapter
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineSummaryItemAdapter
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentListItemViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.BaseVaccineSummaryItemViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineItemOrderViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineItemProductViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineSummaryBottomViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineSummaryHeaderViewHolder
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import java.time.format.DateTimeFormatter
import java.util.Locale

fun withNameInAppointmentList(testPatient: TestPatients, index: Int): Matcher<in View> =
    AppointmentListMatcher(testPatient, index) as Matcher<View>

lateinit var clickItemAppointment: Appointment

class AppointmentListMatcher(
    private val testPatients: TestPatients,
    private val index: Int
) :
    TypeSafeMatcher<RecyclerView>() {
    override fun describeTo(description: Description) {
        description.appendText(
            "Some mismatch in Matcher found here - AppointmentListFragment: PatientLastName-${
                testPatients.lastName.plus(",")
            }, PatientFirstName-${testPatients.firstName}"
        )
    }

    override fun matchesSafely(recycler: RecyclerView): Boolean {
        val holder =
            recycler.findViewHolderForAdapterPosition(index) as? AppointmentListItemViewHolder
        return holder?.let {
            val lastNameString = holder.binding.patientLastName.text.toString()
            val firstNameString = holder.binding.patientFirstName.text.toString()
            val containLastName = lastNameString.equals(testPatients.lastName.plus(","), true)
            val containFirstName = firstNameString.equals(testPatients.firstName, true)
            if (containFirstName && containLastName) {
                clickItemAppointment =
                    (recycler.adapter as ListAdapter<*, *>).currentList[index] as Appointment
            }
            containFirstName && containLastName
        } ?: false
    }
}

class DoseAddedListMatcher(
    private val testProducts: List<TestProducts>,
    private val sites: TestSites
) : TypeSafeMatcher<RecyclerView>() {
    private var descriptionAppend =
        "Some mismatch in Matcher found here - CheckoutPatientFragment: DoseAdded-$testProducts, Site-$sites,"
    private val checkResultSet = mutableSetOf<Pair<Boolean, String>>()

    override fun describeTo(description: Description) {
        val finalDesc = checkResultSet.filter { !it.first }
            .joinToString(", ", postfix = ". ") { it.second } + descriptionAppend
        description.appendText(finalDesc)
    }

    override fun matchesSafely(recyclerView: RecyclerView): Boolean {
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        for (index in 0 until itemCount) {
            val itemType = recyclerView.adapter?.getItemViewType(index)
            val holder =
                recyclerView.findViewHolderForAdapterPosition(index) as VaccineItemProductViewHolder
            val checkoutVaccineTipsText = holder.binding.checkoutVaccineTips.text.toString()
            val checkoutVaccineNameText = holder.binding.checkoutVaccineName.text.toString()
            val checkoutVaccineLotNumberText =
                holder.binding.checkoutVaccineLotNumber.text.toString()
            val checkoutVaccineSiteText = holder.binding.checkoutVaccineSite.text.toString()
            val isTipTextMatch = checkoutVaccineTipsText == "Copay Required"
            val actualProduct = testProducts.firstOrNull {
                it.antigenProductName == checkoutVaccineNameText &&
                    it.lotNumber == checkoutVaccineLotNumberText
            }
            val productFound = actualProduct != null
            checkResultSet.add(
                productFound to "Product not found for $checkoutVaccineLotNumberText | $checkoutVaccineNameText"
            )
            val siteTextMatch = checkoutVaccineSiteText == sites.displayName
            if (actualProduct?.hasCopay == true) {
                checkResultSet.add(isTipTextMatch to "CoPay not found for $checkoutVaccineNameText")
            }
            checkResultSet.add(siteTextMatch to "$siteTextMatch not matching for $checkoutVaccineNameText")
            return checkResultSet.all { it.first }
        }
        return checkResultSet.all { it.first }
    }
}

class OrderedDoseAddedListMatcher(private val orderEntityList: List<TestOrderDose>) :
    TypeSafeMatcher<RecyclerView>() {
    companion object {
        private const val ORDER_TYPE = 2
        private val formatter = DateTimeFormatter.ofPattern("MM/dd/yy 'AT' HH:mma", Locale.US)
    }

    override fun describeTo(description: Description) {
        description.appendText(
            "Some mismatch in Matcher found here - CheckoutPatientFragment: OrderedDoseAdd-$orderEntityList"
        )
    }

    override fun matchesSafely(recyclerView: RecyclerView): Boolean {
        val checkResultSet = arraySetOf<Boolean>()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        var notOrderDoseItemCount = 0
        for (index in 0 until itemCount) {
            val itemType = recyclerView.adapter?.getItemViewType(index)
            if (itemType == ORDER_TYPE) {
                val holder =
                    recyclerView.findViewHolderForAdapterPosition(index) as VaccineItemOrderViewHolder
                val checkoutVaccineNameText = holder.binding.checkoutVaccineName.text.toString()
                val labelText = holder.binding.label.text.toString()
                val dateText = holder.binding.date.text.toString()
                val checkVaccineName =
                    checkoutVaccineNameText == orderEntityList[index - notOrderDoseItemCount].shortDescription
                val checkLabel = labelText == "ORDERED"
                val checkDate =
                    dateText == orderEntityList[index - notOrderDoseItemCount].orderDate.format(
                        formatter
                    )
                checkResultSet.add(checkVaccineName)
                checkResultSet.add(checkLabel)
                checkResultSet.add(checkDate)
            } else {
                notOrderDoseItemCount++
            }
        }
        return checkResultSet.size == 1 && checkResultSet.contains(true)
    }
}

class CheckSummaryListMatcher(
    private val testPartners: TestPartners,
    private val testPatients: TestPatients,
    private val testProducts: List<TestProducts>,
    private val testSites: TestSites,
    private val copayTotalValue: String?
) : TypeSafeMatcher<RecyclerView>() {
    private var descriptionAppend =
        "Some mismatch in Matcher found here - ${testPartners.partnerName}, " +
            "CheckoutSummaryFragment: Patient-$testPatients, Product-$testProducts, " +
            "Site-$testSites, CopayTotalValue-$copayTotalValue"
    private val checkResultSet = mutableSetOf<Pair<Boolean, String>>()

    override fun describeTo(description: Description) {
        val finalDesc = checkResultSet.filter { !it.first }
            .joinToString(", ", postfix = ". ") { it.second } + descriptionAppend
        description.appendText(finalDesc)
    }

    override fun matchesSafely(recyclerView: RecyclerView): Boolean {
        val itemCount = recyclerView.adapter?.itemCount
        for (index in 0 until itemCount!!) {
            when (recyclerView.adapter?.getItemViewType(index)) {
                VaccineSummaryItemAdapter.ViewHolderType.HEADER.ordinal -> {
                    val holder =
                        recyclerView.findViewHolderForAdapterPosition(index) as VaccineSummaryHeaderViewHolder
                    val patientNameText = holder.binding.patientName.text.toString()
                    val checkoutProviderText =
                        holder.binding.patientCheckoutProvider.text.toString()
                    val checkShotAdminText = holder.binding.patientCheckoutShotAdmin.text.toString()
                    checkResultSet.add(
                        patientNameText.equals(
                            testPatients.completePatientName,
                            true
                        ) to "$patientNameText does not match ${testPatients.completePatientName}"
                    )
                    checkResultSet.add(
                        checkoutProviderText.equals(
                            testPartners.partnerName,
                            true
                        ) to "$checkoutProviderText does not match ${testPartners.partnerName}"
                    )
                    checkResultSet.add(
                        checkShotAdminText.equals(
                            testPartners.partnerName,
                            true
                        ) to "$checkShotAdminText does not match ${testPartners.partnerName}"
                    )
                }

                VaccineSummaryItemAdapter.ViewHolderType.ITEM.ordinal -> {
                    val holder =
                        recyclerView.findViewHolderForAdapterPosition(index) as BaseVaccineSummaryItemViewHolder<*>
                    val vaccineNameTextView =
                        holder.itemView.findViewById<TextView>(R.id.checkout_vaccine_name)
                    val vaccineLotNumberTextView =
                        holder.itemView.findViewById<TextView>(R.id.checkout_vaccine_lot_number)
                    val vaccineSiteTextView =
                        holder.itemView.findViewById<TextView>(R.id.checkout_vaccine_site)
                    val vaccineNameText = vaccineNameTextView.text.toString()
                    val vaccineLotNumberText = vaccineLotNumberTextView.text.toString()
                    val vaccineSiteText = vaccineSiteTextView.text.toString()
                    val associatedProduct = testProducts.firstOrNull {
                        it.antigenProductName.equals(vaccineNameText, true) &&
                            it.lotNumber.equals(vaccineLotNumberText, true)
                    }
                    checkResultSet.add((associatedProduct != null) to "$vaccineLotNumberText not found")
                    checkResultSet.add(
                        vaccineSiteText.equals(
                            testSites.displayName,
                            true
                        ) to "$vaccineSiteText not found for ${testSites.displayName}"
                    )
                }

                else -> {
                    val holder =
                        recyclerView.findViewHolderForAdapterPosition(index) as VaccineSummaryBottomViewHolder
                    val copayValueText = holder.binding.checkoutVaccineCopayTotal.text.toString()
                    copayTotalValue.let {
                        checkResultSet.add(
                            copayValueText.equals(
                                "$$copayTotalValue",
                                true
                            ) to "$copayValueText does not match $copayTotalValue"
                        )
                    }
                }
            }
        }
        return checkResultSet.all { it.first }
    }
}

class CheckPaymentSummaryListMatcher(
    private val testPatients: TestPatients,
    private val testProducts: List<TestProducts>,
    private val copayValue: String,
    private val paymentModals: PaymentModals,
    private val cardInfo: TestCards? = null
) : TypeSafeMatcher<RecyclerView>() {
    private var descriptionAppend =
        "Some mismatch in Matcher found here - " +
            "PaymentSummaryFragment: Patient-$testPatients, Product-$testProducts, " +
            "CopayValue-$copayValue, PaymentModal-$paymentModals"
    private val checkResultSet = mutableSetOf<Pair<Boolean, String>>()

    override fun describeTo(description: Description) {
        val finalDesc = checkResultSet.filter { !it.first }
            .joinToString(", ", postfix = ". ") { it.second } + descriptionAppend
        description.appendText(finalDesc)
    }

    override fun matchesSafely(recyclerView: RecyclerView): Boolean {
        val itemCount = recyclerView.adapter?.itemCount
        val headerValidator = PaymentSummaryHeaderValidator(recyclerView)
        val itemValidator = PaymentSummaryItemValidator(recyclerView, testProducts, copayValue)
        val footerValidator = PaymentSummaryFooterValidator(recyclerView, copayValue, cardInfo)
        for (index in 0 until itemCount!!) {
            when (recyclerView.adapter?.getItemViewType(index)) {
                PaymentSummaryItemAdapter.ViewHolderType.HEADER.ordinal ->
                    checkResultSet.add(
                        headerValidator.validateAtIndex(
                            index,
                            testPatients
                        ) to "header not validated"
                    )

                PaymentSummaryItemAdapter.ViewHolderType.ITEM.ordinal ->
                    checkResultSet.add(
                        itemValidator.validateAtIndex(
                            index,
                            paymentModals
                        ) to "item not validated"
                    )

                else -> checkResultSet.add(
                    footerValidator.validateAtIndex(
                        index,
                        paymentModals
                    ) to "footer not validated"
                )
            }
        }
        return checkResultSet.all { it.first }
    }
}

class AppointmentEligibilityIconAndTagMatcher(
    private val eligibility: RiskIconConstant? = null,
    private val testStockPillList: List<TestStockPill> = emptyList(),
    private val testBackground: TestBackground = TestBackground.None
) : TypeSafeMatcher<ViewGroup>() {
    var mismatchDescription =
        "Some mismatch in Matcher found here - Appointment: Eligibility " +
            "Icon-$eligibility, Stock Pill-$testStockPillList, Background-$testBackground"

    override fun describeTo(description: Description) {
        description.appendText(mismatchDescription)
    }

    override fun matchesSafely(itemView: ViewGroup): Boolean {
        val checkResultSet = arraySetOf<Boolean>()
        val eligibilityView = itemView.findViewById<ImageView>(R.id.eligibility_icon)
        val inventoryTypeView = itemView.findViewById<TextView>(R.id.inventoryType)
        val medDTagView = itemView.findViewById<TextView>(R.id.med_d_tag)
        val orderCountView = itemView.findViewById<TextView>(R.id.ordersCount)
        val checkoutBg = itemView.findViewById<View>(R.id.checked_out_bg)
        if (eligibility != null) {
            checkResultSet.add(
                Matchers.allOf(
                    withDrawableWithTintColorByInstrumentation(eligibility),
                    isDisplayed()
                ).matches(eligibilityView)
            )
            if (checkResultSet.contains(false)) {
                mismatchDescription = "mismatch Eligibility Icon-$eligibility"
                return false
            }
        }

        testStockPillList.forEach { item ->
            when (item) {
                is TestStockPill.PrivateStockPill,
                TestStockPill.VFCStockPill,
                TestStockPill.ThreeOneSevenPill -> {
                    checkResultSet.add(
                        Matchers.allOf(
                            withDrawableWithTintColor(
                                item.backgroundDrawable,
                                item.tintColor
                            ),
                            isDisplayed()
                        ).matches(
                            inventoryTypeView
                        )
                    )
                }

                is TestStockPill.MedDTag -> {
                    checkResultSet.add(
                        Matchers.allOf(
                            withDrawableWithTintColor(
                                item.backgroundDrawable,
                                item.tintColor
                            ),
                            isDisplayed()
                        ).matches(
                            medDTagView
                        )
                    )
                }

                is TestStockPill.OrderedOneDose -> {
                    checkResultSet.add(
                        Matchers.allOf(
                            withDrawableWithTintColor(
                                item.backgroundDrawable,
                                item.tintColor
                            ),
                            withText(item.text),
                            isDisplayed()
                        ).matches(
                            orderCountView
                        )
                    )
                }
            }
        }

        if (testStockPillList.isNotEmpty() && checkResultSet.contains(false)) {
            mismatchDescription = "mismatch Stock Pill-$testStockPillList"
            return false
        }

        if (testBackground != TestBackground.None) {
            checkResultSet.add(
                Matchers.allOf(
                    withDrawableWithTintColor(
                        testBackground.backgroundRes,
                        testBackground.tintColor
                    ),
                    isDisplayed()
                ).matches(checkoutBg)
            )
            if (checkResultSet.contains(false)) {
                mismatchDescription = "mismatch Background-$testBackground"
                return false
            }
        }
        return checkResultSet.size == 1 && checkResultSet.contains(true)
    }
}

fun withNameCheckEligibilityHasBackInAppointmentList(testPatients: TestPatients, index: Int): Matcher<in View>? {
    return AppointmentListForEligibilityHasBackMatcher(
        testPatients,
        index
    ) as? Matcher<in View>
}

class AppointmentListForEligibilityHasBackMatcher(
    private val testPatient: TestPatients,
    private val index: Int
) : TypeSafeMatcher<RecyclerView>() {
    private val mismatchDescription =
        "Some mismatch in Matcher: AppointmentListForEligibilityHasBack: Patient-$testPatient"

    override fun describeTo(description: Description) {
        description.appendText(mismatchDescription)
    }

    override fun matchesSafely(recycler: RecyclerView): Boolean {
        val holder =
            recycler.findViewHolderForAdapterPosition(index) as? AppointmentListItemViewHolder
        return holder?.let {
            val lastNameString = holder.binding.patientLastName.text.toString()
            val firstNameString = holder.binding.patientFirstName.text.toString()
            val containLastName = lastNameString.equals(testPatient.lastName.plus(","), true)
            val containFirstName = firstNameString.equals(testPatient.firstName, true)
            containFirstName && containLastName && holder.binding.eligibilityIcon.isVisible
        } ?: false
    }
}
