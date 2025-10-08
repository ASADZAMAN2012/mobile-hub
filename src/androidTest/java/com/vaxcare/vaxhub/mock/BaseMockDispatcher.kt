/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock

import android.util.Log
import com.vaxcare.vaxhub.mock.model.MockRequest
import com.vaxcare.vaxhub.mock.util.MockMutator
import com.vaxcare.vaxhub.mock.util.MockRequestListener
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.io.FileNotFoundException
import java.io.InputStreamReader

/**
 * The Base class for MockDispatchers
 *
 * fileNames - should be just the filenames of the desired data
 * mockTestDirectory - should be the first child of the directory:
 * `mock/MockData/VaxCare.VaxHubServer`
 */
abstract class BaseMockDispatcher : Dispatcher() {
    abstract val mockTestDirectory: String

    private val callHistory: MutableMap<String, Int> = mutableMapOf()
    private var mutator: MockMutator? = null
    private var requestListener: MockRequestListener? = null

    companion object {
        const val RESPONSE_CODE_OK = 200
        const val RESPONSE_CODE_EMPTY = 204
        const val RESPONSE_CODE_ERROR = 500
        const val INDEX_BODY = "up"
        const val INDEX_TXT = "/index.txt"
        const val ROOT_MOCK_DIRECTORY = "mock/MockData/VaxCare.VaxHubServer/Mobile_Hub/"
        const val FILE_SUFFIX_SEPERATOR = "_"
        const val JSON_FILE_EXTENSION = ".json"

        private const val TAG = "MockDispatcher"
        private const val PADDING_FORMAT = "%02d"
    }

    infix fun withMutator(mockMutator: MockMutator) = apply { mutator = mockMutator }

    infix fun withRequestListener(listener: MockRequestListener) = apply { requestListener = listener }

    private fun RecordedRequest.toMockRequest() =
        MockRequest(endpoint = path ?: "", requestMethod = method ?: "", requestBody = body)

    override fun dispatch(request: RecordedRequest): MockResponse {
        requestListener?.let { listener ->
            runBlocking { listener.onBeforeRequest(request.toMockRequest()) }
        }
        return when (request.path) {
            INDEX_TXT -> {
                MockResponse()
                    .setResponseCode(RESPONSE_CODE_OK)
                    .setBody(INDEX_BODY)
            }

            else -> getMockResponse(path = request.path ?: "", requestMethod = request.method ?: "")
        }
    }

    private fun getMockResponse(path: String, requestMethod: String): MockResponse =
        loadResponseFromFile(
            path = path,
            requestMethod = requestMethod,
            file = getFileNameFromRequestPath(path = path, requestMethod = requestMethod)
        )

    /**
     * File Names are formatted as follows:
     * {{name-of-endpoint}}_{{requestMethod}}{{counter}}.json
     * i.e. the first GET call for `api/setup/LocationData` will be looking for a corresponding
     * file named `setup-LocationData_GET01.json`
     *
     * @param path the url path
     * @param requestMethod the request method
     */
    private fun getFileNameFromRequestPath(path: String, requestMethod: String): String {
        val formattedFilePrefix = path.stripPathToFileFormat()
        val counter = callHistory.getAndIncrement("${formattedFilePrefix}_$requestMethod")
        val matchingSuffix = "$requestMethod${PADDING_FORMAT.format(counter)}"

        return StringBuilder()
            .append(ROOT_MOCK_DIRECTORY)
            .append(mockTestDirectory)
            .append(formattedFilePrefix)
            .append(FILE_SUFFIX_SEPERATOR)
            .append(matchingSuffix)
            .append(JSON_FILE_EXTENSION)
            .toString()
    }

    private fun loadResponseFromFile(
        path: String,
        requestMethod: String,
        file: String?
    ): MockResponse =
        file?.let {
            try {
                val body =
                    getFileContents(path = path, requestMethod = requestMethod, filename = it)
                        ?: getFileContents(
                            path = path,
                            requestMethod = requestMethod,
                            filename = it.decrementAndResetCounterSuffix(path, requestMethod)
                        )
                        ?: throw FileNotFoundException("File was not found for: $file")
                MockResponse().setResponseCode(RESPONSE_CODE_OK).setBody(body)
            } catch (e: Exception) {
                Log.e(TAG, "exception: $e:\n ${e.message}")
                MockResponse().setResponseCode(RESPONSE_CODE_ERROR).setBody(e.message ?: "Unknown Error")
            }
        } ?: MockResponse().setResponseCode(RESPONSE_CODE_EMPTY)

    private fun getFileContents(
        path: String,
        requestMethod: String,
        filename: String
    ): String? {
        Log.d(TAG, "Attempting to get file contents for: $filename")
        return javaClass.classLoader?.getResourceAsStream(filename)?.let { stream ->
            InputStreamReader(stream).use { str -> str.readText() }.let { response ->
                mutator?.let { mutator ->
                    runBlocking {
                        mutator.onBeforeResponse(
                            request = MockRequest(path, requestMethod),
                            responseBody = response
                        ) ?: response
                    }
                } ?: response
            }
        }
    }

    /**
     * Resets the counter to "00" and decrements the actual counter in the callHistory hashmap.
     */
    private fun String.decrementAndResetCounterSuffix(rawPath: String, requestMethod: String) =
        this.replace("\\d{2}".toRegex(), "00").also {
            Log.d(TAG, "file for path: $rawPath not found. Counter being decremented.")
            callHistory.decrement("${rawPath.stripPathToFileFormat()}_$requestMethod")
        }

    /**
     * Strips the path for any URL-specific codes for mock data file path
     *
     * example:
     *  /api/patients/appointment/123456/checkout?someVar=444
     *  will become
     *  patients-appointment-checkout
     */
    private fun String.stripPathToFileFormat() =
        split("?")[0]
            .replace("/api/", "")
            .replace("/\\d+".toRegex(), "")
            .replace("/", "-")

    private fun MutableMap<String, Int>.getAndIncrement(key: String): Int =
        (this[key] ?: 0).let { oldValue ->
            val newValue = oldValue + 1
            this[key] = newValue
            newValue
        }

    private fun MutableMap<String, Int>.decrement(key: String) {
        (this[key])?.let { oldValue ->
            val newValue = oldValue - 1
            this[key] = newValue
            newValue
        }
    }
}
