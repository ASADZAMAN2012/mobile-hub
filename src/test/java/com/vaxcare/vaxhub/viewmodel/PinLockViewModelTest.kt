/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.util.getOrAwaitValue
import com.vaxcare.vaxhub.viewmodel.PinLockViewModel.PinLockState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class PinLockViewModelTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var userRepository: UserRepository

    private val pinLockViewmodel: PinLockViewModel by lazy {
        PinLockViewModel(userRepository)
    }

    private val pin = "1234"

    private val testUser = User(
        firstName = "testfirst",
        lastName = "testlast",
        pin = pin,
        userId = 1234,
        userName = "test@vaxcare.com"
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `Successful Pin In Test`() {
        runBlocking {
            coEvery { userRepository.getUserByPin(any()) } returns testUser
            pinLockViewmodel.attemptPinIn(pin)
            val state = pinLockViewmodel.state.getOrAwaitValue(1)
            assert(state is PinLockState.PinSuccess)
            assert((state as PinLockState.PinSuccess).user.pin == pin)
        }
    }

    @Test
    fun `Failure Pin In Test`() {
        coEvery { userRepository.getUserByPin(any()) } returns null
        pinLockViewmodel.attemptPinIn(pin)
        val state = pinLockViewmodel.state.getOrAwaitValue(1)
        assert(state is PinLockState.PinFailed)
    }
}
