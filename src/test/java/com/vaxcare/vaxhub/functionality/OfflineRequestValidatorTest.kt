/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.squareup.moshi.Moshi
import com.vaxcare.core.storage.util.FileStorage
import com.vaxcare.vaxhub.AppInfo
import com.vaxcare.vaxhub.data.MockRequests
import com.vaxcare.vaxhub.web.offline.OfflineRequestValidator
import com.vaxcare.vaxhub.web.offline.OfflineRequestValidatorImpl
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class OfflineRequestValidatorTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var moshi: Moshi

    @RelaxedMockK
    lateinit var fileStorage: FileStorage

    @RelaxedMockK
    lateinit var appInfo: AppInfo

    private lateinit var offlineRequestValidator: OfflineRequestValidator

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        offlineRequestValidator = OfflineRequestValidatorImpl(
            moshi = moshi,
            fileStorage = fileStorage,
            appInfo = appInfo
        )
    }

    @Test
    fun `Validate Invalid Request`() {
        val savedOfflineRequest = offlineRequestValidator.validateRequest(MockRequests.checkoutRequestIgnoreHeader)
        assert(savedOfflineRequest == null)
    }

    @Test
    fun `Validate Valid Request`() {
        val savedOfflineRequest = offlineRequestValidator.validateRequest(MockRequests.checkoutRequest)
        assert(savedOfflineRequest != null)
    }
}
