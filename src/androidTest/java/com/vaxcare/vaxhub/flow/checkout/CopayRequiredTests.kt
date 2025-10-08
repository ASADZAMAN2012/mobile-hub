/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import android.content.Context
import android.content.res.Resources
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.common.robot.appointmentlist.appointmentListScreen
import com.vaxcare.vaxhub.common.robot.checkout.AppointmentInfo
import com.vaxcare.vaxhub.common.robot.checkout.CheckoutRobot
import com.vaxcare.vaxhub.common.robot.checkout.checkoutDosesScreen
import com.vaxcare.vaxhub.common.robot.checkout.complete.checkoutCompleteScreen
import com.vaxcare.vaxhub.common.robot.checkout.copay.medDCopayScreen
import com.vaxcare.vaxhub.common.robot.checkout.copayRequiredDialog
import com.vaxcare.vaxhub.common.robot.checkout.reviewdoses.reviewDosesScreen
import com.vaxcare.vaxhub.common.robot.checkout.summary.checkoutDosesSummaryScreen
import com.vaxcare.vaxhub.common.robot.checkout.summary.collectPaymentScreen
import com.vaxcare.vaxhub.common.robot.checkout.summary.copayDialogRequiredDialog
import com.vaxcare.vaxhub.common.robot.checkout.summary.paymentSummaryScreen
import com.vaxcare.vaxhub.common.robot.checkout.summary.signatureCaptureScreen
import com.vaxcare.vaxhub.common.robot.checkout.unorderedDoseDialog
import com.vaxcare.vaxhub.common.robot.home.homeScreen
import com.vaxcare.vaxhub.common.robot.home.splashScreen
import com.vaxcare.vaxhub.data.PaymentModals
import com.vaxcare.vaxhub.data.TestBackground
import com.vaxcare.vaxhub.data.TestCards
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.data.TestStockPill
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.flow.checkout.mock.dispatcher.CheckoutDispatcher
import com.vaxcare.vaxhub.mock.util.usecase.checkout.CheckoutUseCases
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.MedDCheckResponse
import com.vaxcare.vaxhub.ui.PermissionsActivity
import com.vaxcare.vaxhub.ui.idlingresource.HubIdlingResource
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class CopayRequiredTests : TestsBase() {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var storageUtil: StorageUtil

    @Inject
    lateinit var listener: HiltWorkManagerListener

    private val testWorkManagerHelper = TestWorkManagerHelper()

    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val homeScreenUtil = HomeScreenUtil()
    private val patientUtil = PatientUtil()

    private val idlingResource: IdlingResource? = HubIdlingResource.instance
    private val resources: Resources
        get() = ApplicationProvider.getApplicationContext<Context>().resources

    companion object {
        const val MEDD_CTA_NOTRUN_MSG = "MED D: RUN COPAY CHECK TO DETERMINE PATIENT RESPONSIBILITY"
        const val MEDD_CTA_RAN_MSG = "MED D: COPAY REQUIRED FOR TDAP, ZOSTER AND RSV"
        const val MEDB_PAYER_MSG = "Medicare B (Flu, Pneumovax and Prevnar vaccines ONLY)"
        const val LIMITED_COVERAGE_CTA = "LIMITED COVERAGE: FLU, PNEUMONIA, AND COVID ONLY"
        const val MAX_REFRESH_ATTEMPTS = 5

        const val MAX_SUBMIT_LOAD_TIME = 5
        const val COPAY_REQUIRED_YES = "Yes, Review Copay"
        const val COPAY_REQUIRED_NO = "No, Skip This"
        const val COPAY_REVIEW = "Review Copay"

        const val COPAY_TESTS_JSON_FILES_DIRECTORY = "CopayRequiredTests/"
    }

    @Inject
    lateinit var checkoutUseCases: CheckoutUseCases

    @Before
    fun beforeTests() {
        hiltRule.inject()
        testWorkManagerHelper.initializeWorkManager(workerFactory, listener)
        scenario = ActivityScenario.launch(PermissionsActivity::class.java)
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun afterTests() {
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().unregister(idlingResource)
        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer.shutdown()
        }
    }

    @Test
    fun runCoPayAndCheckMedDCTAMessages_test() {
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            CopayRequiredTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "runCoPayAndCheckMedDCTAMessages_test"
            )
        )
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        homeScreenUtil.loginAsTestPartner(testPartner)
        var medDCheckResponse: MedDCheckResponse? = null
        var appointmentId = ""

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
                appointmentId = createAppointmentWithPatient(testPatient)
            }
        }

        appointmentListScreen {
            verifyTitleAndAppointmentList()
            searchForCreatedAppointment(
                testPatient = testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )
            appointmentId =
                appointmentId.ifEmpty { getAppointmentIdFromPatientName(testPatient).toString() }
            assert(appointmentId != "0") {
                "AppointmentId not found for ${testPatient.completePatientName}"
            }
            verifyCorrectMedDCallToActionInAppointmentData(appointmentId, CallToAction.MedDCanRun)
            tapFirstAppointmentWithPatient(testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyMedDCTAText(MEDD_CTA_NOTRUN_MSG)
            tapRunCopayCheckButton()
            medDCopayScreen {
                verifyAndFillInSSNIfNeeded()
                waitAndVerifyResultsTitle()
                verifyResultsHeaderText()

                medDCheckResponse = runBlocking {
                    patientUtil.getMedDCopays(appointmentId.toInt())
                }

                verifyCopayResults(
                    copays = medDCheckResponse?.copays ?: emptyList(),
                    resources = resources
                )
                tapNextButton()
            }

            verifyTitle()
            verifyMedDCTAText(MEDD_CTA_RAN_MSG)
            tapCloseAppointmentButton()
        }

        appointmentListScreen {
            waitForEligibilityToSettle()
            verifyTitleAndAppointmentList()
            tapRefreshAppointmentsButton()
            tapFirstAppointmentWithPatient(testPatient)

            checkoutDosesScreen {
                waitForProgressBarToGone()
                verifyCorrectMedDCallToActionInAppointmentData(
                    appointmentId = appointmentId,
                    expectedCallToAction = CallToAction.MedDCollectCreditCard
                )
                verifyMedDCTAText(MEDD_CTA_RAN_MSG)
            }
        }
    }

    @Test
    fun medDCoPayWithCashOrCheck_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        registerMockServerDispatcher(
            CopayRequiredTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "medDCoPayWithCashOrCheck_test"
            )
        )
        homeScreenUtil.loginAsTestPartner(testPartner)
        val testProducts = TestProducts.Boostrix
        val testSite = TestSites.LeftArm
        var medDCheckResponse: MedDCheckResponse? = null
        var appointmentId = ""

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
                appointmentId = createAppointmentWithPatient(testPatient)
            }
        }

        appointmentListScreen {
            verifyTitleAndAppointmentList()
            searchForCreatedAppointment(
                testPatient = testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )
            appointmentId =
                appointmentId.ifEmpty { getAppointmentIdFromPatientName(testPatient).toString() }
            assert(appointmentId != "0") {
                "AppointmentId not found for ${testPatient.completePatientName}"
            }
            verifyAppointmentIconAndTags(
                testPatient = testPatient,
                expectedRiskIcon = RiskIconConstant.RiskFreeIcon,
                stockPillList = listOf(TestStockPill.PrivateStockPill, TestStockPill.MedDTag)
            )
            tapFirstAppointmentWithPatient(testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyTitle()
            verifyAppointmentInfo(
                AppointmentInfo(
                    testPatient = testPatient,
                    riskIconConstant = RiskIconConstant.RiskFreeIcon,
                    eligibilityText = "Ready to Vaccinate",
                    responsibilityText = "VaxCare Bill",
                    payerText = MEDB_PAYER_MSG,
                    eligibilityCtaText = LIMITED_COVERAGE_CTA,
                    medDCtaText = MEDD_CTA_NOTRUN_MSG,
                    isMedDTagShow = true,
                    hasMedDRan = false
                )
            )

            tapRunCopayCheckButton()
            medDCopayScreen {
                verifyAndFillInSSNIfNeeded()
                waitAndVerifyResultsTitle()
                verifyResultsHeaderText()

                medDCheckResponse = runBlocking {
                    patientUtil.getMedDCopays(appointmentId.toInt())
                }

                verifyCopayResults(
                    copays = medDCheckResponse!!.copays,
                    resources = resources
                )
                tapNextButton()
            }

            verifyAppointmentInfo(
                AppointmentInfo(
                    testPatient = testPatient,
                    riskIconConstant = RiskIconConstant.RiskFreeIcon,
                    eligibilityText = "Ready to Vaccinate",
                    responsibilityText = "VaxCare Bill",
                    payerText = MEDB_PAYER_MSG,
                    eligibilityCtaText = LIMITED_COVERAGE_CTA,
                    medDCtaText = MEDD_CTA_RAN_MSG,
                    isMedDTagShow = true,
                    hasMedDRan = true
                )
            )

            verifyMedDCopaysDisplayed(medDCheckResponse!!.copays)
            addDose(testProducts)
            unorderedDoseDialog {
                tapKeepDoseDialogButton()
            }

            selectSiteForDose(testProducts, testSite)
            verifyProductGrid(listOf(testProducts), testSite)
            tapArrowToCheckoutSummary()

            reviewDosesScreen {
                selectUnorderedReasonDoseNotAppearingForAll()
                tapArrowToCheckoutSummary()
            }

            checkoutDosesSummaryScreen {
                verifyTitle()
                verifyCopaySubTotalAndAddedDose(
                    testPartners = testPartner,
                    testPatient = testPatient,
                    testProducts = listOf(testProducts),
                    testSites = testSite,
                    medDCheckResponse = medDCheckResponse
                )

                tapCollectPaymentInfo()

                copayDialogRequiredDialog {
                    tapCashOrCheck()
                }

                paymentSummaryScreen {
                    verifyTitle()
                    verifyCopaySubTotalAndAddedDose(
                        testPatient = testPatient,
                        testProducts = listOf(testProducts),
                        paymentModal = PaymentModals.PaymentCashOrCheck,
                        medDCheckResponse = medDCheckResponse!!
                    )
                    tapSignatureCaptureButton()
                }

                signatureCaptureScreen {
                    verifyTitle()
                    verifySubmitButtonDisabled()
                    drawSignature()
                    tapSignatureSubmitButton()
                }

                IntegrationUtil.waitForOperationComplete()
                checkoutCompleteScreen {
                    verifyTitle()
                    verifyPatientName(testPatient)
                    verifyNumberOfShotsAdministered(1, resources)
                    tapCheckoutAnotherPatient()
                }
            }
        }

        // POST checkout: IT is going to fail here: The EncounterState is STILL "PreShot" until more time passes.
//        verifyPostShotEligibility(
//            AppointmentInfo(
//                testPatient = testPatient,
//                riskIconConstant = RiskIconConstant.PartnerBillIcon(),
//                eligibilityText = "Ready to Bill",
//                responsibilityText = "Partner Bill",
//                payerText = MEDB_PAYER_MSG,
//                eligibilityCtaText = "",
//                medDCtaText = "",
//                isMedDTagShow = true,
//                hasMedDRan = true
//            ),
//            medDCheckResponse
//        )
    }

    @Test
    fun medDCoPayWithDebitOrCredit_test() {
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            CopayRequiredTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "medDCoPayWithDebitOrCredit_test"
            )
        )
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        homeScreenUtil.loginAsTestPartner(testPartner)
        val testProducts = TestProducts.Boostrix
        val testSite = TestSites.LeftArm
        val testCardInfo = TestCards.ILabsCard
        var medDCheckResponse: MedDCheckResponse? = null
        var appointmentId = ""

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
                appointmentId = createAppointmentWithPatient(testPatient)
            }
        }

        appointmentListScreen {
            verifyTitleAndAppointmentList()
            searchForCreatedAppointment(
                testPatient = testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )
            appointmentId =
                appointmentId.ifEmpty { getAppointmentIdFromPatientName(testPatient).toString() }
            assert(appointmentId != "0") {
                "AppointmentId not found for ${testPatient.completePatientName}"
            }
            verifyAppointmentIconAndTags(
                testPatient = testPatient,
                expectedRiskIcon = RiskIconConstant.RiskFreeIcon,
                stockPillList = listOf(TestStockPill.PrivateStockPill, TestStockPill.MedDTag)
            )
            tapFirstAppointmentWithPatient(testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyTitle()
            verifyAppointmentInfo(
                AppointmentInfo(
                    testPatient = testPatient,
                    riskIconConstant = RiskIconConstant.RiskFreeIcon,
                    eligibilityText = "Ready to Vaccinate",
                    responsibilityText = "VaxCare Bill",
                    payerText = MEDB_PAYER_MSG,
                    eligibilityCtaText = LIMITED_COVERAGE_CTA,
                    medDCtaText = MEDD_CTA_NOTRUN_MSG,
                    isMedDTagShow = true,
                    hasMedDRan = false
                )
            )
            tapRunCopayCheckButton()
            medDCopayScreen {
                verifyAndFillInSSNIfNeeded()
                waitAndVerifyResultsTitle()
                verifyResultsHeaderText()

                medDCheckResponse = runBlocking {
                    patientUtil.getMedDCopays(appointmentId.toInt())
                }

                verifyCopayResults(
                    copays = medDCheckResponse?.copays ?: emptyList(),
                    resources = resources
                )
                tapNextButton()
            }

            verifyMedDCopaysDisplayed(medDCheckResponse?.copays ?: emptyList())
            addDose(testProducts)
            unorderedDoseDialog {
                tapKeepDoseDialogButton()
            }

            selectSiteForDose(testProducts, testSite)
            verifyProductGrid(listOf(testProducts), testSite)
            tapArrowToCheckoutSummary()

            reviewDosesScreen {
                selectUnorderedReasonDoseNotAppearingForAll()
                tapArrowToCheckoutSummary()
            }

            checkoutDosesSummaryScreen {
                verifyTitle()
                verifyCopaySubTotalAndAddedDose(
                    testPartners = testPartner,
                    testPatient = testPatient,
                    testProducts = listOf(testProducts),
                    testSites = testSite,
                    medDCheckResponse = medDCheckResponse
                )

                tapCollectPaymentInfo()
                copayDialogRequiredDialog {
                    tapDebitOrCredit()
                }

                collectPaymentScreen {
                    verifyTitle()
                    verifyPhonePrefilled()
                    enterCardNumber(testCardInfo.cardNumber)
                    enterCardExpiration(testCardInfo.expMonth, testCardInfo.expYear)
                    scrollDown()
                    enterNameOnCard(testCardInfo.nameOnCard)
                    enterPhoneNumber(
                        testCardInfo.phoneNumberFirst,
                        testCardInfo.phoneNumberMid,
                        testCardInfo.phoneNumberLast
                    )
                    enterEmailAddress(testCardInfo.emailAddress)
                    tapSavePaymentInfo()
                }

                paymentSummaryScreen {
                    verifyTitle()
                    waitForSeconds(5)
                    verifyCopaySubTotalAndAddedDose(
                        testPatient = testPatient,
                        testProducts = listOf(testProducts),
                        paymentModal = PaymentModals.PaymentDebitOrCredit(),
                        medDCheckResponse = medDCheckResponse!!,
                        cardInfo = testCardInfo
                    )
                    tapSignatureCaptureButton()
                }

                signatureCaptureScreen {
                    verifyTitle()
                    verifySubmitButtonDisabled()
                    drawSignature()
                    tapSignatureSubmitButton()
                }

                IntegrationUtil.waitForOperationComplete()
                checkoutCompleteScreen {
                    verifyTitle()
                    verifyPatientName(testPatient)
                    verifyNumberOfShotsAdministered(1, resources)
                    tapCheckoutAnotherPatient()
                }
            }
        }
        // POST checkout: IT is going to fail here: The EncounterState is STILL "PreShot" until more time passes.
//        verifyPostShotEligibility(
//            appointmentInfo = AppointmentInfo(
//                testPatient = testPatient,
//                riskIconConstant = RiskIconConstant.RiskFreeIcon,
//                eligibilityText = "Guaranteed Payment",
//                responsibilityText = "VaxCare Bill",
//                payerText = MEDB_PAYER_MSG,
//                eligibilityCtaText = "",
//                medDCtaText = "",
//                isMedDTagShow = true,
//                hasMedDRan = true
//            ), medDCheckResponse = medDCheckResponse
//        )
    }

    @Ignore("This test requires a legacy COVID product which have been disabled")
    @Test
    fun medDCheckoutWithMedDCheckPromptedAfterEnteringTdapDoseAndValidatePostCheckout_test() {
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        val testBoostrixProduct = TestProducts.Boostrix
        val testModernaProduct = TestProducts.ModernaCovid
        val testSite = TestSites.LeftArm
        val testCardInfo = TestCards.ILabsCard
        var medDCheckResponse: MedDCheckResponse? = null
        var appointmentId = ""

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
                appointmentId = createAppointmentWithPatient(testPatient)
            }
        }

        appointmentListScreen {
            verifyTitleAndAppointmentList()
            searchForCreatedAppointment(
                testPatient = testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )
            appointmentId =
                appointmentId.ifEmpty { getAppointmentIdFromPatientName(testPatient).toString() }
            assert(appointmentId != "0") {
                "AppointmentId not found for ${testPatient.completePatientName}"
            }
            verifyAppointmentIconAndTags(
                testPatient = testPatient,
                expectedRiskIcon = RiskIconConstant.RiskFreeIcon,
                stockPillList = listOf(TestStockPill.PrivateStockPill, TestStockPill.MedDTag)
            )
            tapFirstAppointmentWithPatient(testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyTitle()
            verifyAppointmentInfo(
                AppointmentInfo(
                    testPatient = testPatient,
                    riskIconConstant = RiskIconConstant.RiskFreeIcon,
                    eligibilityText = "Ready to Vaccinate",
                    responsibilityText = "VaxCare Bill",
                    payerText = MEDB_PAYER_MSG,
                    eligibilityCtaText = LIMITED_COVERAGE_CTA,
                    medDCtaText = MEDD_CTA_NOTRUN_MSG,
                    isMedDTagShow = true,
                    hasMedDRan = false
                )
            )
            addDose(testBoostrixProduct)
            unorderedDoseDialog {
                tapKeepDoseDialogButton()
            }
            copayRequiredDialog {
                verifyCopayDialog()
                verifyViewDisableWhenBehindDialog()
                tapRunCopayDialogButton()
            }
            medDCopayScreen {
                verifyAndFillInSSNIfNeeded()
                waitAndVerifyResultsTitle()
                verifyResultsHeaderText()

                medDCheckResponse = runBlocking {
                    patientUtil.getMedDCopays(appointmentId.toInt())
                }

                verifyCopayResults(
                    copays = medDCheckResponse?.copays ?: emptyList(),
                    resources = resources
                )
                tapNextButton()
            }
            verifyMedDCopaysDisplayed(medDCheckResponse?.copays ?: emptyList())

            selectSiteForDose(testBoostrixProduct, testSite)
            verifyProductGrid(listOf(testBoostrixProduct), testSite)

            addDoseAndSelectSite(testModernaProduct, testSite)
            verifyProductGrid(listOf(testModernaProduct), testSite)
            tapArrowToCheckoutSummary()

            reviewDosesScreen {
                selectUnorderedReasonDoseNotAppearingForAll()
                tapArrowToCheckoutSummary()
            }

            checkoutDosesSummaryScreen {
                verifyTitle()
                verifyCopaySubTotalAndAddedDose(
                    testPartners = testPartner,
                    testPatient = testPatient,
                    testProducts = listOf(testModernaProduct, testBoostrixProduct),
                    testSites = testSite,
                    medDCheckResponse = medDCheckResponse
                )

                tapCollectPaymentInfo()
                copayDialogRequiredDialog {
                    tapDebitOrCredit()
                }

                collectPaymentScreen {
                    verifyTitle()
                    verifyPhonePrefilled()
                    enterCardNumber(testCardInfo.cardNumber)
                    enterCardExpiration(testCardInfo.expMonth, testCardInfo.expYear)
                    scrollDown()
                    enterNameOnCard(testCardInfo.nameOnCard)
                    enterPhoneNumber(
                        testCardInfo.phoneNumberFirst,
                        testCardInfo.phoneNumberMid,
                        testCardInfo.phoneNumberLast
                    )
                    enterEmailAddress(testCardInfo.emailAddress)
                    tapSavePaymentInfo()
                }

                paymentSummaryScreen {
                    verifyTitle()
                    verifyCopaySubTotalAndAddedDose(
                        testPatient = testPatient,
                        testProducts = listOf(testModernaProduct, testBoostrixProduct),
                        paymentModal = PaymentModals.PaymentDebitOrCredit(),
                        medDCheckResponse = medDCheckResponse!!,
                        cardInfo = testCardInfo
                    )
                    tapSignatureCaptureButton()
                }

                signatureCaptureScreen {
                    verifyTitle()
                    verifySubmitButtonDisabled()
                    drawSignature()
                    tapSignatureSubmitButton()
                }

                IntegrationUtil.waitForOperationComplete()
                checkoutCompleteScreen {
                    verifyTitle()
                    verifyPatientName(testPatient)
                    verifyNumberOfShotsAdministered(2, resources)
                    tapCheckoutAnotherPatient()
                }
            }
        }
        // POST checkout: IT is going to fail here: The EncounterState is STILL "PreShot" until more time passes.
//        verifyPostShotEligibility(
//            appointmentInfo = AppointmentInfo(
//                testPatient = testPatient,
//                riskIconConstant = RiskIconConstant.RiskFreeIcon,
//                eligibilityText = "Guaranteed Payment",
//                responsibilityText = "VaxCare Bill",
//                payerText = MEDB_PAYER_MSG,
//                eligibilityCtaText = "",
//                medDCtaText = "",
//                isMedDTagShow = true,
//                hasMedDRan = true
//            ), medDCheckResponse = medDCheckResponse
//        )
    }

    @Test
    fun addDoseAndThenRunCopay_test() {
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            CopayRequiredTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "addDoseAndThenRunCopay_test"
            )
        )
        homeScreenUtil.loginAsTestPartner(testPartner)
        val testProduct = TestProducts.Boostrix
        val testSite = TestSites.LeftArm
        var medDCheckResponse: MedDCheckResponse? = null
        var appointmentId = ""

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
                appointmentId = createAppointmentWithPatient(testPatient)
            }
        }

        appointmentListScreen {
            verifyTitleAndAppointmentList()
            searchForCreatedAppointment(
                testPatient = testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )
            appointmentId =
                appointmentId.ifEmpty { getAppointmentIdFromPatientName(testPatient).toString() }
            assert(appointmentId != "0") {
                "AppointmentId not found for ${testPatient.completePatientName}"
            }
            verifyAppointmentIconAndTags(
                testPatient = testPatient,
                expectedRiskIcon = RiskIconConstant.RiskFreeIcon,
                stockPillList = listOf(TestStockPill.PrivateStockPill, TestStockPill.MedDTag)
            )
            tapFirstAppointmentWithPatient(testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyTitle()
            verifyAppointmentInfo(
                AppointmentInfo(
                    testPatient = testPatient,
                    riskIconConstant = RiskIconConstant.RiskFreeIcon,
                    eligibilityText = "Ready to Vaccinate",
                    responsibilityText = "VaxCare Bill",
                    payerText = MEDB_PAYER_MSG,
                    eligibilityCtaText = LIMITED_COVERAGE_CTA,
                    medDCtaText = MEDD_CTA_NOTRUN_MSG,
                    isMedDTagShow = true,
                    hasMedDRan = false
                )
            )

            addDose(testProduct)
            unorderedDoseDialog {
                tapKeepDoseDialogButton()
            }

            copayRequiredDialog {
                verifyCopayDialog()
                verifyViewDisableWhenBehindDialog()
                tapRunCopayDialogButton()
            }

            medDCopayScreen {
                verifyAndFillInSSNIfNeeded()
                waitAndVerifyResultsTitle()
                verifyResultsHeaderText()
                medDCheckResponse = runBlocking {
                    patientUtil.getMedDCopays(appointmentId.toInt())
                }

                verifyCopayResults(
                    copays = medDCheckResponse?.copays ?: emptyList(),
                    resources = resources
                )

                tapNextButton()
            }

            verifyTitle()
            selectSiteForDose(testProduct, testSite)
            verifyProductGrid(listOf(testProduct), testSite)

            tapArrowToCheckoutSummary()

            reviewDosesScreen {
                selectUnorderedReasonDoseNotAppearingForAll()
                tapArrowToCheckoutSummary()
            }

            checkoutDosesSummaryScreen {
                verifyTitle()
                verifyCopaySubTotalAndAddedDose(
                    testPartners = testPartner,
                    testPatient = testPatient,
                    testProducts = listOf(testProduct),
                    testSites = testSite,
                    medDCheckResponse = medDCheckResponse
                )

                tapCollectPaymentInfo()

                copayDialogRequiredDialog {
                    tapCashOrCheck()
                }

                paymentSummaryScreen {
                    verifyTitle()
                    verifyCopaySubTotalAndAddedDose(
                        testPatient = testPatient,
                        testProducts = listOf(testProduct),
                        paymentModal = PaymentModals.PaymentCashOrCheck,
                        medDCheckResponse = medDCheckResponse!!
                    )
                    tapSignatureCaptureButton()
                }

                signatureCaptureScreen {
                    verifyTitle()
                    verifySubmitButtonDisabled()
                    drawSignature()
                    tapSignatureSubmitButton()
                }

                IntegrationUtil.waitForOperationComplete()
                checkoutCompleteScreen {
                    verifyTitle()
                    verifyPatientName(testPatient)
                    verifyNumberOfShotsAdministered(1, resources)
                    tapCheckoutAnotherPatient()
                }
            }
        }

        // POST checkout: IT is going to fail here: The EncounterState is STILL "PreShot" until more time passes.
//        verifyPostShotEligibility(
//            AppointmentInfo(
//                testPatient = testPatient,
//                riskIconConstant = RiskIconConstant.PartnerBillIcon(),
//                eligibilityText = "Ready to Bill",
//                responsibilityText = "Partner Bill",
//                payerText = MEDB_PAYER_MSG,
//                eligibilityCtaText = "",
//                medDCtaText = "",
//                isMedDTagShow = true,
//                hasMedDRan = true
//            ),
//            medDCheckResponse
//        )
    }

    @Test
    fun addDoseOnCopayPreviouslyRanAppointment_test() {
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            CopayRequiredTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "addDoseOnCopayPreviouslyRanAppointment_test"
            )
        )
        homeScreenUtil.loginAsTestPartner(testPartner)
        val testProduct = TestProducts.Boostrix
        val testSite = TestSites.RightArm
        val testCardInfo = TestCards.ILabsCard
        var medDCheckResponse: MedDCheckResponse? = null
        var appointmentId = ""

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
                appointmentId = createAppointmentWithPatient(testPatient)
            }
        }

        appointmentListScreen {
            verifyTitleAndAppointmentList()
            searchForCreatedAppointment(
                testPatient = testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )
            appointmentId =
                appointmentId.ifEmpty { getAppointmentIdFromPatientName(testPatient).toString() }
            assert(appointmentId != "0") {
                "AppointmentId not found for ${testPatient.completePatientName}"
            }

            manuallyRunMedDCheck(appointmentId.toInt())
            waitForEligibilityToSettle()
            medDCheckResponse = runBlocking {
                patientUtil.getMedDCopays(appointmentId.toInt())
            }
            verifyAppointmentIconAndTags(
                testPatient = testPatient,
                expectedRiskIcon = RiskIconConstant.RiskFreeIcon,
                stockPillList = listOf(TestStockPill.PrivateStockPill, TestStockPill.MedDTag)
            )
            tapFirstAppointmentWithPatient(testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyTitle()
            verifyAppointmentInfo(
                AppointmentInfo(
                    testPatient = testPatient,
                    riskIconConstant = RiskIconConstant.RiskFreeIcon,
                    eligibilityText = "Ready to Vaccinate",
                    responsibilityText = "VaxCare Bill",
                    payerText = MEDB_PAYER_MSG,
                    eligibilityCtaText = "",
                    medDCtaText = "",
                    isMedDTagShow = true,
                    hasMedDRan = true
                )
            )
            verifyMedDCopaysDisplayed(medDCheckResponse!!.copays)

            addDose(testProduct)
            unorderedDoseDialog {
                tapKeepDoseDialogButton()
            }

            selectSiteForDose(testProduct, testSite)
            verifyProductGrid(
                testProducts = listOf(testProduct),
                site = testSite
            )

            tapArrowToCheckoutSummary()

            reviewDosesScreen {
                selectUnorderedReasonDoseNotAppearingForAll()
                tapArrowToCheckoutSummary()
            }

            checkoutDosesSummaryScreen {
                verifyTitle()
                verifyCopaySubTotalAndAddedDose(
                    testPartners = testPartner,
                    testPatient = testPatient,
                    testProducts = listOf(testProduct),
                    testSites = testSite,
                    medDCheckResponse = medDCheckResponse
                )

                tapCollectPaymentInfo()
                copayDialogRequiredDialog {
                    tapDebitOrCredit()
                }

                collectPaymentScreen {
                    verifyTitle()
                    verifyPhonePrefilled()
                    enterCardNumber(testCardInfo.cardNumber)
                    enterCardExpiration(testCardInfo.expMonth, testCardInfo.expYear)
                    scrollDown()
                    enterNameOnCard(testCardInfo.nameOnCard)
                    enterPhoneNumber(
                        testCardInfo.phoneNumberFirst,
                        testCardInfo.phoneNumberMid,
                        testCardInfo.phoneNumberLast
                    )
                    enterEmailAddress(testCardInfo.emailAddress)
                    tapSavePaymentInfo()
                }

                paymentSummaryScreen {
                    verifyTitle()
                    verifyCopaySubTotalAndAddedDose(
                        testPatient = testPatient,
                        testProducts = listOf(testProduct),
                        paymentModal = PaymentModals.PaymentDebitOrCredit(),
                        medDCheckResponse = medDCheckResponse!!,
                        cardInfo = testCardInfo
                    )
                    tapSignatureCaptureButton()
                }

                signatureCaptureScreen {
                    verifyTitle()
                    verifySubmitButtonDisabled()
                    drawSignature()
                    tapSignatureSubmitButton()
                }

                IntegrationUtil.waitForOperationComplete()
                checkoutCompleteScreen {
                    verifyTitle()
                    verifyPatientName(testPatient)
                    verifyNumberOfShotsAdministered(1, resources)
                    tapCheckoutAnotherPatient()
                }
            }
        }

        // POST checkout: IT is going to fail here: The EncounterState is STILL "PreShot" until more time passes.
//        verifyPostShotEligibility(
//            appointmentInfo = AppointmentInfo(
//                testPatient = testPatient,
//                riskIconConstant = RiskIconConstant.RiskFreeIcon,
//                eligibilityText = "Guaranteed Payment",
//                responsibilityText = "VaxCare Bill",
//                payerText = MEDB_PAYER_MSG,
//                eligibilityCtaText = "",
//                medDCtaText = "",
//                isMedDTagShow = true,
//                hasMedDRan = true
//            ), medDCheckResponse = medDCheckResponse
//        )
    }

    @Ignore("This test requires a legacy COVID product which have been disabled")
    @Test
    fun checkoutMedDAndCovidDoseDuringCheckout_test() {
        val testPatient = TestPatients.MedDWithSsnPatientForCopayRequired()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        val tdapProduct = TestProducts.Boostrix
        val covidProduct = TestProducts.ModernaCovid
        val testSite = TestSites.LeftArm
        val testCardInfo = TestCards.ILabsCard
        var medDCheckResponse: MedDCheckResponse? = null
        var appointmentId = ""

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
                appointmentId = createAppointmentWithPatient(testPatient)
            }
        }

        appointmentListScreen {
            verifyTitleAndAppointmentList()
            searchForCreatedAppointment(
                testPatient = testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )
            appointmentId =
                appointmentId.ifEmpty { getAppointmentIdFromPatientName(testPatient).toString() }
            assert(appointmentId != "0") {
                "AppointmentId not found for ${testPatient.completePatientName}"
            }
            verifyAppointmentIconAndTags(
                testPatient = testPatient,
                expectedRiskIcon = RiskIconConstant.RiskFreeIcon,
                stockPillList = listOf(TestStockPill.PrivateStockPill, TestStockPill.MedDTag)
            )
            tapFirstAppointmentWithPatient(testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyTitle()
            verifyAppointmentInfo(
                AppointmentInfo(
                    testPatient = testPatient,
                    riskIconConstant = RiskIconConstant.RiskFreeIcon,
                    eligibilityText = "Ready to Vaccinate",
                    responsibilityText = "VaxCare Bill",
                    payerText = MEDB_PAYER_MSG,
                    eligibilityCtaText = LIMITED_COVERAGE_CTA,
                    medDCtaText = MEDD_CTA_NOTRUN_MSG,
                    isMedDTagShow = true,
                    hasMedDRan = false
                )
            )
            tapRunCopayCheckButton()
            medDCopayScreen {
                verifyAndFillInSSNIfNeeded()
                waitAndVerifyResultsTitle()
                verifyResultsHeaderText()

                medDCheckResponse = runBlocking {
                    patientUtil.getMedDCopays(appointmentId.toInt())
                }

                verifyCopayResults(
                    copays = medDCheckResponse?.copays ?: emptyList(),
                    resources = resources
                )
                tapNextButton()
            }

            verifyMedDCopaysDisplayed(medDCheckResponse?.copays ?: emptyList())

            addDoseAndSelectSite(covidProduct, testSite)
            addDoseAndSelectSite(tdapProduct, testSite)

            verifyProductGrid(listOf(covidProduct, tdapProduct), testSite)
            tapArrowToCheckoutSummary()

            reviewDosesScreen {
                selectUnorderedReasonDoseNotAppearingForAll()
                tapArrowToCheckoutSummary()
            }

            checkoutDosesSummaryScreen {
                verifyTitle()
                verifyCopaySubTotalAndAddedDose(
                    testPartners = testPartner,
                    testPatient = testPatient,
                    testProducts = listOf(tdapProduct, covidProduct),
                    testSites = testSite,
                    medDCheckResponse = medDCheckResponse
                )

                tapCollectPaymentInfo()
                copayDialogRequiredDialog {
                    tapDebitOrCredit()
                }

                collectPaymentScreen {
                    verifyTitle()
                    verifyPhonePrefilled()
                    enterCardNumber(testCardInfo.cardNumber)
                    enterCardExpiration(testCardInfo.expMonth, testCardInfo.expYear)
                    scrollDown()
                    enterNameOnCard(testCardInfo.nameOnCard)
                    enterPhoneNumber(
                        testCardInfo.phoneNumberFirst,
                        testCardInfo.phoneNumberMid,
                        testCardInfo.phoneNumberLast
                    )
                    enterEmailAddress(testCardInfo.emailAddress)
                    tapSavePaymentInfo()
                }

                paymentSummaryScreen {
                    verifyTitle()
                    verifyCopaySubTotalAndAddedDose(
                        testPatient = testPatient,
                        testProducts = listOf(tdapProduct, covidProduct),
                        paymentModal = PaymentModals.PaymentDebitOrCredit(),
                        medDCheckResponse = medDCheckResponse!!,
                        cardInfo = testCardInfo
                    )
                    tapSignatureCaptureButton()
                }

                signatureCaptureScreen {
                    verifyTitle()
                    verifySubmitButtonDisabled()
                    drawSignature()
                    tapSignatureSubmitButton()
                }

                IntegrationUtil.waitForOperationComplete()
                checkoutCompleteScreen {
                    verifyTitle()
                    verifyPatientName(testPatient)
                    verifyNumberOfShotsAdministered(2, resources)
                    tapCheckoutAnotherPatient()
                }
            }
        }

        // POST checkout: IT is going to fail here: The EncounterState is STILL "PreShot" until more time passes.
//        verifyPostShotEligibility(
//            appointmentInfo = AppointmentInfo(
//                testPatient = testPatient,
//                riskIconConstant = RiskIconConstant.RiskFreeIcon,
//                eligibilityText = "Guaranteed Payment",
//                responsibilityText = "VaxCare Bill",
//                payerText = MEDB_PAYER_MSG,
//                eligibilityCtaText = "",
//                medDCtaText = "",
//                isMedDTagShow = true,
//                hasMedDRan = true
//            ), medDCheckResponse = medDCheckResponse
//        )
    }

    private fun CheckoutRobot.addDoseAndSelectSite(testProduct: TestProducts, testSite: TestSites) {
        addDose(testProduct)
        unorderedDoseDialog {
            tapKeepDoseDialogButton()
        }

        selectSiteForDose(testProduct, testSite)
    }

    private fun verifyPostShotEligibility(appointmentInfo: AppointmentInfo, medDCheckResponse: MedDCheckResponse?) {
        appointmentListScreen {
            verifyTitleAndAppointmentList()
            waitForEligibilityToSettle()
            searchForCreatedAppointment(
                testPatient = appointmentInfo.testPatient,
                maxRefreshAttempts = MAX_REFRESH_ATTEMPTS
            )

            verifyAppointmentIconAndTags(
                testPatient = appointmentInfo.testPatient,
                expectedRiskIcon = appointmentInfo.riskIconConstant,
                itemBackground = TestBackground.PrimaryPurple
            )

            tapRefreshAppointmentsButton()
            tapFirstAppointmentWithPatient(appointmentInfo.testPatient)
        }

        checkoutDosesScreen {
            waitForProgressBarToGone()
            verifyTitle()
            verifyAppointmentInfo(appointmentInfo)
            verifyMedDCopaysDisplayed(medDCheckResponse?.copays ?: emptyList())
        }
    }

    private class CopayRequiredTestsDispatcher(
        useCases: CheckoutUseCases,
        clinicId: Long,
        testDirectory: String
    ) : CheckoutDispatcher(useCases, clinicId) {
        init {
            this withRequestListener requestListener()
            this withMutator responseMutator()
        }

        override val mockTestDirectory: String =
            "${COPAY_TESTS_JSON_FILES_DIRECTORY}$testDirectory/"
    }
}
