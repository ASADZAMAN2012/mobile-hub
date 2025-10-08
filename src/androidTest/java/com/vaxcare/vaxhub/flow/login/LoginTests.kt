/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.login

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
import com.vaxcare.vaxhub.ui.PermissionsActivity
import com.vaxcare.vaxhub.ui.idlingresource.HubIdlingResource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginTests : TestsBase() {
    companion object {
        const val LOGIN_TESTS_DIRECTORY = "LoginTests/"
    }

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var storageUtil: StorageUtil

    private val testWorkManagerHelper = TestWorkManagerHelper()
    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val homeScreenUtil = HomeScreenUtil()

    private val idlingResource: IdlingResource? = HubIdlingResource.instance

    @Before
    fun beforeTests() {
        hiltRule.inject()
        testWorkManagerHelper.initializeWorkManager(workerFactory)
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
    fun testLoginSuccess() {
        registerMockServerDispatcher(
            LoginTestsDispatcher(
                testDirectory = "testLoginSuccess"
            )
        )
        homeScreenUtil.setupPartnerAndClinic(TestPartners.RprdCovidPartner)
        homeScreenUtil.verifySuccessfulLoginFlow(TestPartners.RprdCovidPartner)
        assertEquals(
            "Expected to see ${TestPartners.RprdCovidPartner.partnerName} as Partner Name.",
            TestPartners.RprdCovidPartner.partnerName,
            IntegrationUtil.getText(
                onView(
                    withId(R.id.partner_name)
                )
            )
        )
    }

    @Test
    fun testLoginFail() {
        registerMockServerDispatcher(
            LoginTestsDispatcher(
                testDirectory = "testLoginFail"
            )
        )
        homeScreenUtil.setupPartnerAndClinic(TestPartners.InvalidPartner)
        homeScreenUtil.verifyUnsuccessfulLoginFlow()
        assertEquals(
            "Expected to see Empty as Partner Name",
            TestPartners.InvalidPartner.partnerName,
            IntegrationUtil.getText(
                onView(
                    withId(R.id.partner_name)
                )
            )
        )
    }

    @Ignore(
        "This tests needs the following condition in PinLockViewModel " +
            "to be true `pinLockViewModel.needUsersSynced()`. Since this is false when the test" +
            "runs then the sync label doesn't show and the test fails"
    )
    @Test
    fun validateInvalidPinSyncsUsers() {
        registerMockServerDispatcher(
            LoginTestsDispatcher(
                testDirectory = "validateInvalidPinSyncsUsers"
            )
        )
        val testPartner: TestPartners = homeScreenUtil.loginAsTestPartner()
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner = testPartner, validateUserSyncText = true)
    }

    /**
     * Scaffolding Dispatcher for LoginTests
     *
     * @param testDirectory this should be the name of the test function.
     * ex: "testLoginSuccess" for the Login Success test
     */
    private class LoginTestsDispatcher(
        testDirectory: String,
    ) : BaseMockDispatcher() {
        override val mockTestDirectory = "$LOGIN_TESTS_DIRECTORY$testDirectory/"
    }
}
