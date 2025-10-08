/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.EntryPointHelper.lazyEntryPoint
import com.vaxcare.vaxhub.HiltEntryPointInterface
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.CHECKOUT_SUMMARY
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.COMPLETE_CHECKOUT
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.CREATE_PATIENT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.SELECT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.SELECT_DOSES
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.WAIT_TIME_FOR_LOAD
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.clickOnViewChild
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.swipeBottomSheetDialogToExpand
import com.vaxcare.vaxhub.common.matchers.AppointmentEligibilityIconAndTagMatcher
import com.vaxcare.vaxhub.common.matchers.CheckPaymentSummaryListMatcher
import com.vaxcare.vaxhub.common.matchers.CheckSummaryListMatcher
import com.vaxcare.vaxhub.common.matchers.DoseAddedListMatcher
import com.vaxcare.vaxhub.common.matchers.OrderedDoseAddedListMatcher
import com.vaxcare.vaxhub.common.matchers.withBackgroundTintForFloatingActionButton
import com.vaxcare.vaxhub.common.matchers.withDrawable
import com.vaxcare.vaxhub.common.matchers.withDrawableByInstrumentation
import com.vaxcare.vaxhub.common.matchers.withDrawableWithTintColor
import com.vaxcare.vaxhub.common.matchers.withDrawableWithTintColorByInstrumentation
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.common.matchers.withTextOR
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.data.PaymentModals
import com.vaxcare.vaxhub.data.TestBackground
import com.vaxcare.vaxhub.data.TestCards
import com.vaxcare.vaxhub.data.TestOrderDose
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.data.TestStockPill
import com.vaxcare.vaxhub.data.dao.OrderDao
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.MedDCheckResponse
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.model.order.OrderDto
import com.vaxcare.vaxhub.ui.checkout.extensions.vaccineSupplyColor
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentListItemViewHolder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.Locale

@EntryPoint
@InstallIn(ActivityComponent::class)
interface CheckOutUtilEntryPoint : HiltEntryPointInterface {
    fun orderDao(): OrderDao
}

class CheckOutUtil : TestUtilBase() {
    companion object {
        const val MED_D_ZOSTER: String = "Zoster"
        const val MED_D_TDAP: String = "Tdap"
        const val MAX_CREATE_LOAD_TIME = 30
        const val SCAN_SCREEN_TITLE = "Scan Doses"
    }

    private val context by lazy { ApplicationProvider.getApplicationContext<Context>() }
    private val productUtil = ProductUtil()
    private val patientUtil = PatientUtil()
    private val appointmentListUtil = AppointmentListUtil()
    private val homeScreenUtil = HomeScreenUtil()

    /**
     *  stages for which we can stop a checkout during execution
     */
    enum class CheckoutFlowScreens {
        CREATE_PATIENT_APPOINTMENT,
        SELECT_APPOINTMENT,
        SELECT_DOSES,
        CHECKOUT_SUMMARY,
        COMPLETE_CHECKOUT
    }

    /**
     * Function to create and completely check out a curbside patient with option to stop early
     *
     * @param testPartner curbside test partner to use
     * @param testPatient test patient to create appointment for
     * @param stopAt option to stop the checkout process early
     */
    fun createAndCheckOutCurbsidePatientWithStopAt(
        testPartner: TestPartners = TestPartners.RprdCovidPartner,
        testPatient: TestPatients = TestPatients.RiskFreePatientForCheckout(),
        doseActions: Array<DoseAction> = arrayOf(DoseAction.AddDose(TestProducts.Varicella)),
        stopAt: CheckoutFlowScreens? = COMPLETE_CHECKOUT
    ) {
        when (stopAt) {
            // Curbside appointment creation will open the appointment unless MedD prompt stops it
            CREATE_PATIENT_APPOINTMENT -> {
                pinInAndCreateCurbsidePatientAppointment(testPartner, testPatient)
            }
            SELECT_APPOINTMENT -> {
                pinInAndCreateCurbsidePatientAppointment(testPartner, testPatient)
                verifyToCheckoutPatientFragmentScreen()
            }
            SELECT_DOSES -> {
                pinInAndCreateCurbsidePatientAppointment(testPartner, testPatient)
                verifyToCheckoutPatientFragmentScreen()
                performDoseActions(*doseActions)
            }
            CHECKOUT_SUMMARY -> {
                pinInAndCreateCurbsidePatientAppointment(testPartner, testPatient)
                verifyToCheckoutPatientFragmentScreen()
                performDoseActions(*doseActions)
                val uniqueProducts = (doseActions.map { it.testProduct }).toSet().toList()
                continueToCheckoutSummary(testPatient, uniqueProducts)
            }
            COMPLETE_CHECKOUT -> {
                pinInAndCreateCurbsidePatientAppointment(testPartner, testPatient)
                verifyToCheckoutPatientFragmentScreen()
                performDoseActions(*doseActions)
                val uniqueProducts = (doseActions.map { it.testProduct }).toSet().toList()
                continueToCheckoutSummary(testPatient, uniqueProducts)
                continueToCheckoutComplete(testPatient)
            }
            else -> Unit
        }
    }

    /**
     * Function to create and completely check out a patient with option to stop early
     *
     * @param testPartner test partner to use
     * @param testPatient test patient to create appointment for
     * @param stopAt option to stop the checkout process early
     */
    fun createAndCheckOutVC3PatientWithStopAt(
        testPartner: TestPartners = TestPartners.RprdCovidPartner,
        testPatient: TestPatients = TestPatients.QaRobotPatient(),
        doseActions: Array<DoseAction> = arrayOf(DoseAction.AddDose(TestProducts.Varicella)),
        stopAt: CheckoutFlowScreens? = COMPLETE_CHECKOUT
    ): String {
        return when (stopAt) {
            CREATE_PATIENT_APPOINTMENT -> {
                pinInAndCreatePatientAppointment(testPartner, testPatient)
            }
            SELECT_APPOINTMENT -> {
                val appointmentId = pinInAndCreatePatientAppointment(testPartner, testPatient)
                awaitEligibilityAndOpenAppointment(testPatient)
                appointmentId
            }
            SELECT_DOSES -> {
                val appointmentId = pinInAndCreatePatientAppointment(testPartner, testPatient)
                awaitEligibilityAndOpenAppointment(testPatient)
                performDoseActions(*doseActions)
                appointmentId
            }
            CHECKOUT_SUMMARY -> {
                val appointmentId = pinInAndCreatePatientAppointment(testPartner, testPatient)
                awaitEligibilityAndOpenAppointment(testPatient)
                performDoseActions(*doseActions)
                val uniqueProducts = (doseActions.map { it.testProduct }).toSet().toList()
                continueToCheckoutSummary(testPatient, uniqueProducts)
                appointmentId
            }
            COMPLETE_CHECKOUT -> {
                val appointmentId = pinInAndCreatePatientAppointment(testPartner, testPatient)
                awaitEligibilityAndOpenAppointment(testPatient)
                performDoseActions(*doseActions)
                val uniqueProducts = (doseActions.map { it.testProduct }).toSet().toList()
                continueToCheckoutSummary(testPatient, uniqueProducts)
                continueToCheckoutComplete(testPatient)
                appointmentId
            }
            else -> "-1"
        }
    }

    /**
     * Pins user in and goes through the flow of creating a new appointment for curbside
     *
     * @param testPartner
     * @param testPatient
     */
    private fun pinInAndCreateCurbsidePatientAppointment(
        testPartner: TestPartners = TestPartners.RprdCovidPartner,
        testPatient: TestPatients = TestPatients.RiskFreePatientForCheckout(),
    ) {
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        patientUtil.createTestPatient(testPatient)
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.tapPurplePlusIconToAdd()
        patientUtil.searchAndSelectRiskFreePatient(testPatient)
        patientUtil.verifyAndClickToConfirmPatientInfoToCreate(testPatient)
        patientUtil.verifyCreatePatientScreenLoading(MAX_CREATE_LOAD_TIME)
    }

    /**
     * Pins user in and goes through the flow of creating a new appointment
     *
     * @param testPartner
     * @param testPatient
     */
    fun createAndSearchPatientAppointment(
        testPartner: TestPartners = TestPartners.RprdCovidPartner,
        testPatient: TestPatients = TestPatients.RiskFreePatientForCheckout(),
    ) {
        patientUtil.createTestPatient(testPatient)
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.tapPurplePlusIconToAdd()
        patientUtil.searchAndSelectRiskFreePatient(testPatient)
        patientUtil.verifyAndClickToConfirmPatientInfoToCreate(testPatient)
        patientUtil.verifyCreatePatientScreenLoading(MAX_CREATE_LOAD_TIME)
    }

    /**
     * Pins user in and kicks off an api call to create a new appointment
     *
     * @param testPartner
     * @param testPatient
     */
    private fun pinInAndCreatePatientAppointment(
        testPartner: TestPartners = TestPartners.RprdCovidPartner,
        testPatient: TestPatients = TestPatients.QaRobotPatient()
    ): String {
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        appointmentListUtil.waitAppointmentLoadingFinish()
        return patientUtil.getAppointmentIdByCreateTestPatient(testPatient, 0)
    }

    /**
     * Reload the appointment list until we get eligibility back for our appointment, then open the appointment
     *
     * @param testPatient
     * @param maxRefreshEligibilityWait
     */
    fun awaitEligibilityAndOpenAppointment(
        testPatient: TestPatients = TestPatients.QaRobotPatient(),
        maxRefreshEligibilityWait: Int = 3
    ) {
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(
            testPatient,
            maxRefreshEligibilityWait
        )
        appointmentListUtil.tapLastElementInAppointmentListByPatientName(testPatient)
        verifyToCheckoutPatientFragmentScreen()
    }

    /**
     * Navigate to checkout summary from dose screen and verify basic screen information
     *
     * @param testPatient
     */
    fun continueToCheckoutSummary(
        testPatient: TestPatients = TestPatients.QaRobotPatient(),
        testProducts: List<TestProducts> = listOf(TestProducts.Varicella)
    ) {
        tapArrowToCheckoutSummary()
        IntegrationUtil.waitForOperationComplete()
        verifyCheckoutSummaryScreen(
            testPatient = testPatient,
            testProducts = testProducts
        )
    }

    /**
     * Navigate to checkout complete from checkout summary and verify basic screen information
     *
     * @param testPatient
     */
    fun continueToCheckoutComplete(testPatient: TestPatients = TestPatients.QaRobotPatient()) {
        tapCheckOutButton()
        //verifyCheckoutCompleteScreen(testPatient)
    }

    /**
     * Verify Checkout Patient Fragment screen.
     */
    fun verifyToCheckoutFragmentScreenByTitle() {
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(SCAN_SCREEN_TITLE)
    }

