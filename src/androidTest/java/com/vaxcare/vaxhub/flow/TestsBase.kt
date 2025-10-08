/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow

import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.common.ScreenshotTakingRule
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule

open class TestsBase {
    protected lateinit var mockServer: MockWebServer

    @get:Rule
    val screenshotRule = ScreenshotTakingRule()

    protected fun registerMockServerDispatcher(baseDispatcher: BaseMockDispatcher) {
        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer = MockWebServer()
            with(mockServer) {
                start(8080)
                dispatcher = baseDispatcher
            }
        }
    }
}
