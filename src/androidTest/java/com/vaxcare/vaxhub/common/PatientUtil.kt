/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.EntryPointHelper.lazyEntryPoint
import com.vaxcare.vaxhub.HiltEntryPointInterface
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.WAIT_TIME_DEFAULT
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.WAIT_TIME_FOR_LOAD
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.swipeBottomSheetDialogToExpand
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.data.TestOrderDose
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestSearch
import com.vaxcare.vaxhub.data.dao.OrderDao
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.MedDCheckRequestBody
import com.vaxcare.vaxhub.model.MedDCheckResponse
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.order.OrderEntity
import com.vaxcare.vaxhub.model.order.OrderJson
import com.vaxcare.vaxhub.web.PatientsApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Calendar

@EntryPoint
@InstallIn(ActivityComponent::class)
interface PatientUtilEntryPoint : HiltEntryPointInterface {
    fun patientsApi(): PatientsApi

    fun localStorage(): LocalStorage

    fun orderDao(): OrderDao
}

class PatientUtil : TestUtilBase() {
    private val context by lazy { ApplicationProvider.getApplicationContext<Context>() }
    private val entryPoint: PatientUtilEntryPoint by lazyEntryPoint()

    companion object {
        const val FAKE_SSN = "123121234"
    }

    /**
     * Enter a patient name and verify search results should narrow as more letters are entered
     */
    fun verifyEnterPendingSearchPatientText() {
        // Enter three letter
        IntegrationUtil.typeText(
            onView(withId(R.id.appointment_search_et)),
            TestSearch.FirstEnter.letter
        )
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "LoadingForFirstSearch",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.waitForOperationComplete(2)
        // Vaccine row elements are present
        val rowsByFirstEnter: Int =
            IntegrationUtil.getCountFromRecyclerView(withId(R.id.rv_patient_search_results))

        // Clear letters
        IntegrationUtil.clearText(onView(withId(R.id.appointment_search_et)))
        IntegrationUtil.waitForOperationComplete(1)
        IntegrationUtil.verifyElementsNotPresentOnPage(
            0,
            withId(R.id.patient_name)
        )

        // Enter more letters
        IntegrationUtil.typeText(
            onView(withId(R.id.appointment_search_et)),
            TestSearch.SecondEnter.letter
        )
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "LoadingForSecondSearch",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.waitForOperationComplete(2)
        // Vaccine row elements are present
        val rowsBySecondEnter: Int =
            IntegrationUtil.getCountFromRecyclerView(withId(R.id.rv_patient_search_results))
        Assert.assertTrue(
            "Expected to search results should narrow as more letters are entered. " +
                "Number of rows second time - $rowsBySecondEnter must be less than " +
                "or equal to rows got the first time - $rowsBySecondEnter",
            rowsBySecondEnter <= rowsByFirstEnter
        )
    }