    /**
     * Verify Checkout Patient Fragment screen.
     */
    fun verifyToCheckoutPatientFragmentScreen(
        toolbarTitleText: String = SCAN_SCREEN_TITLE,
        testProduct: TestProducts? = null
    ) {
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "Loading",
            WAIT_TIME_FOR_LOAD
        )
        if (testProduct != null) {
            IntegrationUtil.waitForElementInRecyclerView(
                withId(R.id.rv_vaccines),
                "Vaccine List",
                allOf(
                    withId(R.id.checkout_vaccine_name),
                    withText(testProduct.antigenProductName)
                )
            )
            IntegrationUtil.verifyElementsPresentOnPage(
                IntegrationUtil.WAIT_TIME_DEFAULT,
                allOf(withId(R.id.checkout_vaccine_name), withText(testProduct.antigenProductName))
            )
        }
    }

    /**
     * Action to perform on a dose in the checkout product grid
     */
    sealed class DoseAction {
        open val testProduct: TestProducts = TestProducts.Varicella

        /**
         * Edit an existing dose's site
         */
        data class EditDose(
            override val testProduct: TestProducts = TestProducts.Varicella,
            val site: TestSites = TestSites.LeftArm
        ) : DoseAction()

        /**
         * Add a new dose to the checkout
         */
        data class AddDose(
            override val testProduct: TestProducts = TestProducts.Varicella
        ) : DoseAction()

        /**
         * Remove an existing dose from the checkout
         */
        data class RemoveDose(
            override val testProduct: TestProducts = TestProducts.Varicella
        ) : DoseAction()
    }

    /**
     * perform the incoming list of dose actions
     *
     * @return the list of lot numbers of the added doses
     */
    fun performDoseActions(vararg doseAction: DoseAction) {
        doseAction.forEach { action ->
            when (action) {
                is DoseAction.AddDose -> {
                    searchForLotNumberUsingMagnifyingGlassOnViewPort(action.testProduct)
                }
                is DoseAction.RemoveDose -> deleteDose(action.testProduct)
                is DoseAction.EditDose -> {
                    selectSiteForCheckout(action.site)
                    action.testProduct.lotNumber
                }
            }
        }
    }

    /**
     * Click on magnifying glass on view port and select a vaccine
     *
     * @param testProduct Test Product used in test
     */
    private fun searchForLotNumberUsingMagnifyingGlassOnViewPort(testProduct: TestProducts): String {
        tapPurpleMagnifierIconForSelectProduct()
        selectLotNumber(testProduct)
        return testProduct.lotNumber
    }

    /**
     * Delete a selected vaccine by clicking the delete button
     *
     * @param testProduct Test Product used in test
     */
    private fun deleteDose(testProduct: TestProducts): String {
        scrollVaccinationListAndSwipeItem(testProduct.lotNumber)
//        swipeLeftOnRecyclerViewItem(testProduct.lotNumber)
//        tapDeleteButtonToDeleteDose()
        return testProduct.lotNumber
    }

    private fun tapPurpleMagnifierIconForSelectProduct() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.patient_checkout_lot_search_btn)),
            "PurpleMagnifierIcon",
            IntegrationUtil.WAIT_TIME_FOR_INIT_SCANNER
        )
    }

    private fun selectLotNumber(testProduct: TestProducts) {
        val toolbarTitleText = context.resources.getString(R.string.lot_look_up)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        // Enter test Product
        IntegrationUtil.typeText(
            onView(withId(R.id.lot_search_et)),
            testProduct.lotNumber
        )
        IntegrationUtil.waitForElementInRecyclerView(
            withId(R.id.lot_results_rv),
            "SearchProductResult",
            allOf(
                withId(R.id.lot_row_lot_tv),
                withText(testProduct.lotNumber)
            )
        )
        // Select the product
        IntegrationUtil.scrollToElementInRecyclerViewAndClick(
            withId(R.id.lot_results_rv),
            "Search Products Grid",
            allOf(
                withId(R.id.lot_row_lot_tv),
                withText(testProduct.lotNumber)
            ),
            "product",
            R.id.lot_row_pointless_btn
        )
    }

    /**
     * Click on 'Set Site' hyperlink and select administration site
     *
     * @param site Site where dose needs to be administered
     */
    private fun selectSiteForCheckout(site: TestSites) {
        // Click on 'Set Site' hyperlink
        if (!IntegrationUtil.waitForElementToDisplayed(onView(withText("Set Site")), "Set Site")) {
            IntegrationUtil.simpleClick(onView(withText("Set Site")))
        } else {
            IntegrationUtil.simpleClick(onView(withId(R.id.checkout_vaccine_site)))
        }

        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(Matchers.containsString(site.displayName)),
            isDialog()
        )
    }

    /**
     * Tap arrow to Checkout Summary
     */
    fun tapArrowToCheckoutSummary() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.fab_next)),
            "ArrowButton"
        )
        IntegrationUtil.simpleClick(onView(withId(R.id.fab_next)), "ArrowButton")
    }

    /**
     * Verify summary screen
     * 1. Appt time
     * 2. Patient name
     * 3. Risk icon
     * 4. Patient ID
     * 5. DOB
     * 6. Physician
     * 7. Administered by
     * 8. Product info and lot #
     * @param testPatient Test Patient used in test
     * @param testProducts Test Product used in test
     */
    private fun verifyCheckoutSummaryScreen(testPatient: TestPatients, testProducts: List<TestProducts>) {
        val toolbarTitleText = context.resources.getString(R.string.summary)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.appt_time),
                not(withText(""))
            ),
            allOf(
                allOf(
                    withId(R.id.patient_name),
                    withTextIgnoringCase(testPatient.completePatientName)
                )
            ),
            allOf(
                withText("ID"),
                hasSibling(
                    allOf(
                        withId(R.id.patient_id),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withText("DOB"),
                hasSibling(
                    allOf(
                        withId(R.id.patient_dob),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withText("PHYSICIAN"),
                hasSibling(
                    allOf(
                        withId(R.id.patient_checkout_provider),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withText("ADMINISTERED BY"),
                hasSibling(
                    allOf(
                        withId(R.id.patient_checkout_shot_admin),
                        not(withText(""))
                    )
                )
            )
        )

        testProducts.forEach { testProduct ->
            IntegrationUtil.verifyElementsPresentOnPage(
                2,
                allOf(
                    withId(R.id.checkout_vaccine_icon),
                    hasSibling(withText(testProduct.antigenProductName))
                ),
                allOf(
                    withId(R.id.checkout_vis_date_label),
                    withText("V.I.S DATE"),
                    hasSibling(
                        allOf(
                            withId(R.id.checkout_vis_date),
                            not(withText(""))
                        )
                    )
                ),
                allOf(
                    withId(R.id.checkout_age_indication_label),
                    withText("AGE IND."),
                    hasSibling(
                        allOf(
                            withId(R.id.checkout_age_indication),
                            withText("YES")
                        )
                    )
                ),
                allOf(
                    withId(R.id.checkout_vaccine_lot_number),
                    withText(testProduct.lotNumber)
                )
            )
        }
    }

    /**
     * Check Out a patient after selecting dose
     */
    fun tapCheckOutButton() {
        IntegrationUtil.clickOnNotFullyVisibleElement(
            onView(withId(R.id.checkout_btn)),
            "CheckOutButton"
        )
    }

    /**
     * Verify Checkout Complete screen
     * 1. Patient name
     * 2. Risk icon
     * 3. Patient ID
     * 4. DOB
     * 5. # of shots administered
     * @param testPatient Test Patient used in test
     */
    fun verifyCheckoutCompleteScreen(testPatient: TestPatients) {
        val titleText = context.resources.getString(R.string.patient_checkout_complete)
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.checkout_complete_title),
                withText(titleText)
            )
        )

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                allOf(
                    withId(R.id.checkout_complete_patient_name),
                    withTextIgnoringCase(testPatient.completePatientName)
                )
            ),
            allOf(
                withText("ID"),
                hasSibling(
                    allOf(
                        withId(R.id.patient_id),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withText("DOB"),
                hasSibling(
                    allOf(
                        withId(R.id.patient_dob),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withId(R.id.patient_shot_administered),
                withSubstring("Administered")
            )
        )
    }

    /**
     * Tap “Logout” to Home Screen
     */
    fun tapLogoutButton() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.checkout_log_out)),
            "Logout"
        )
    }

    /**
     * Scroll Appointment list to specific patient and click the item
     * @param patientFullName Test patient name used in test
     */
    fun scrollAppointmentListAndClickItem(patientFullName: String) {
        IntegrationUtil.waitForElementInRecyclerView(
            withId(R.id.recycler_view),
            "Appointment List",
            allOf(
                withId(R.id.patient_name),
                withTextIgnoringCase(patientFullName)
            )
        )

        onView(withId(R.id.recycler_view)).perform(
            RecyclerViewActions.scrollToHolder(
                IntegrationUtil.matchNameInAppointmentList(
                    patientFullName
                )
            )
        )
        onView(withId(R.id.recycler_view)).perform(
            RecyclerViewActions.actionOnHolderItem(
                IntegrationUtil.matchNameInAppointmentList(patientFullName),
                click()
            )
        )
    }

    /**
     * Scroll vaccination list to specific lot number and swipe the item
     * @param lotNumber lot number to match
     */
    fun scrollVaccinationListAndSwipeItem(lotNumber: String) {
        onView(withId(R.id.rv_vaccines)).perform(
            RecyclerViewActions.scrollToHolder(
                IntegrationUtil.matchNameInVaccinationList(
                    lotNumber
                )
            )
        )
        onView(withId(R.id.rv_vaccines)).perform(
            RecyclerViewActions.actionOnHolderItem(
                IntegrationUtil.matchNameInVaccinationList(lotNumber),
                clickOnViewChild(R.id.button_delete)
            )
        )
    }

    fun swipeLeftOnRecyclerViewItem(lotNumber: String) {
        onView(ViewMatchers.withId(R.id.rv_vaccines))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeLeft()))
    }

    /**
     * Verify the added vaccination along with the previous vaccination
     * @param testProducts vaccination to verify
     */
    fun verifySpecialVaccination(testProducts: TestProducts) {
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.checkout_vaccine_lot_number),
                withText(testProducts.lotNumber)
            )
        )
    }

    /**
     * Verify 2 Shots Administered
     */
    fun verifyShotsAdministered() {
        IntegrationUtil.waitForElementToDisplayed(
            onView(withId(R.id.checkout_complete_title)),
            "Checkout Complete screen"
        )

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.patient_shot_administered),
                withText("2 Shots Administered")
            )
        )
    }

    /**
     * Sees “Med D Check Available”
     * Select “Yes, Review Copay” or “No, Skip This”
     * Sees “Med D Check Available” and Select “Yes, Review Copay” or “No, Skip This”
     *
     * @param selection “Yes, Review Copay” or “No, Skip This”
     */
    fun promptMedDCheckAvailableDialogToSelect(selection: String, userFriendlyName: String = "Med D Check Available") {
        IntegrationUtil.waitUIWithDelayed()
        val medDCheckAvailableText = context.resources.getString(R.string.med_d_check_dialog_1242)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(medDCheckAvailableText)).inRoot(isDialog()),
            userFriendlyName,
            5
        )

        IntegrationUtil.delayedClick(withText(selection))
    }

    /**
     * Verify text “Requires a Copay” modal is displayed and Tap “Review Copay”
     *
     * @param selection “Review Copay”
     */
    fun promptReviewCopayDialogToSelect(selection: String) {
        IntegrationUtil.waitUIWithDelayed()
        val medDCheckAvailableText =
            context.resources.getString(R.string.med_d_review_copay_dialog_header)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(medDCheckAvailableText)).inRoot(isDialog()),
            "Requires a Copay",
            5
        )

        IntegrationUtil.delayedClick(withText(selection))
    }

    /**
     * Verify Med D eligibility check runs
     */
    fun verifyToMedDCheckFragmentScreen() {
        val toolbarTitleText = context.resources.getString(R.string.med_d_copay_check)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
    }

    /**
     * If Social Security Number is displayed to run Med D Copay, will check Social Security Number
     */
    fun checkSocialSecurityNumberIfNeed() {
        val checkSSNText = context.resources.getString(R.string.med_d_check_prompt)
        val isShowCheckSSN = IntegrationUtil.isElementToViewMatcher(
            onView(withText(checkSSNText)),
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
        if (isShowCheckSSN) {
            verifyToMedDCheckFragmentScreen()
            IntegrationUtil.typeTextWithCloseSoftKeyboard(
                onView(withId(R.id.ssn_start)),
                "123"
            )
            IntegrationUtil.waitUIWithDelayed(1000)
            IntegrationUtil.typeTextWithCloseSoftKeyboard(
                onView(withId(R.id.ssn_mid)),
                "12"
            )
            IntegrationUtil.waitUIWithDelayed(1000)
            IntegrationUtil.typeTextWithCloseSoftKeyboard(
                onView(withId(R.id.ssn_end)),
                "1234"
            )

            IntegrationUtil.waitForElementToEnabled(
                onView(withId(R.id.fab_next)),
                secondsToWait = WAIT_TIME_FOR_LOAD
            )
            IntegrationUtil.delayedClick(withId(R.id.fab_next))
        }
    }

    /**
     * Verify copays for Zoster and/or TDap are displayed, tap arrow
     */
    fun verifyCopayDataDisplayedAndTapArrow(hasTapNextBtn: Boolean = true, appointmentId: String = "") {
        checkSocialSecurityNumberIfNeed()
        IntegrationUtil.waitForElementToEnabled(
            onView(withId(R.id.fab_next)),
            secondsToWait = WAIT_TIME_FOR_LOAD
        )
        // Verify title- Med D Copay Requirements
        val requirementsText = context.resources.getString(R.string.med_d_check_checked_prompt)
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.results_header_text),
                withText(requirementsText)
            )
        )

        if (appointmentId.isNotBlank()) {
//            val copays = productUtil.retrieveMedDCopaysByAppointmentId(appointmentId)
//            assert(!copays.isNullOrEmpty()) {
//                "Expected to verify both Tdap/Zoster are displayed with respective icons and Copay amount."
//            }
            // Verify both Tdap/Zoster are displayed with respective icons and Copay amount
//            copays?.forEach { copay ->
//                val copayName = if (copay.productName == null) {
//                    copay.antigen.value
//                } else {
//                    "${copay.antigen.value} (${copay.productName})"
//                }
//                val copayAmount = context.resources.getString(
//                    R.string.med_d_check_copay,
//                    copay.copay.setScale(2, RoundingMode.HALF_UP).toString()
//                )
//                IntegrationUtil.verifyElementsPresentOnPage(
//                    2,
//                    allOf(
//                        allOf(
//                            withId(R.id.med_d_vaccine_name),
//                            withTextIgnoringCase(copayName)
//                        ),
//                        hasSibling(
//                            allOf(
//                                withId(R.id.med_d_vaccine_price),
//                                withTextIgnoringCase(copayAmount)
//                            )
//                        ),
//                        hasSibling(
//                            allOf(
//                                withId(R.id.med_d_vaccine_icon),
//                                withDrawable(R.drawable.ic_presentation_prefilled_syringe)
//                            )
//                        )
//                    )
//                )
//            }
        } else {
            // Verify both Tdap/Zoster are displayed with respective icons and Copay amount
            IntegrationUtil.verifyAndScrollToElementInRecyclerView(
                withId(R.id.rv_med_d_results),
                "Copays grid",
                allOf(
                    withId(R.id.med_d_vaccine_name),
                    withTextOR(MED_D_ZOSTER, MED_D_TDAP)
                ),
                "$MED_D_ZOSTER or $MED_D_TDAP",
            )
        }

        if (hasTapNextBtn) {
            IntegrationUtil.delayedClick(withId(R.id.fab_next))
        }
    }

    fun verifyCorrectMedDCallToActionInApptData(appointmentId: String, expectedCallToAction: CallToAction) {
        val appointmentData = patientUtil.getAppointmentById(appointmentId)
        val callToAction =
            appointmentData?.toAppointment()?.getMedDCta()
        assert(expectedCallToAction == callToAction) {
            "Expected to verify the callToAction-$expectedCallToAction " +
                "in medicarePartDRiskDetermination object. But was $callToAction"
        }
        Timber.d(
            "Verified appt-$appointmentId has expected medD Call To " +
                "Action-$expectedCallToAction"
        )
    }

    /**
     * Verify text “Copay Required” is displayed above the product
     *
     * @param testProduct
     */
    fun verifyTextCopayRequiredDisplayed(testProduct: TestProducts) {
        // There is a time delay in the display text “Copay Required” here
        IntegrationUtil.verifyAndScrollToElementInRecyclerView(
            withId(R.id.rv_vaccines),
            "Product grid",
            allOf(
                allOf(
                    withId(R.id.checkout_vaccine_name),
                    withText(testProduct.antigenProductName)
                ),
                hasSibling(
                    allOf(
                        withId(R.id.checkout_vaccine_tips),
                        withText("Copay Required")
                    )
                )
            ),
            "TestProduct",
        )
    }

    /**
     * Verify Copay amount and Subtotal amount are displayed on the “Summary“ screen
     * @param testProduct Test Product used in test
     */
    fun verifyCheckoutSummaryScreenByCopayAndSubtotalAmount(testProduct: TestProducts) {
        IntegrationUtil.waitUIWithDelayed()
        val toolbarTitleText = context.resources.getString(R.string.summary)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.checkout_vaccine_icon),
                hasSibling(withText(testProduct.antigenProductName))
            ),
            allOf(
                withId(R.id.checkout_vis_date_label),
                withText("V.I.S DATE"),
                hasSibling(
                    allOf(
                        withId(R.id.checkout_vis_date),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withId(R.id.checkout_vaccine_lot_number),
                withText(testProduct.lotNumber)
            ),
            allOf(
                withId(R.id.checkout_vaccine_copay_title),
                withText("Copay"),
                hasSibling(
                    allOf(
                        withId(R.id.checkout_vaccine_copay_value),
                        not(withText(""))
                    )
                )
            )
        )

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withText("Subtotal"),
                hasSibling(
                    allOf(
                        withId(R.id.checkout_vaccine_copay_total),
                        not(withText(""))
                    )
                )
            )
        )
    }

    /**
     * Tap Check Out Another Patient button
     */
    fun tapCheckoutAnotherPatientButton() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.check_out_another)),
            "check out another"
        )
        IntegrationUtil.delayedClick(withId(R.id.check_out_another))
    }

    /**
     * Tap the red X to remove the dose
     */
    fun tapDeleteButtonToDeleteDose() {
//        IntegrationUtil.waitForElementToAppearAndEnabled(
//            onView(withId(R.id.checkout_vaccine_delete)),
//            "check out another"
//        )
//        IntegrationUtil.delayedClick(withId(R.id.checkout_vaccine_delete))
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(
                withId(R.id.button_delete)
            ),
            "Delete Dose",
            WAIT_TIME_FOR_LOAD
        )

    }

    private fun swipeLeft(): ViewAction {
        return ViewActions.actionWithAssertions(
            GeneralSwipeAction(
                Swipe.SLOW,
                GeneralLocation.TOP_RIGHT,
                GeneralLocation.CENTER_RIGHT,
                Press.FINGER
            )
        )
    }

    /**
     * Verify a checked out patient appears and visit is not highlighted purple
     * @param testPatient Test Patient used in test
     */
    fun verifyCheckedOutPatientVisitNotHighlightedPurple(testPatient: TestPatients) {
        IntegrationUtil.verifyAndScrollToElementInRecyclerView(
            withId(R.id.recycler_view),
            "Patients grid",
            allOf(
                allOf(
                    withId(R.id.patient_name),
                    withTextIgnoringCase(testPatient.completePatientName)
                ),
                withParent(
                    hasSibling(
                        allOf(
                            withId(R.id.checked_out_bg),
                            not(
                                withDrawableWithTintColor(
                                    R.drawable.bg_rounded_corner_purple,
                                    R.color.list_purple
                                )
                            )
                        )
                    )
                )
            ),
            "CheckedOutPatientVisitNotHighlightedPurple"
        )
    }

    /**
     * Tap “Collect Payment”
     *
     */
    fun tapCollectPaymentButton() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.collect_payment_info)),
            "Collect Payment Button"
        )
        IntegrationUtil.delayedClick(withId(R.id.collect_payment_info))
    }

    /**
     * Select “Debit or Credit” on “Copay Required” modal
     *
     * @param paymentModal “Debit or Credit” or “Cash or Check”
     */
    fun promptCopayRequiredDialogToSelect(
        paymentModal: PaymentModals,
        needVerifyHighlighted: Boolean = false,
        highlightItem: PaymentModals = PaymentModals.PaymentDebitOrCredit()
    ) {
        IntegrationUtil.waitUIWithDelayed()
        val medDCheckAvailableText = context.resources.getString(R.string.med_d_copay_dialog_title)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(medDCheckAvailableText)).inRoot(isDialog()),
            "Copay Required",
            5
        )
        if (needVerifyHighlighted) {
            onView(withText(highlightItem.display)).check(matches(withDrawable(R.drawable.button_primary_purple_bg)))
        }
        IntegrationUtil.delayedClick(withText(paymentModal.display))
    }

    /**
     * Verify Payment summary screen
     * 1. Patient Name
     * 2. Risk Messaging
     * 3. Patient Id
     * 4. DOB
     * 5. Product info/lot number
     * 6. Copay amount
     * 7. Total to be charged
     * 8. CC  type, and last 4 numbers
     * @param testProduct Test Product used in test
     * @param paymentModal
     * @param cardInfo
     */
    fun verifyPaymentSummaryScreen(
        testProduct: TestProducts,
        paymentModal: PaymentModals,
        cardInfo: TestCards? = null
    ) {
        IntegrationUtil.waitUIWithDelayed()
        val toolbarTitleText = context.resources.getString(R.string.med_d_summary_title)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withText("ID"),
                hasSibling(
                    allOf(
                        withId(R.id.med_d_patient_id),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withText("DOB"),
                hasSibling(
                    allOf(
                        withId(R.id.med_d_patient_dob),
                        not(withText(""))
                    )
                )
            )
        )

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.med_d_summary_vaccine_icon),
                hasSibling(withText(testProduct.antigenProductName))
            ),
            allOf(
                withId(R.id.med_d_summary_vaccine_lot_number),
                withText(testProduct.lotNumber)
            ),
            allOf(
                withId(R.id.med_d_summary_vaccine_copay_title),
                withSubstring("Copay"),
                hasSibling(
                    allOf(
                        withId(R.id.med_d_summary_vaccine_copay_value),
                        not(withText(""))
                    )
                )
            )
        )
        when (paymentModal) {
            is PaymentModals.PaymentDebitOrCredit -> {
                val lastFourNumbers =
                    cardInfo?.cardNumber?.substring(cardInfo.cardNumber.length - 4)
                IntegrationUtil.verifyElementsPresentOnPage(
                    2,
                    allOf(
                        withId(R.id.med_d_summary_copay_total),
                        hasSibling(
                            allOf(
                                withId(R.id.med_d_summary_copay_subtotal),
                                not(withText(""))
                            )
                        )
                    ),
                    allOf(
                        withId(R.id.med_d_summary_copay_card_icon),
                        hasSibling(
                            allOf(
                                withId(R.id.med_d_summary_copay_card_brand),
                                not(withText(""))
                            )
                        )
                    ),
                    allOf(
                        withId(R.id.med_d_summary_copay_card_number),
                        withText(lastFourNumbers)
                    )
                )
            }
            PaymentModals.PaymentCashOrCheck -> {
                IntegrationUtil.verifyElementsPresentOnPage(
                    2,
                    allOf(
                        withId(R.id.med_d_summary_copay_total),
                        hasSibling(
                            allOf(
                                withId(R.id.med_d_summary_copay_subtotal),
                                not(withText(""))
                            )
                        )
                    ),
                    allOf(
                        withId(R.id.med_d_summary_cash),
                        withText(paymentModal.display)
                    )
                )
            }
            PaymentModals.PaymentByPhone -> {
                IntegrationUtil.verifyElementsPresentOnPage(
                    2,
                    allOf(
                        withId(R.id.med_d_summary_copay_total),
                        hasSibling(
                            allOf(
                                withId(R.id.med_d_summary_copay_subtotal),
                                withText("")
                            )
                        )
                    ),
                    allOf(
                        withId(R.id.med_d_summary_copay_card_info),
                        withText(R.string.med_d_summary_copay_with_phone)
                    )
                )
            }
        }
    }

    /**
     * Tap “Patient Consent” arrow
     */
    fun tapPatientConsentButton() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.fab_next)),
            "Patient Consent"
        )
        IntegrationUtil.delayedClick(withId(R.id.fab_next))
    }

    /**
     * Verify texts displayed on 'Signature and Confirm' screen.
     */
    fun verifyToSignatureAndConfirmFragmentScreen() {
        IntegrationUtil.waitUIWithDelayed()
        val toolbarTitleText = context.resources.getString(R.string.med_d_signature_title)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
    }

    /**
     * tap screen for Signature
     */
    fun doSignatureForConfirm() {
        IntegrationUtil.waitForElementToAppear(
            onView(withId(R.id.med_d_signature_view)),
            "Signature Capture View"
        )
        IntegrationUtil.drawForSignature(withId(R.id.med_d_signature_view))
    }

    /**
     * Tap Signature Submit Button
     */
    fun tapSignatureSubmitButton() {
        IntegrationUtil.waitForElementToEnabled(onView(withId(R.id.med_d_signature_submit)))
        IntegrationUtil.clickOnNotFullyVisibleElement(
            onView(withId(R.id.med_d_signature_submit)),
            "Signature Submit Button"
        )
    }

    /**
     * Submitting checkout spinner should take <5 seconds
     *
     * @param maxLoadTime
     */
    fun verifySubmittingCheckoutScreenLoading(maxLoadTime: Int) {
        val showText = context.resources.getString(R.string.med_d_signature_submit)
        val realTime = IntegrationUtil.returnSecondToWaitElementFromAppearToDisappear(
            onView(withText(showText)),
            "LoadingForSubmit",
            WAIT_TIME_FOR_LOAD
        )
        Assert.assertTrue(
            "Verify that Submitting checkout spinner should take <5 seconds is ${realTime < maxLoadTime}",
            realTime < maxLoadTime
        )
    }

    /**
     * Tap the pencil icon to the right of the credit card info on the “Payment Summary” info screen
     */
    fun tapPencilIconToEditPayment() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.med_d_summary_card_edit)),
            "Pencil Icon"
        )
        IntegrationUtil.delayedClick(withId(R.id.med_d_summary_card_edit))
    }

    /**
     * Tap “Cash or Check” on the “Change Payment Method” modal
     */
    fun promptChangePaymentMethodDialogToSelect() {
        IntegrationUtil.waitUIWithDelayed()
        val cashOrCheckText = context.resources.getString(R.string.med_d_copay_dialog_cancel)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(cashOrCheckText)).inRoot(isDialog()),
            "Cash or Check",
            5
        )

        IntegrationUtil.delayedClick(withText(cashOrCheckText))
    }

    /**
     * Tap the pencil icon on the Checkout Complete screen
     */
    fun tapCheckoutEdit() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.checkout_edit)),
            "CheckoutEdit"
        )
    }

    /**
     * Verify that the Updated Summary screen shows the add doses
     */
    fun verifyUpdatedSummaryShowsBothDoses(list: List<TestProducts>) {
        for (item in list) {
            onView(withId(R.id.rv_vaccines)).check(matches(hasDescendant(withText(item.antigenProductName))))
        }
    }

    /**
     * Verify the “Keep Check Out” modal closes
     */
    fun verifyKeepCheckoutModelClose() {
        IntegrationUtil.verifyElementsNotPresentOnPage(
            1,
            allOf(withText(R.string.patient_checkout_back_continue))
        )
    }

    /**
     * Verify that the “Out of Age Indication” modal appears
     */
    fun verifyPromptOutOfAgeIndicationDialog() {
        val outOfAgeIndicationText =
            context.resources.getString(R.string.patient_checkout_out_of_age_exclusion_title)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(outOfAgeIndicationText)).inRoot(isDialog()),
            "Out of Age Indication",
            5
        )
    }

    /**
     * Verify that the “Doses Not Covered” modal appears
     */
    fun verifyPromptDosesNotCoveredDialog() {
        val medDCheckAvailableText =
            context.resources.getString(R.string.patient_checkout_prompt_exclusion_title)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(medDCheckAvailableText)).inRoot(isDialog()),
            "Doses Not Covered",
            5
        )
    }

    /**
     * tap close icon on action bar to cancel checkout.
     */
    fun tapCloseIconOnActionBarToCancelCheckout() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.toolbar_icon)),
            "checkout's close icon"
        )
    }

    /**
     * Verify that the “Keep Check Out” modal appears
     */
    fun verifyKeepCheckoutTextAppear() {
        IntegrationUtil.verifyElementsPresentOnPage(
            1,
            allOf(withText(R.string.patient_checkout_back_continue))
        )
    }

    /**
     * Select “Self-Pay” button,
     * Verify that this button also displays the $ amount for the cost of the dose (video ex = $168.37)
     */
    fun selectAndVerifyDisplaySelfPayPrompt(testProduct: TestProducts) {
        val pricePerDose = productUtil.findPricePerDoseByProductId(testProduct)
        val selfPayText = context.resources.getString(
            R.string.patient_checkout_base_exclusion_neutral_fmt,
            pricePerDose
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(selfPayText)).inRoot(isDialog()),
            selfPayText,
            5
        )

        IntegrationUtil.delayedClick(withText(selfPayText))
    }

    /**
     * Verify that the summary screen displays the two doses with separate payment modes:
     * 1. Shingrix as Self-Pay: Out of Age Indication with the cost of the product (video ex = $168.37)
     * 2. if isCopayRequired = false Varivax with no dollar amount
     * 3. if isCopayRequired = true Varivax as Copay  with the cost of the product
     *
     * @param testProduct Test Patient used in test
     * @param testProductWithSelfPay Test Product used in test
     * @param isCopayRequired
     */
    fun verifyCheckoutSummaryWithSeparatePaymentModes(
        testProduct: TestProducts,
        testProductWithSelfPay: TestProducts,
        isCopayRequired: Boolean = false
    ) {
        val toolbarTitleText = context.resources.getString(R.string.summary)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        val pricePerDose = productUtil.findPricePerDoseByProductId(testProductWithSelfPay)
        val selfPayCopayValue = context.resources.getString(
            R.string.med_d_check_copay_double,
            pricePerDose
        )
        // Shingrix as Self-Pay: with the cost of the product (video ex = $168.37)
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.checkout_vaccine_icon),
                hasSibling(withText(testProductWithSelfPay.antigenProductName))
            ),
            allOf(
                withId(R.id.checkout_vaccine_lot_number),
                withText(testProductWithSelfPay.lotNumber)
            ),
            allOf(
                withId(R.id.checkout_vaccine_copay_title),
                withText(
                    if (isCopayRequired) "Self-Pay: Not Covered" else "Self-Pay: Out of Age Indication"
                )
            ),
            allOf(
                withId(R.id.checkout_vaccine_copay_value),
                withText(selfPayCopayValue)
            )
        )

        if (isCopayRequired) {
            // Varivax as Copay  with the cost of the product
            IntegrationUtil.verifyElementsPresentOnPage(
                2,
                allOf(
                    withId(R.id.checkout_vaccine_icon),
                    hasSibling(withText(testProduct.antigenProductName))
                ),
                allOf(
                    withId(R.id.checkout_vaccine_lot_number),
                    withText(testProduct.lotNumber)
                ),
                allOf(
                    withId(R.id.checkout_vaccine_copay_title),
                    withText("Copay")
                ),
                allOf(
                    withId(R.id.checkout_vaccine_copay_value),
                    withSubstring("$")
                )
            )
        } else {
            // Varivax with no dollar amount
            IntegrationUtil.verifyElementsPresentOnPage(
                2,
                allOf(
                    withId(R.id.checkout_vaccine_icon),
                    hasSibling(withText(testProduct.antigenProductName))
                ),
                allOf(
                    withId(R.id.checkout_vaccine_lot_number),
                    withText(testProduct.lotNumber)
                ),
                allOf(
                    not(withSubstring("$"))
                )
            )
        }
    }

    /**
     * Tap cancel checkout button to cancel a checkout
     */
    fun tapCancelCheckoutButton() {
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(
                withText(R.string.patient_checkout_back_cancel)
            ),
            "CancelCheckout",
            WAIT_TIME_FOR_LOAD
        )
    }

    /**
     * Tap delete button to remove dose
     */
    fun tapDeleteButton() {
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(
                withId(R.id.button_delete)
            ),
            "Delete Dose",
            WAIT_TIME_FOR_LOAD
        )
    }

    /**
     * Tap keep checkout button to continue a checkout
     */
    fun tapKeepCheckoutButton() {
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(
                withText(R.string.patient_checkout_back_continue)
            ),
            "CancelCheckout",
            WAIT_TIME_FOR_LOAD
        )
    }

    /**
     * Tap the “Debit or Credit” button on the “Select Payment Method” modal
     *
     * @param paymentModal “Debit or Credit” or “Cash or Check”
     */
    fun promptSelectPaymentDialogToSelect(paymentModal: PaymentModals) {
        val paymentText = context.resources.getString(R.string.self_pay_dialog_title)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(paymentText)).inRoot(isDialog()),
            "Select Payment",
            5
        )

        IntegrationUtil.delayedClick(withText(paymentModal.display))
    }

    /**
     * Verify that the “Confirm and Pay” screen displays the two doses with separate payment modes:
     * 1. Shingrix as Self-Pay: Out of Age Indication with the cost of the product (video ex = $168.37)
     * 2. Varivax with no dollar amount
     *
     * @param testProduct Test Patient used in test
     * @param testProductWithSelfPay Test Product used in test
     */
    fun verifyConfirmAndPayScreen(
        testProduct: TestProducts,
        testProductWithSelfPay: TestProducts,
        cardInfo: TestCards? = null
    ) {
        IntegrationUtil.waitUIWithDelayed()
        val toolbarTitleText = context.resources.getString(R.string.self_pay_one_touch_checkout)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        val pricePerDose = productUtil.findPricePerDoseByProductId(testProductWithSelfPay)
        val selfPayCopayValue = context.resources.getString(
            R.string.med_d_check_copay_double,
            pricePerDose
        )
        // Shingrix as Self-Pay: Out of Age Indication with the cost of the product (video ex = $168.37)
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.med_d_summary_vaccine_icon),
                hasSibling(withText(testProductWithSelfPay.antigenProductName))
            ),
            allOf(
                withId(R.id.med_d_summary_vaccine_lot_number),
                withText(testProductWithSelfPay.lotNumber)
            ),
            allOf(
                withId(R.id.med_d_summary_vaccine_copay_title),
                withText("Self-Pay: Out of Age Indication")
            ),
            allOf(
                withId(R.id.med_d_summary_vaccine_copay_value),
                withText(selfPayCopayValue)
            )
        )

        // Varivax with no dollar amount
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.med_d_summary_vaccine_icon),
                hasSibling(withText(testProduct.antigenProductName))
            ),
            allOf(
                withId(R.id.med_d_summary_vaccine_lot_number),
                withText(testProduct.lotNumber)
            ),
            allOf(
                not(withSubstring("$"))
            )
        )

        // Verify that the total to be charged amount is displayed
        // and Verify the Credit Card summary info is displayed
        verifyCreditCardSummaryInfo(cardInfo)
    }

    /**
     * Verify that the total to be charged amount is displayed
     * and Verify the Credit Card summary info is displayed
     *
     * @param cardInfo
     */
    fun verifyCreditCardSummaryInfo(cardInfo: TestCards? = null) {
        val totalChargedText =
            context.resources.getString(R.string.med_d_checkout_vaccine_copay_total)
        val lastFourNumbers = cardInfo?.cardNumber?.substring(cardInfo.cardNumber.length - 4)
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.med_d_summary_copay_total),
                withText(totalChargedText),
                hasSibling(
                    allOf(
                        withId(R.id.med_d_summary_copay_subtotal),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withId(R.id.med_d_summary_copay_card_icon),
                hasSibling(
                    allOf(
                        withId(R.id.med_d_summary_copay_card_brand),
                        not(withText(""))
                    )
                )
            ),
            allOf(
                withId(R.id.med_d_summary_copay_card_number),
                withText(lastFourNumbers)
            )
        )
    }

    /**
     * Tap the “Patient Counseling” check button to confirm the checkout
     */
    fun tapPatientCounselingCheckButton() {
        IntegrationUtil.clickOnNotFullyVisibleElement(
            onView(withId(R.id.continueBtn)),
            "Patient Counseling Button"
        )
    }

    /**
     * Verify that the “Checkout Complete” screen displays:
     * 2 Check Outs
     * 2 Shots Administered
     */
    fun verifyCheckoutCompleteScreenAfterSplitPayment(testPatient: TestPatients) {
        val titleText = context.resources.getString(R.string.patient_checkout_complete)
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.checkout_complete_title),
                withText(titleText)
            )
        )

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.patient_check_eligibility_icon),
                hasSibling(
                    allOf(
                        withId(R.id.checkout_complete_patient_name),
                        withTextIgnoringCase(testPatient.completePatientName)
                    )
                )
            ),
            allOf(
                withId(R.id.patient_shot_administered),
                withText("2 Shots Administered")
            )
        )
    }

    /**
     * Get the number of patients in the list based on patient name
     *
     * @param testPatient Test Patient used in test
     */
    fun getCountInAppointmentListByPatientName(testPatient: TestPatients): Int {
        return IntegrationUtil.getNumberOfNameInAppointmentList(
            withId(R.id.recycler_view),
            "Patients grid",
            testPatient
        )
    }

    /**
     * Verify the Risk Free visit will have the testProduct checkout
     * and verify that the correct vaccination was assigned to the correct visit by payment mode logo.
     *
     * @param testPatient
     * @param testProduct
     */
    fun verifyRiskFreeCheckoutInVisit(testPatient: TestPatients, testProduct: TestProducts) {
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "Loading",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(SCAN_SCREEN_TITLE)

        // Please ignore the resource file error here, it can be compiled through
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withTextIgnoringCase(testPatient.completePatientName),
                hasSibling(
                    allOf(
                        withId(R.id.toolbar_sub_icon),
                        withDrawableByInstrumentation(RiskIconConstant.RiskFreeIcon.resourceId)
                    )
                )
            )
        )

        verifyProductCheckoutInVisit(testProduct)
    }

    /**
     * Verify the Self-Pay visit will have the testProduct checkout:
     * and verify that the correct vaccination was assigned to the correct visit by payment mode logo.
     *
     * @param testPatient
     * @param testProduct
     */
    fun verifySelfPayCheckoutInVisit(testPatient: TestPatients, testProduct: TestProducts) {
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "Loading",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(SCAN_SCREEN_TITLE)

        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withTextIgnoringCase(testPatient.completePatientName),
                hasSibling(
                    allOf(
                        withId(R.id.toolbar_sub_icon),
                        withDrawable(R.drawable.ic_one_touch_self_pay)
                    )
                )
            )
        )

        verifyProductCheckoutInVisit(testProduct)
    }

    /**
     * The visit will have the testProduct checkout
     *
     * @param testProduct
     */
    private fun verifyProductCheckoutInVisit(testProduct: TestProducts) {
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.checkout_vaccine_icon),
                hasSibling(withText(testProduct.antigenProductName))
            ),
            allOf(
                withId(R.id.checkout_vaccine_lot_number),
                withText(testProduct.lotNumber)
            )
        )
    }

    /**
     * Tap the “Cancel Check Out” button on the “Cancel Check Out” modal
     */
    fun promptCancelCheckDialogToBack() {
        IntegrationUtil.delayedClick(withId(R.id.toolbar_icon))

        val cancelCheckText = context.resources.getString(R.string.patient_checkout_back_title)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(cancelCheckText)).inRoot(isDialog()),
            "Cancel Check Out",
            5
        )

        IntegrationUtil.delayedClick(withText("Cancel Check Out"))
    }

    /**
     * Tap the “Cancel Check Out” button on the “Processing Appointment” modal
     */
    fun promptProcessingAppointmentDialogToRefreshSplit() {
        val processingText =
            context.resources.getString(R.string.processing_appointment_dialog_title)
        IntegrationUtil.waitForElementToAppear(
            onView(withText(processingText)).inRoot(isDialog()),
            "Processing Appointment",
            5
        )

        IntegrationUtil.delayedClick(withText("Refresh Schedule"))
    }

    /**
     * Verify the “Check Out Other Family” display && Check Out Parent/Guardian“ text beneath
     */
    fun verifyCheckoutOtherFamilyDisplay() {
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.patient_parent_guardian_flu_shots),
                withText(R.string.patient_parent_guardian_flu_shots)
            )
        )

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.patient_parent_insured_name),
                not(withText(""))
            )
        )

        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.patient_add_parent),
                withText(R.string.patient_add_parent)
            )
        )
    }

    /**
     * Select the existing family member name that’s displayed
     */
    fun selectFamilyMemberCheckout() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.patient_parent_insured_name)),
            "FamilyMember"
        )
    }

    /**
     * Confirm patient info
     */
    fun confirmParentInfo() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.button_ok)),
            "confirmParentInfo"
        )
    }

    /**
     * Scroll the appointment list to make the family member display
     */
    fun scrollToFamilyPatient(familyName: String?) {
        if (familyName.isNullOrEmpty()) {
            throw Exception("get family member name is null or empty")
        }
        onView(withId(R.id.recycler_view)).perform(
            RecyclerViewActions.scrollTo<AppointmentListItemViewHolder>(
                hasDescendant(withText(familyName))
            )
        )
    }

    /**
     * get the family member`s mane
     */
    fun getFamilyMemberName(): String? {
        return IntegrationUtil.getText(onView(withId(R.id.patient_parent_insured_name)))
    }

    /**
     * Verify a checked out patient appears and visit is highlighted purple
     * @param testPatientName Test Patient used in test
     */
    fun verifyCheckedOutPatientVisitHighlightedPurple(testPatientName: String?) {
        if (testPatientName.isNullOrEmpty()) {
            throw Exception("get family member name is null or empty")
        }
        IntegrationUtil.verifyAndScrollToElementInRecyclerView(
            withId(R.id.recycler_view),
            "Patients grid",
            allOf(
                allOf(
                    withId(R.id.patient_name),
                    withTextIgnoringCase(testPatientName)
                ),
                withParent(
                    hasSibling(
                        allOf(
                            withId(R.id.checked_out_bg),
                            withDrawable(R.drawable.bg_rounded_corner_purple)
                        )
                    )
                )
            ),
            "CheckedOutPatientVisitHighlightedPurple",
        )
    }

    /**
     * Tap the partner bill button
     */
    fun tapPartnerBillButton() {
        IntegrationUtil.waitForElementToAppear(
            onView(withId(R.id.button_continue)),
            "PartnerBill"
        )
        IntegrationUtil.delayedClick(withId(R.id.button_continue))
    }

    /**
     * Verify that the summary screen displays the two doses with separate payment modes
     */
    fun verifyTwoDosesWithSeparatePaymentModes(productList: ArrayList<TestProducts>) {
        for (item in productList) {
            IntegrationUtil.verifyElementsPresentOnPage(
                2,
                allOf(
                    withId(R.id.checkout_vaccine_lot_number),
                    withText(item.lotNumber)
                )
            )
        }
    }

    /**
     * Scroll the appointment list to end
     */
    fun scrollAppointmentListToEnd() {
        IntegrationUtil.waitUIWithDelayed()
        onView(withId(R.id.recycler_view)).perform(
            RecyclerViewActions.scrollToPosition<AppointmentListItemViewHolder>(
                IntegrationUtil.getCountFromRecyclerView(withId(R.id.recycler_view)) - 1
            )
        )
        IntegrationUtil.waitUIWithDelayed()
    }

    /**
     * Verify that there are now 2 patient visits for the patient, whereas pre-check out there was only one
     */
    fun verifyPatientVisitCountAndCheckoutCount(testPatient: TestPatients, visitList: ArrayList<Int>) {
        scrollAppointmentListToEnd()
        onView(withId(R.id.recycler_view)).perform(
            patientVisitCountAndCheckoutCountAction(
                testPatient,
                visitList
            )
        )
    }

    /**
     * Check visit count and checkout count
     */
    private fun patientVisitCountAndCheckoutCountAction(
        testPatient: TestPatients,
        visitList: ArrayList<Int>
    ): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(
                    isAssignableFrom(
                        RecyclerView::class.java
                    ),
                    isDisplayed()
                )
            }

            override fun getDescription(): String {
                return "Verify that there are now 2 patient visits for the patient, " +
                    "whereas pre-check out there was only one"
            }

            override fun perform(uiController: UiController?, view: View?) {
                val recyclerView = view as RecyclerView
                val adapter = recyclerView.adapter

                val itemCount = adapter!!.itemCount
                recyclerView.smoothScrollToPosition(itemCount - 1)
                for (i in 0 until itemCount) {
                    val itemView = recyclerView.layoutManager!!.findViewByPosition(i)
                    if (itemView != null) {
                        val textView = itemView.findViewById<TextView>(R.id.patient_name)
                        if (textView != null && textView.text.toString()
                                .equals(testPatient.completePatientName, true)
                        ) {
                            visitList.add(i)
                        }
                    }
                }
                assertThat(visitList, Matchers.hasSize(2))
            }
        }
    }

    /**
     * Verify the Partner Bill visit will have the testProduct checkout:
     * and verify that the correct vaccination was assigned to the correct visit by payment mode logo.
     *
     * @param testPatient
     * @param testProduct
     */
    fun verifyPartnerBillCheckoutInVisit(testPatient: TestPatients, testProduct: TestProducts) {
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "Loading",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(SCAN_SCREEN_TITLE)
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withTextIgnoringCase(testPatient.completePatientName),
                hasSibling(
                    allOf(
                        withId(R.id.toolbar_sub_icon),
                        withDrawable(RiskIconConstant.PartnerBillIcon().resourceId)
                    )
                )
            )
        )
        verifyProductCheckoutInVisit(testProduct)
    }

    /**
     * Verify  Shots Administered
     */
    fun verifyTwoShotsAdministered() {
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.patient_shot_administered),
                withText("2 Shots Administered")
            )
        )
        IntegrationUtil.waitUIWithDelayed()
    }

    fun verifyMedDTagDisplayedOnScanDosesScreen() {
        onView(withId(R.id.med_d_tag)).check(
            matches(
                allOf(
                    withText("MED D"),
                    withDrawable(R.drawable.bg_rounded_corner_gray)
                )
            )
        )
    }

    /**
     * Wait for Loading finished on base Fragment.
     */
    private fun waitForBaseFragmentLoadingFinish() {
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "Loading",
            WAIT_TIME_FOR_LOAD
        )
    }

    /**
     * Click first element in appointment list by patient Name.
     * @param testPatient Patient entity
     */
    fun clickFirstElementInAppointmentListByPatientName(testPatient: TestPatients) {
        val count = IntegrationUtil.getTotalRowsInRecyclerView(
            withId(R.id.recycler_view),
            "Appointment list"
        )
        if (count == 0) {
            throw Exception(
                "Expected to find following element on Appointment list-${testPatient.completePatientName}"
            )
        }
        for (i in 0 until count) {
            onView(withId(R.id.recycler_view)).perform(
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(
                    i
                )
            )
            if (IntegrationUtil.checkNameInAppointmentList(
                    withId(R.id.recycler_view),
                    testPatient,
                    i
                )
            ) {
                onView(withId(R.id.recycler_view)).perform(
                    RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(i, click())
                )
                // wait for page to fully load by waiting for lot search button to get displayed
                IntegrationUtil.waitForElementToAppear(
                    onView(
                        allOf(
                            withId(R.id.patient_checkout_lot_search_btn),
                            isDisplayed()
                        )
                    ),
                    "Lot Search Button after opening appointment",
                    WAIT_TIME_FOR_LOAD
                )
                break
            }
        }
    }

    /**
     * Verify the patient info.
     *
     * @param testPatient Patient entity
     * @param riskIconConstant RiskIconConstant entity
     * @param eligibilityText eligibility text
     * @param responsibilityText responsibility text
     * @param payerText payer text
     * @param eligibilityCtaText eligibility cat text
     * @param medDCatText medD cat text
     * @param runCopayCheckText run copay check text
     * @param isMedDTagShow is MedD tag show
     */
    fun verifyCheckoutPatientInfoText(
        testPatient: TestPatients,
        riskIconConstant: RiskIconConstant,
        eligibilityText: String = "",
        responsibilityText: String = "",
        payerText: String = "",
        eligibilityCtaText: String = "",
        medDCatText: String = "",
        isRunCopayCheckVisible: Boolean = false,
        isMedDTagShow: Boolean
    ) {
        val toolbarTitleText = context.resources.getString(R.string.scan_doses)

        val patientFullName = testPatient.lastName.plus(", ").plus(testPatient.firstName)

        val riskIconMatcher: Matcher<View> = allOf(
            withDrawableWithTintColorByInstrumentation(
                riskIconConstant
            ),
            isDisplayed()
        )

        val medTagMatcher = if (isMedDTagShow) {
            allOf(
                withText("MED D"),
                withDrawable(R.drawable.bg_rounded_corner_gray),
                isDisplayed()
            )
        } else {
            not(isDisplayed())
        }
        waitForBaseFragmentLoadingFinish()
        IntegrationUtil.waitUIWithDelayed()
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
        onView(withId(R.id.patient_name)).check(matches(withTextIgnoringCase(patientFullName)))
        onView(withId(R.id.eligibility_icon)).check(matches(riskIconMatcher))
        if (!eligibilityText.isNullOrEmpty()) {
            onView(withId(R.id.eligibility_text)).check(matches(withText(eligibilityText)))
        }
        if (!responsibilityText.isNullOrEmpty()) {
            onView(withId(R.id.responsibility_text)).check(matches(withText(responsibilityText)))
        }
        if (!payerText.isNullOrEmpty()) {
            onView(withId(R.id.payer_text)).check(matches(withText(payerText)))
        }
        if (!eligibilityCtaText.isNullOrEmpty()) {
            onView(withId(R.id.eligibility_cta)).check(matches(withText(eligibilityCtaText)))
        }
        if (!medDCatText.isNullOrEmpty()) {
            onView(withId(R.id.med_d_cta)).check(matches(withText(medDCatText)))
        }
        if (isRunCopayCheckVisible) {
            onView(withId(R.id.run_copay_btn)).check(matches(withText(R.string.run_copay_check)))
        }
        onView(withId(R.id.med_d_tag)).check(matches(medTagMatcher))
    }

    fun notShowRequireCopayText() {
        onView(withId(R.id.med_d_cta)).check(matches(not(withText("MED D: COPAY REQUIRED FOR TDAP AND ZOSTER"))))
    }

    /**
     * Verify the title texts are displayed- 'Med D Copay Check Results','Med D Copay Requirements'
     *
     */
    fun verifyToMedDCoPayCheckResultsScreen() {
        val toolbarTitleText = context.resources.getString(R.string.med_d_check_title)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.results_header_text),
                withText(context.resources.getString(R.string.med_d_check_checked_prompt))
            )
        )
    }

    fun checkMedDCTAText(medDCTA: String) {
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.med_d_cta),
                withText(medDCTA),
                isDisplayed()
            )
        )
        Timber.d("Verified correct medD message shown-$medDCTA")
    }

    fun checkMedDCoPayDisplayedOnScanDoses(copays: List<MedDCheckResponse.Copay>) {
        assert(copays.isNotEmpty()) {
            "Expected to check MedDCoPay are displayed on Scan Doses. but copays is empty"
        }
        copays.firstOrNull { it.antigen == MedDVaccines.TDAP.value }?.let {
            IntegrationUtil.verifyElementsPresentOnPage(
                1,
                allOf(
                    withId(R.id.tdapCopay),
                    withText(copays[0].antigen),
                    hasSibling(withText("$ " + copays[0].copay))
                )
            )
        }
        copays.firstOrNull { it.antigen == MedDVaccines.ZOSTER.value }?.let {
            IntegrationUtil.verifyElementsPresentOnPage(
                1,
                allOf(
                    withId(R.id.zosterCopay),
                    withText(copays[1].antigen),
                    hasSibling(withText("$ " + copays[1].copay))
                )
            )
        }
    }

    fun confirmUnorderedDoseDialog() {
        IntegrationUtil.confirmAlertDialog(
            processingTextResId = R.string.orders_unordered_dose_prompt_message,
            userFriendlyName = "Unordered Dose Dialog",
            selectedStringResId = R.string.orders_unordered_dose_prompt_yes
        )
    }

    fun verifyDoseAddList(testProducts: List<TestProducts>, testSites: TestSites) {
        IntegrationUtil.waitForElementInRecyclerView(
            withId(R.id.rv_vaccines),
            "Vaccine List",
            allOf(
                withId(R.id.checkout_vaccine_name),
                withText(testProducts.first().antigenProductName)
            )
        )
        onView(withId(R.id.rv_vaccines)).check(
            matches(
                DoseAddedListMatcher(
                    testProducts,
                    testSites
                ) as Matcher<in View>
            )
        )
    }

    fun verifyOrderedDoseAddList(orderEntityList: List<TestOrderDose>) {
        IntegrationUtil.waitForElementInRecyclerView(
            withId(R.id.rv_vaccines),
            "Ordered Dse List",
            allOf(
                withId(R.id.checkout_vaccine_name),
                withTextIgnoringCase(orderEntityList[0].shortDescription)
            )
        )
        onView(withId(R.id.rv_vaccines)).check(
            matches(
                OrderedDoseAddedListMatcher(
                    orderEntityList
                ) as Matcher<in View>
            )
        )
    }

    /**
     * select reason for unordered doses
     */
    fun tapSetReasonAndSelectedReason() {
        onView(withText(R.string.orders_review_set_reason)).perform(click())
        IntegrationUtil.waitUIWithDelayed(1000)
        IntegrationUtil.clickBottomDialogItem(R.string.orders_unordered_dose_reason_order_not_appearing)
    }

    /**
     * Returns CoPay amount based on dose administered
     *
     * @param medDCheckResponse - medD response object for that appt
     * @param testProducts - products administered
     * @return copay amount for the dose administered.
     */
    private fun getCopayTotalValue(medDCheckResponse: MedDCheckResponse, testProducts: List<TestProducts>): String {
        var copays = medDCheckResponse.copays
        val sum: Double = testProducts.sumOf { product ->
            copays.filter { it.antigen == product.antigen }.sumOf { it.copay.toDouble() }
        }
        return sum.toString()
    }

    /**
     * Verify checkout summary screen(head,body and bottom)
     *
     * @param testPartners partner entity used in this test
     * @param testPatient patient entity used in this test
     * @param testProducts product entity used in this test
     * @param testSites site entity used in this test
     * @param medDCheckResponse medDCheckResponse entity
     */
    fun verifyAddedDoseAndCoPaySubTotalOnSummaryScreen(
        testPartners: TestPartners,
        testPatient: TestPatients,
        testProducts: List<TestProducts>,
        testSites: TestSites,
        medDCheckResponse: MedDCheckResponse? = null
    ) {
        IntegrationUtil.waitUIWithDelayed()
        val toolbarTitleText = context.resources.getString(R.string.summary)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
        val copayValue =
            medDCheckResponse?.let { getCopayTotalValue(medDCheckResponse, testProducts) }
        onView(withId(R.id.rv_vaccines)).check(
            matches(
                CheckSummaryListMatcher(
                    testPartners,
                    testPatient,
                    testProducts,
                    testSites,
                    copayValue
                ) as Matcher<in View>
            )
        )
    }

    fun tapCollectPaymentInfo() {
        IntegrationUtil.delayedClick(withId(R.id.collect_payment_info))
    }

    fun verifyAddedDoseAndTotalToCollectOnPaymentSummaryScreen(
        testPatient: TestPatients,
        testProduct: TestProducts,
        medDCheckResponse: MedDCheckResponse
    ) {
        val toolbarTitleText = context.resources.getString(R.string.med_d_summary_title)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
        val copayValue = getCopayTotalValue(medDCheckResponse, listOf(testProduct))
        onView(withId(R.id.rv_vaccines)).check(
            matches(
                CheckPaymentSummaryListMatcher(
                    testPatient,
                    listOf(testProduct),
                    copayValue,
                    PaymentModals.PaymentCashOrCheck
                ) as Matcher<in View>
            )
        )
    }

    /**
     * Click ‘Run Copay Check’
     */
    fun tapRunCopayCheckBtn() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.run_copay_btn)),
            "RunCopayCheckBtn"
        )
        IntegrationUtil.delayedClick(withId(R.id.run_copay_btn))
    }

    /**
     * Verify checkout complete screen.
     * @param testPatient patient entity used in this test
     */
    fun verifyCheckoutComplete(testPatient: TestPatients) {
        val titleText = context.resources.getString(R.string.patient_checkout_complete)
        IntegrationUtil.verifyElementsPresentOnPage(
            6,
            allOf(
                withId(R.id.checkout_complete_title),
                withText(titleText)
            )
        )
        IntegrationUtil.verifyElementsPresentOnPage(
            6,
            allOf(
                withId(R.id.checkout_complete_patient_name),
                withTextIgnoringCase(testPatient.completePatientName)
            ),
            allOf(
                withId(R.id.patient_shot_administered),
                withSubstring("1 Shot Administered")
            )
        )
    }

    /**
     * Verify eligibility icon and tags on appointment list.
     *
     * @param testPatient patient entity used in this test
     * @param eligibilityIcon riskIconConstant entity
     * @param testStockPillList list of tags to be verified
     * @param itemBackground appointment list item background entity
     */
    fun verifyAppointmentEligibilityIconAndTags(
        testPatient: TestPatients,
        eligibilityIcon: RiskIconConstant,
        testStockPillList: List<TestStockPill>? = null,
        itemBackground: TestBackground? = null
    ) {
        val stockPillMatcher = AppointmentEligibilityIconAndTagMatcher(
            eligibility = eligibilityIcon,
            testStockPillList = testStockPillList ?: emptyList(),
            testBackground = itemBackground ?: TestBackground.None
        ) as Matcher<View>
        Timber.d("waiting for Appointment List recyclerView to check -2")
        IntegrationUtil.checkAppointmentListItemByPatientName(
            withId(R.id.recycler_view),
            "Appointment List",
            testPatient,
            stockPillMatcher
        )
    }

    /**
     * Verify Wrong Stock dialog (with Stock Selector FF on) and tap a selection
     * @param selection
     */
    fun verifyWrongStockDialogToSelect(selection: String) {
        val dialogHeader = context.resources.getString(R.string.dialog_wrong_stock_selected_title)
        val dialogBody = context.resources.getString(
            R.string.dialog_wrong_stock_selected_body_fmt,
            "Private",
            context.resources.getString(R.string.dialog_wrong_stock_selected_suffix2)
        )
        val topButtonText = context.resources.getString(R.string.menu_stock_selector_set_stock)
        val middleButtonText = context.resources.getString(R.string.dialog_wrong_stock_selected_remove_dose)
        val bottomButtonText = context.resources.getString(R.string.dialog_wrong_stock_selected_keep_dose)

        IntegrationUtil.waitUIWithDelayed()
        IntegrationUtil.waitForElementToAppear(
            onView(withText(dialogHeader)).inRoot(isDialog()),
            "Wrong Stock Title",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(dialogBody)).inRoot(isDialog()),
            "Wrong Stock Body",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(topButtonText)).inRoot(isDialog()),
            "Set Stock Button",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(middleButtonText)).inRoot(isDialog()),
            "Remove Dose Button",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(bottomButtonText)).inRoot(isDialog()),
            "Keep Dose Button",
            5
        )

        IntegrationUtil.waitUIWithDelayed()
        onView(withText(selection)).inRoot(isDialog()).perform(click())
    }

    /**
     * Verify Set Stock bottom sheet dialog (Stock Selector FF on) and tap a selection
     *
     * @param selection
     */
    fun verifySetStockBottomDialogToSelect(selection: String) {
        val setStockString = context.resources.getString(R.string.menu_stock_selector_set_stock)
        val privateString = context.resources.getString(R.string.menu_stock_selector_private)
        val vfcEnrolledString = context.resources.getString(R.string.menu_stock_selector_vfc_enrolled)
        val spinnerString = context.resources.getString(R.string.fragment_checkout_stock_saving_info)

        IntegrationUtil.waitUIWithDelayed()
        IntegrationUtil.waitForElementToAppear(
            onView(withText(setStockString)).inRoot(isDialog()),
            "Set Stock Title",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(privateString)).inRoot(isDialog()),
            "Set Stock Body",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(vfcEnrolledString)).inRoot(isDialog()),
            "VFC Medicaid option",
            5
        )

        IntegrationUtil.waitUIWithDelayed()
        onView(withText(selection)).inRoot(isDialog()).perform(click())

        IntegrationUtil.waitForElementToAppear(
            onView(withText(spinnerString)),
            "Stock Switch Spinner",
            5
        )

        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "Loading",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.waitUIWithDelayed()
    }

    /**
     * Verify text “Unordered Dose” modal is displayed and Tap “Yes,Keep Dose”
     * @param selection text click
     */
    fun verifyUnorderedDoseDialogToSelect(selection: String) {
        IntegrationUtil.waitUIWithDelayed()
        val dialogHeader = context.resources.getString(R.string.orders_unordered_dose_prompt_title)
        val dialogText = context.resources.getString(R.string.orders_unordered_dose_prompt_message)
        val topButtonText = context.resources.getString(R.string.orders_unordered_dose_prompt_yes)
        val bottomButtonText = context.resources.getString(R.string.orders_unordered_dose_prompt_no)

        IntegrationUtil.waitForElementToAppear(
            onView(withText(dialogHeader)).inRoot(isDialog()),
            "Unordered Dose",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(dialogText)).inRoot(isDialog()),
            "prompt message",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(topButtonText)).inRoot(isDialog()),
            "Yes,Keep Dose",
            5
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withText(bottomButtonText)).inRoot(isDialog()),
            "No,Remove Dose",
            5
        )
        IntegrationUtil.waitUIWithDelayed()
        onView(withText(selection)).inRoot(isDialog()).perform(click())
    }

    /**
     * Verify review doses screen and set reason.
     * @param testProduct product in this test
     */
    fun verifyReviewDosesScreenAndClickSetReason(testProduct: TestProducts) {
        IntegrationUtil.verifyDestinationScreenByToolbarTitle("Review Doses")
        val promptText = context.resources.getString(R.string.orders_review_header_title_unordered)
        val subText = context.resources.getString(R.string.orders_review_header_subtitle_unordered)
        val setReasonButtonText = context.resources.getString(R.string.orders_review_set_reason)
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.review_title),
                withText(promptText)
            ),
            allOf(withId(R.id.review_subtitle), withText(subText)),
            allOf(withId(R.id.display_reason_label), withText(setReasonButtonText)),
            allOf(
                withId(R.id.vaccine_icon),
                withDrawable(R.drawable.ic_presentation_single_dose_vial)
            ),
            allOf(
                withId(R.id.vaccine_product_name),
                withText(testProduct.antigenProductName)
            )
        )
        IntegrationUtil.waitUIWithDelayed()
        onView(withText("Set Reason")).perform(click())
    }

    /**
     * Verify review doses screen for ordered dose and set reason.
     */
    fun verifyReviewDosesScreenForOrderedDose(
        promptText: String,
        subText: String = "",
        setReasonButtonText: String,
        testOrderDose: TestOrderDose? = null,
        testProduct: TestProducts? = null
    ) {
        IntegrationUtil.verifyDestinationScreenByToolbarTitle("Review Doses")
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.review_title),
                withText(promptText)
            ),
            allOf(withId(R.id.display_reason_label), withText(setReasonButtonText)),
        )

        if (!subText.isNullOrEmpty()) {
            IntegrationUtil.verifyElementsPresentOnPage(
                IntegrationUtil.WAIT_TIME_DEFAULT,
                allOf(withId(R.id.review_subtitle), withText(subText)),
            )
        }

        testOrderDose?.let {
            IntegrationUtil.verifyElementsPresentOnPage(
                IntegrationUtil.WAIT_TIME_DEFAULT,
                allOf(
                    withId(R.id.vaccine_icon),
                    withDrawable(testOrderDose.icon)
                ),
                allOf(
                    withId(R.id.vaccine_product_name),
                    withText(testOrderDose.shortDescription)
                )
            )
        }

        testProduct?.let {
            IntegrationUtil.verifyElementsPresentOnPage(
                IntegrationUtil.WAIT_TIME_DEFAULT,
                allOf(
                    withId(R.id.vaccine_icon),
                    withDrawable(testProduct.icon)
                ),
                allOf(
                    withId(R.id.vaccine_product_name),
                    withText(testProduct.antigenProductName)
                )
            )
            IntegrationUtil.verifyElementsPresentOnPage(
                IntegrationUtil.WAIT_TIME_DEFAULT,
                allOf(withId(R.id.vaccine_product_name), withText(testProduct.antigenProductName))
            )
        }
    }

    fun verifyReasonBottomDialog(reasonList: List<String>) {
        IntegrationUtil.verifyBottomDialogItems(
            withId(R.id.rv_bottom),
            "Reason bottom dialog",
            reasonList
        )
    }

    fun clickReasonBottomDialogItem(itemSelectedResId: Int) {
        IntegrationUtil.clickBottomDialogItem(itemSelectedResId)
    }

    fun verifyReasonText(itemSelectedString: String) {
        onView(withId(R.id.display_reason_label)).check(matches(withText(itemSelectedString)))
    }

    /**
     * Verify the "next" button is disabled until select reason.
     */
    fun verifyCannotCompleteCheckoutUntilSelectReason() {
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(
                withId(R.id.fab_next),
                isNotEnabled(),
                isDisplayed(),
                withBackgroundTintForFloatingActionButton(R.color.primaryPurpleDisabled)
            )
        )
        IntegrationUtil.delayedClick(withId(R.id.fab_next))
    }

    /**
     * Verify appointment list item background.
     *
     * @param testPatient patient entity used in this test
     */
    fun verifyAppointmentBackgroundForPrimaryPurple(testPatient: TestPatients) {
        val stockPillMatcher = hasDescendant(
            allOf(
                withId(R.id.checked_out_bg),
                withDrawableWithTintColor(
                    R.drawable.bg_rounded_corner_purple,
                    R.color.list_purple
                ),
                isDisplayed()
            )
        )
        IntegrationUtil.checkAppointmentListItemByPatientName(
            withId(R.id.recycler_view),
            "Appointment List",
            testPatient,
            stockPillMatcher
        )
    }

    /**
     * Verifies that the checkout displays the placeholder scan message
     *
     */
    fun verifyNoDosesInCheckout() {
        val emptyText = context.resources.getString(R.string.patient_scan_doses_usin)
        // verify text and display
        onView(withId(R.id.patient_empty_text)).check(
            matches(
                allOf(withText(emptyText), isDisplayed())
            )
        )
    }

    /**
     * Verify Patient Info and all highlighted fields are displayed.
     * “Scan doses using the viewfinder.” is Displayed and proceed button is ‘not enabled and gray in color’.
     *
     * @param testPatient
     */
    fun verifyPatientInfoOnScanDosesScreen(
        testPatient: TestPatients,
        appointment: Appointment,
        hasDisplayViewfinder: Boolean = true
    ) {
        IntegrationUtil.waitUIWithDelayed()
        val tintColor = appointment.vaccineSupplyColor()
        // Verify RiskIcon that indicate patient status.
        IntegrationUtil.checkRiskIconForPatient(
            withId(R.id.eligibility_icon),
            RiskIconConstant.RiskFreeIcon.resourceId,
            RiskIconConstant.RiskFreeIcon.isInstrumentation,
            tintColor
        )

        val riskMessage = appointment.encounterState?.vaccinePrimaryMessage
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            allOf(
                withId(R.id.eligibility_icon),
                hasSibling(
                    allOf(
                        withId(R.id.patient_name),
                        withTextIgnoringCase(testPatient.completePatientName)
                    )
                ),
                hasSibling(
                    allOf(
                        withId(R.id.eligibility_text),
                        withText(riskMessage)
                    )
                ),
                hasSibling(
                    allOf(
                        withId(R.id.responsibility_text),
                        withText(appointment.paymentMethod.printableName)
                    )
                ),
                hasSibling(
                    allOf(
                        withId(R.id.payer_text),
                        withText(
                            appointment.patient.paymentInformation?.insuranceName ?: "Uninsured"
                        )
                    )
                )
            )
        )

        if (hasDisplayViewfinder) {
            IntegrationUtil.verifyElementsPresentOnPage(
                2,
                allOf(
                    withId(R.id.patient_empty_text),
                    withText("Scan doses using the viewfinder.")
                )
            )
        } else {
            IntegrationUtil.verifyElementsGoneOnPage(
                2,
                allOf(
                    withId(R.id.patient_empty_text),
                    withText("Scan doses using the viewfinder.")
                )
            )
        }
    }

    /**
     * proceed button is ‘not enabled and gray in color’.
     */
    fun verifyIsEnabledAndColorForProceedButton(isEnabled: Boolean) {
        onView(withId(R.id.fab_next)).check(
            matches(
                allOf(
                    isDisplayed(),
                    withBackgroundTintForFloatingActionButton(
                        if (isEnabled) R.color.primary_purple else R.color.primaryPurpleDisabled
                    )
                )
            )
        )
    }

    /**
     * Swipe Up Patient Info
     */
    fun swipePatientInfoOnScanDosesScreen(isSwipeUp: Boolean) {
        onView(
            withId(R.id.coordinator_container)
        ).perform(if (isSwipeUp) ViewActions.swipeUp() else ViewActions.swipeDown())
    }

    /**
     * Swipe Up Patient Info- Verify the following are
     * a. Patient details - hidden
     * b. Scan doses using the viewfinder.-displayed
     * c. Proceed button disabled and color gray
     */
    fun verifyPatientInfoNotOnScanDosesScreen(
        testPatient: TestPatients,
        appointment: Appointment,
        hasDisplayViewfinder: Boolean = false
    ) {
        IntegrationUtil.waitUIWithDelayed()

        onView(withId(R.id.eligibility_icon)).check(matches(not(isDisplayed())))
        onView(withId(R.id.patient_name)).check(matches(not(isDisplayed())))
        onView(withId(R.id.eligibility_text)).check(matches(not(isDisplayed())))
        onView(withId(R.id.responsibility_text)).check(matches(not(isDisplayed())))
        onView(withId(R.id.payer_text)).check(matches(not(isDisplayed())))

        if (hasDisplayViewfinder) {
            IntegrationUtil.verifyElementsPresentOnPage(
                2,
                allOf(
                    withId(R.id.patient_empty_text),
                    withText("Scan doses using the viewfinder.")
                )
            )
        } else {
            IntegrationUtil.verifyElementsGoneOnPage(
                2,
                allOf(
                    withId(R.id.patient_empty_text),
                    withText("Scan doses using the viewfinder.")
                )
            )
        }
    }

    /**
     * Prompt Unordered Dose Dialog
     *
     * @param selection “Yes, Keep Dose” or “No, Remove Dose”
     */
    fun promptUnorderedDoseDialogToSelect(selection: String) {
        IntegrationUtil.waitUIWithDelayed()
        val unorderedDoseTitle =
            context.resources.getString(R.string.orders_unordered_dose_prompt_title)
        IntegrationUtil.verifyElementToAppear(
            onView(withText(unorderedDoseTitle)).inRoot(
                isDialog()
            )
        )
        IntegrationUtil.verifyElementToAppear(onView(withText(selection)).inRoot(isDialog()))
        IntegrationUtil.delayedClick(withText(selection))
    }

    fun pregnantWeeks(weeksPregnant: String) {
        IntegrationUtil.clickBottomDialogItem(weeksPregnant)
    }

    fun confirmMedDCopayInfoAvailableDialog() {
        IntegrationUtil.confirmAlertDialog(
            titleTextResId = R.string.med_d_check_dialog_1243,
            processingTextResId = R.string.med_d_check_dialog_1243_body,
            userFriendlyName = "med D Copay Info Available",
            positiveTextResId = R.string.med_d_check_dialog_ok,
            positiveTintColor = R.color.primary_purple,
            negativeTextResId = R.string.med_d_check_dialog_cancel,
            negativeTintColor = R.color.primaryPurpleDisabled,
            selectedStringResId = R.string.med_d_check_dialog_ok
        )
    }

    /**
     * verify you land on ‘Edit Patient Info’ page.
     */
    fun verifyEditPatientInfoScreen() {
        IntegrationUtil.waitUIWithDelayed()
        val toolbarTitleText = context.resources.getString(R.string.patient_edit_title)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
    }

    fun selectPayer(insurance: String) {
        val payerSearchField = onView(
            allOf(
                withId(R.id.payer_search_et),
                childAtPosition(
                    allOf(
                        withId(R.id.view_select_payer),
                        childAtPosition(
                            withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                            2
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        payerSearchField.perform(replaceText(insurance), closeSoftKeyboard())

        onView(withId(R.id.rv_payer_search_results))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(insurance)),
                    click()
                )
            )
    }

    private fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent) && view == parent.getChildAt(position)
            }
        }
    }

    fun verifyEditPatientInformationScreen(
        screenTitle: String = "",
        insuranceCollectTitle: String = "",
        currentInfoLabel: String = "",
        currentPayerName: String = "",
        currentPayerMemberId: String = "",
        currentPayerIssueLabel: String = "",
        clickLabel: String
    ) {
        if (screenTitle.isNotEmpty()) {
            IntegrationUtil.verifyDestinationScreenByToolbarTitle(screenTitle)
        }
        if (insuranceCollectTitle.isNotEmpty()) {
            onView(withId(R.id.insurance_collect_title)).check(
                matches(
                    withText(
                        insuranceCollectTitle
                    )
                )
            )
        }
        if (currentInfoLabel.isNotEmpty()) {
            onView(withId(R.id.current_info_label)).check(matches(withText(currentInfoLabel)))
        }
        if (currentPayerName.isNotEmpty()) {
            onView(withId(R.id.current_payer_name)).check(matches(withText(currentPayerName)))
        }
        if (currentPayerMemberId.isNotEmpty()) {
            onView(withId(R.id.current_payer_member_id)).check(matches(withText(currentPayerMemberId)))
        }
        if (currentPayerIssueLabel.isNotEmpty()) {
            onView(withId(R.id.current_payer_issue_label)).check(
                matches(
                    withText(
                        currentPayerIssueLabel
                    )
                )
            )
        }
        IntegrationUtil.delayedClick(withText(clickLabel))
    }

    /**
     * Verify text “Requires a Copay” modal is displayed and Tap “Review Copay”
     */
    fun verifyReviewCopayDialogAndSelect(clickToDismiss: Boolean = false) {
        IntegrationUtil.confirmAlertDialog(
            titleTextResId = R.string.med_d_review_copay_dialog_header,
            processingTextResId = R.string.med_d_review_copay_dialog_body,
            userFriendlyName = "Requires a Copay modal dialog",
            positiveTintColor = R.color.primary_purple,
            selectedStringResId = if (clickToDismiss) R.string.med_d_review_copay_dialog_run else null
        )
    }

    fun verifyScanDosesScreen() {
        val toolbarTitleText = context.resources.getString(R.string.scan_doses)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
    }

    fun verifyCopayRequiredDialog() {
        IntegrationUtil.confirmAlertDialog(
            titleTextResId = R.string.med_d_copay_dialog_title,
            processingTextResId = R.string.med_d_copay_dialog_message,
            positiveTextResId = R.string.med_d_copay_dialog_ok,
            negativeTextResId = R.string.med_d_copay_dialog_cancel,
            positiveTintColor = R.color.primary_purple,
            userFriendlyName = "copay required dialog",
            selectedStringResId = R.string.med_d_copay_dialog_cancel
        )
    }

    /**
     * Verify ordered dose display.
     *
     * @param orderEntityList list to be displayed
     */
    fun verifyOrderedDoseDisplayed(orderEntityList: List<TestOrderDose>) {
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            Matchers.allOf(
                withId(R.id.patient_empty_text),
                withText("Scan doses using the viewfinder.")
            )
        )
        verifyOrderedDoseAddList(orderEntityList)
    }

    /**
     * Verify ordered dose disappear.
     *
     * @param orderDtoList list to be disappear
     */
    fun verifyOrderedDoseDisappear(orderDtoList: List<TestOrderDose>) {
        onView(withId(R.id.patient_empty_text)).check(matches(not(isDisplayed())))
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yy 'AT' HH:mma", Locale.US)
        for (item in orderDtoList) {
            onView(
                allOf(
                    withId(R.id.checkout_vaccine_name),
                    withText(item.shortDescription),
                    hasTextColor(R.color.gray_text)
                )
            ).check(doesNotExist())
            onView(
                allOf(
                    allOf(withId(R.id.checkout_vaccine_name), withText(item.shortDescription)),
                    hasSibling(
                        allOf(
                            withId(R.id.date),
                            withText(item.orderDate.format(formatter)),
                            hasTextColor(R.color.gray_text)
                        )
                    )
                )
            ).check(doesNotExist())
        }
    }

    /**
     * Get ordered dose list.
     */
    fun getOrderedDoseList(sql: String): List<TestOrderDose> {
        val entryPoint: CheckOutUtilEntryPoint by lazyEntryPoint()
        val orderDoseList = mutableListOf<TestOrderDose>()
        runBlocking {
            orderDoseList.addAll(
                entryPoint.orderDao().getOrdersByActivePatients(SimpleSQLiteQuery(sql)).map {
                    transformOrderDtoToTestOrderDose(OrderDto.fromOrder(it)).apply {
                        orderDate = it.orderDate
                    }
                }
            )
        }
        sortOrderedDoseList(orderDoseList)
        return orderDoseList.distinctBy { it.shortDescription.lowercase(Locale.US) }
    }

    /**
     * Sort ordered dose list.
     */
    private fun sortOrderedDoseList(items: MutableList<TestOrderDose>) {
        items.sortWith(
            compareBy(
                { it.isDeleted == true },
                { it.shortDescription.lowercase(Locale.US) }
            )
        )
    }

    /**
     * Transform OrderDtoTo entity to TestOrderDose.
     */
    private fun transformOrderDtoToTestOrderDose(orderDto: OrderDto): TestOrderDose {
        return when (orderDto.shortDescription) {
            TestOrderDose.Varicella.shortDescription -> TestOrderDose.Varicella
            else -> TestOrderDose.HibPrpT
        }
    }

    /**
     * Tap Set Reason.
     */
    fun tapSetReason() {
        IntegrationUtil.waitForElementToAppearAndClick(onView(withText("Set Reason")))
    }

    /**
     * Click on magnifying glass on view port and add a new lot
     *
     * @param testProduct Test Product used in test
     */
    fun addNewLotUsingMagnifyingGlassOnViewPort(testProduct: TestProducts): String {
        tapPurpleMagnifierIconForSelectProduct()
        addNewLot(testProduct)
        return testProduct.antigenProductName
    }

    private fun addNewLot(testProduct: TestProducts) {
        val toolbarTitleText = context.resources.getString(R.string.lot_look_up)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        val timestamp = System.currentTimeMillis().toString().substring(4, 10)

        IntegrationUtil.typeText(
            onView(withId(R.id.lot_search_et)),
            "NEWLOT85371234$timestamp"
        )

        IntegrationUtil.waitForElementToAppearAndClick(
            onView(allOf(withId(R.id.lot_row_btn), withText("Add Lot #"))),
            "Add lot' "
        )

        waitForElementAndClick("Confirm")

        // choose antigen
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(withId(R.id.select_antigen)),
            "Select Antigen' "
        )
        clickBottomDialogItem(testProduct.antigen)

        // choose product
        IntegrationUtil.delayedClick(withId(R.id.select_product))
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(testProduct.productName),
            RootMatchers.isDialog()
        )

        // choose presentation
        IntegrationUtil.delayedClick(withId(R.id.select_presentation))
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(testProduct.presentation),
            RootMatchers.isDialog()
        )

        // choose month
        IntegrationUtil.delayedClick(withId(R.id.expiration_month))
        swipeBottomSheetDialogToExpand()
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(testProduct.expiryMonth),
            RootMatchers.isDialog()
        )

        // choose day
        IntegrationUtil.delayedClick(withId(R.id.expiration_day))
        swipeBottomSheetDialogToExpand()
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(testProduct.expiryDay),
            RootMatchers.isDialog()
        )

        IntegrationUtil.simpleClick(onView(withId(R.id.expiration_year)), "close icon")
        swipeBottomSheetDialogToExpand()
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(testProduct.expiryYear),
            RootMatchers.isDialog()
        )
        IntegrationUtil.simpleClick(onView(withId(R.id.button_confirm)), "Continue button")
    }

    /**
     * select item on bottom dialog.
     * @param itemValue select item text res id
     */
    fun clickBottomDialogItem(itemValue: String) {
        Thread.sleep(5000)
        swipeBottomSheetDialogToExpand()
        onView(withId(R.id.rv_bottom)).inRoot(RootMatchers.isDialog()).perform(
            RecyclerViewActions.scrollTo<BottomDialog.BottomDialogHolder>(
                hasDescendant(
                    withText(itemValue)
                )
            ),
            RecyclerViewActions.actionOnItem<BottomDialog.BottomDialogHolder>(
                hasDescendant(withText(itemValue)),
                click()
            )
        )
    }

    /**
     * Wait for element
     *
     * @param selection “Keep Dose” or “Remove Dose”
     */
    fun waitForElementAndClick(selection: String) {
        IntegrationUtil.waitUIWithDelayed(5000)
        IntegrationUtil.delayedClick(withText(selection))
    }
}