    /**
     * Search for a patient and Select the patient to create an appointment
     */
    fun searchAndSelectRiskFreePatient(testPatient: TestPatients) {
        IntegrationUtil.waitForElementToDisplayed(onView(withId(R.id.editText_searchAppointment)))
        IntegrationUtil.typeText(
            onView(withId(R.id.editText_searchAppointment)),
            testPatient.completePatientName
        )
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "LoadingForSearch",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.waitForElementInRecyclerView(
            withId(R.id.recyclerView_patientsFound),
            "SearchProductResult",
            Matchers.allOf(
                withId(R.id.patient_name),
                withTextIgnoringCase(testPatient.completePatientName)
            )
        )
        IntegrationUtil.waitForOperationComplete()
        val recyclerViewPatientsFound = onView(
            allOf(
                withId(R.id.click_area),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.recyclerView_patientsFound),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        recyclerViewPatientsFound.perform(click())
    }

    fun verifyAndClickToConfirmPatientInfoToCreate(testPatient: TestPatients) {
        val toolbarTitleText =
            context.resources.getString(R.string.patient_confirm_patient_info)
//        IntegrationUtil.verifyElementsPresentOnPage(
//            IntegrationUtil.WAIT_TIME_DEFAULT,
//            Matchers.allOf(withId(R.id.toolbar_title), withText(toolbarTitleText)),
//            Matchers.allOf(
//                withId(R.id.patient_name),
//                withTextIgnoringCase(testPatient.completePatientName)
//            )
//        )
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.button_ok)),
            "Confirm Button"
        )
    }

    // Verify that the eligibility check spinner takes < maxLoadTime
    fun verifyCreatePatientScreenLoading(maxLoadTime: Int) {
        val showText =
            context.resources.getString(R.string.patient_add_in_progress)
        val realTime = IntegrationUtil.returnSecondToWaitElementFromAppearToDisappear(
            onView(withText(showText)),
            "LoadingForCreate",
            WAIT_TIME_FOR_LOAD
        )
        Timber.d("Loading $realTime for Create for ")
        Assert.assertTrue(
            "Verify that the eligibility check spinner takes < $maxLoadTime seconds is ${realTime < maxLoadTime}",
            realTime < maxLoadTime
        )
    }

    /**
     * This is create a new patient (and appointment) that can be used in subsequent test runs.
     *
     * By default an appointment will be generated, however it will be in the past. Goal is to leverage
     * the patient created by this flow rather than the appointment
     *
     * @param patient - Test patient that will be created
     */
    fun createTestPatient(patient: TestPatients, daysBefore: Int = 30) {
        // visit date 30 days in the past; we don't care about the visit, just the patient
        val visitDate =
            LocalDateTime.of(LocalDate.now().minusDays(daysBefore.toLong()), LocalTime.of(12, 30))
        val patientPostBody = generatePatientPostBody(patient, visitDate)
        GlobalScope.safeLaunch {
            entryPoint.patientsApi().postAppointment(patientPostBody)
        }
    }

    suspend fun createTestPatientAndAppointment(patient: TestPatients): String {
        return entryPoint.patientsApi().postAppointment(generatePatientPostBody(patient, LocalDateTime.now()))
    }

    private fun generatePatientPostBody(patient: TestPatients, visitDate: LocalDateTime): PatientPostBody {
        return PatientPostBody(
            newPatient = PatientPostBody.NewPatient(
                firstName = patient.firstName,
                lastName = patient.lastName,
                dob = patient.dateOfBirth,
                gender = patient.gender,
                // Phone is required via patients API
                phoneNumber = "1234567890",
                address1 = null,
                address2 = null,
                city = null,
                state = "FL",
                zip = null,
                paymentInformation = PatientPostBody.PaymentInformation(
                    primaryInsuranceId = patient.primaryInsuranceId,
                    primaryMemberId = patient.primaryMemberId,
                    primaryGroupId = patient.primaryGroupId,
                    uninsured = false
                ),
                race = null,
                ethnicity = null,
                ssn = patient.ssn
            ),
            clinicId = entryPoint.localStorage().currentClinicId,
            date = visitDate,
            providerId = 0,
            initialPaymentMode = patient.paymentMode,
            visitType = "Well"
        )
    }

    /**
     * This is create a new patient (and appointment) that can be used in subsequent test runs.
     *
     * By default an appointment will be generated, however it will be in the past. Goal is to leverage
     * the patient created by this flow rather than the appointment
     *
     * @param patient - Test patient that will be created
     */
    fun getAppointmentIdByCreateTestPatient(patient: TestPatients, pastDays: Long = 0): String =
        runBlocking {
            val visitDate =
                LocalDateTime.of(LocalDate.now().minusDays(pastDays), LocalTime.of(12, 30))
            val patientPostBody = generatePatientPostBody(patient, visitDate)
            entryPoint.patientsApi().postAppointment(patientPostBody)
        }

    /**
     * Get AppointmentDetailDto by appointmentId
     * we can use this object to verify "callToAction"
     *
     * @param appointmentId
     * @return AppointmentDetailDto
     */
    fun getAppointmentById(appointmentId: String) =
        runBlocking {
            appointmentId.toIntOrNull()?.let {
                entryPoint.patientsApi().getAppointmentById(it).body()
            }
        }

    /**
     * Tap the Create New Patient button to create a new patient
     */
    fun tapCreateNewPatientButton() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.button_createNewPatient)),
            "Create New patient Button"
        )
        IntegrationUtil.delayedClick(withId(R.id.button_createNewPatient))
    }

    /**
     * Tap the understand button to dismiss keep device in hand dialog
     */
    fun tapUnderstandToDismissDialog() {
        IntegrationUtil.waitForElementToAppearAndEnabled(
            onView(withId(R.id.button_ok)),
            "I Understand Button"
        )
        IntegrationUtil.delayedClick(withId(R.id.button_ok))
    }

    /**
     * User enters demographic information for create a new patient
     */
    fun enterDemographicInformationForCreatePatient(testPatient: TestPatients) {
        val yearOfBirth = testPatient.dateOfBirth.year // make the patient ten years old
        val monthOfBirth = 0 // January
        val dayOfBirth = "1" // first day of month
        val phoneStart = "123"
        val phoneMid = "456"
        val phoneEnd = "7890"
        val gender = "Male"
        val race = "White"
        val ethnicity = "Hispanic or Latino Spanish"
        val insurances = "Humana"
        val monthSimplyArray =
            context.resources.getStringArray(R.array.array_month)

        // input firstName
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.patient_add_first_name)),
            testPatient.firstName
        )

        // input firstName
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.patient_add_last_name)),
            testPatient.lastName
        )

        // input birthday
        IntegrationUtil.delayedClick(withId(R.id.date_of_birth_month), 2000)
        swipeBottomSheetDialogToExpand()
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(Matchers.containsString(monthSimplyArray[monthOfBirth])),
            RootMatchers.isDialog()
        )
        swipeBottomSheetDialogToExpand()
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(dayOfBirth),
            RootMatchers.isDialog()
        )
        swipeYearBottomSheetDialogToExpand(1)
        IntegrationUtil.waitUIWithDelayed()
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(Matchers.containsString(yearOfBirth.toString())),
            RootMatchers.isDialog()
        )

        // input phone number
        IntegrationUtil.delayedClick(withId(R.id.patient_add_phone_start))
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.patient_add_phone_start)),
            phoneStart
        )
        IntegrationUtil.delayedClick(withId(R.id.patient_add_phone_mid))
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.patient_add_phone_mid)),
            phoneMid
        )
        IntegrationUtil.delayedClick(withId(R.id.patient_add_phone_end))
        IntegrationUtil.typeTextWithCloseSoftKeyboard(
            onView(withId(R.id.patient_add_phone_end)),
            phoneEnd
        )

        // choose gender
        IntegrationUtil.delayedClick(withId(R.id.patient_add_gender))
        IntegrationUtil.checkElementAppearAndClickWithRoot(
            withText(Matchers.containsString(gender)),
            RootMatchers.isDialog()
        )

        // swipe screen to show more information
        swipeEditPatientView()

        IntegrationUtil.delayedClick(withId(R.id.fab_next))
        IntegrationUtil.delayedClick(withId(R.id.capture_not_catch))
        createTestPatient(testPatient)
        IntegrationUtil.waitUIWithDelayed(4000)
        onView(withId(R.id.rv_payer_search_results))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(insurances)),
                    click()
                )
            )
        IntegrationUtil.delayedClick(withId(R.id.capture_not_catch))
        IntegrationUtil.delayedClick(withId(R.id.button_ok))
    }

    /**
     * Update birth year
     */
    fun updateInfoForInvalidPatient(testPatient: TestPatients) {
        val yearOfBirth = Calendar.getInstance().get(Calendar.YEAR) - 65
        IntegrationUtil.delayedClick(withId(R.id.date_of_birth_year), 2000)
        clickBottomDialogItem(yearOfBirth.toString())
        IntegrationUtil.delayedClick(withId(R.id.fab_next))
    }

    fun waitForViewToGone(
        resId: Int,
        userFriendlyName: String,
        additionalTime: Int = WAIT_TIME_DEFAULT
    ) {
        IntegrationUtil.waitForElementToGone(
            elementToCheck = onView(withId(resId)),
            userFriendlyName = userFriendlyName,
            secondsToWait = additionalTime
        )
    }

    /**
     * select item on bottom dialog.
     * @param itemStringIds select item text res id
     */
    fun clickBottomDialogItem(itemValue: String) {
        IntegrationUtil.swipeBottomSheetDialogToExpand()
        onView(withId(R.id.rv_bottom)).inRoot(RootMatchers.isDialog()).perform(
            RecyclerViewActions.scrollTo<BottomDialog.BottomDialogHolder>(
                ViewMatchers.hasDescendant(
                    withText(itemValue)
                )
            ),
            RecyclerViewActions.actionOnItem<BottomDialog.BottomDialogHolder>(
                ViewMatchers.hasDescendant(withText(itemValue)),
                ViewActions.click()
            )
        )
    }

    /**
     * select item on bottom dialog.
     * @param itemStringIds select item text res id
     */
    fun clickPayerFromBottomItem(itemValue: String) {
        IntegrationUtil.swipeBottomSheetDialogPayerSearchToExpand()
        onView(withId(R.id.rv_payer_search_results)).inRoot(RootMatchers.isDialog()).perform(
            RecyclerViewActions.scrollTo<BottomDialog.BottomDialogHolder>(
                ViewMatchers.hasDescendant(
                    withText(itemValue)
                )
            ),
            RecyclerViewActions.actionOnItem<BottomDialog.BottomDialogHolder>(
                ViewMatchers.hasDescendant(withText(itemValue)),
                ViewActions.click()
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

    fun closeCheckoutPage() {
        IntegrationUtil.waitForElementToGone(
            onView(withId(R.id.loading)),
            "LoadingForSecondSearch",
            WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.waitForElementToAppear(
            onView(withId(R.id.toolbar_icon)),
            "PurpleMagnifierIcon"
        )
        IntegrationUtil.delayedClick(withId(R.id.toolbar_icon))
    }

    /**
     * verify special patient on the schedule grid
     */
    fun verifySpecialPatientOnScheduleGrid(testPatient: TestPatients) {
        IntegrationUtil.waitUIWithDelayed()
        onView(withId(R.id.recycler_view)).check(
            ViewAssertions.matches(
                hasDescendant(
                    withTextIgnoringCase(testPatient.completePatientName)
                )
            )
        )
    }

    /**
     * Expand bottomSheet Dialog and swipe the recyclerView several times
     */
    private fun swipeYearBottomSheetDialogToExpand(repeat: Int) {
        repeat(repeat) {
            onView(withId(R.id.rv_bottom)).inRoot(RootMatchers.isDialog()).perform(
                RecyclerViewActions.actionOnItemAtPosition<BottomDialog.BottomDialogHolder>(
                    it,
                    ViewActions.swipeUp()
                )
            )
            IntegrationUtil.waitUIWithDelayed(1000)
        }
    }

    /**
     * Swipe edit patient view to show more information.
     */
    private fun swipeEditPatientView() {
        repeat(15) {
            onView(withId(R.id.patient_add_gender)).perform(ViewActions.swipeUp())
        }
        IntegrationUtil.waitUIWithDelayed(1000)
    }

    suspend fun getMedDCopays(appointmentId: Int): MedDCheckResponse {
        return entryPoint.patientsApi().getMedDCopays(appointmentId)
    }

    suspend fun runMedDCheck(appointmentId: Int) {
        val body = MedDCheckRequestBody(FAKE_SSN, null)
        entryPoint.patientsApi().doMedDCheck(appointmentId, body)
    }

    suspend fun getAppointmentList(): List<Appointment> {
        return entryPoint.patientsApi().getClinicAppointmentsByDate(
            entryPoint.localStorage().currentClinicId.toInt(),
            LocalDate.now()
        )
            .map { it.toAppointment() }.sortedBy { it.appointmentTime }
    }

    fun selectNoInsuranceCardInEditPatientInfoScreen() {
        IntegrationUtil.delayedClick(withText(R.string.patient_edit_no_insurance_card))
    }

    /**
     * Get appointment  detail by appointmentId and fake order dose.
     *
     * @param appointmentId - patient to create dose
     * @param testPartners - Test patient to create dose
     */
    fun fakeOrderedDoseForSpecificPatient(
        testPartners: TestPartners,
        appointmentId: Int,
        testOrderDose: List<TestOrderDose>
    ) {
        runBlocking {
            val appointmentDetailDto = entryPoint.patientsApi().getAppointmentById(appointmentId)
            val patientId = appointmentDetailDto.body()?.patient?.id ?: 0
            fakeOrderedDose(testPartners, patientId, testOrderDose)
        }
    }

    /**
     * Fake a ordered dose with patientId.
     *
     * @param patientId - patient to create dose
     * @param testPartners - Test patient to create dose
     */
    private suspend fun fakeOrderedDose(
        testPartners: TestPartners,
        patientId: Int,
        testOrderDoseList: List<TestOrderDose>
    ) {
        val orderDoseList = mutableListOf<OrderJson>()
        for (item in testOrderDoseList) {
            orderDoseList.add(
                OrderJson(
                    item.orderId,
                    testPartners.partnerID.toInt(),
                    testPartners.clinicID.toInt(),
                    item.patientVisitId,
                    patientId,
                    item.isDeleted,
                    item.shortDescription,
                    item.orderNumber,
                    item.satisfyingProductIds,
                    item.serverSyncDateTimeUtc,
                    item.durationInDays,
                    item.expirationDate,
                    item.orderDate
                )
            )
        }
        entryPoint.orderDao().upsert(orderDoseList.map { OrderEntity.fromOrder(it) })
    }
}
