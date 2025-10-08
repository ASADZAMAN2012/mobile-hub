package com.vaxcare.vaxhub.useCase

import com.vaxcare.vaxhub.core.extension.isDoBCaptureDisabled
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import timber.log.Timber

@ExperimentalCoroutinesApi
class DeterminePatientCheckoutInitialScreenUseCaseTest {
    private lateinit var useCase: DeterminePatientCheckoutInitialScreenUseCase
    private lateinit var locationRepository: LocationRepository

    @Before
    fun setup() {
        locationRepository = mockk(relaxed = true) // relaxed = true for default behavior
        useCase = DeterminePatientCheckoutInitialScreenUseCase(locationRepository)

        Timber.plant(object : Timber.DebugTree() {
            override fun log(
                priority: Int,
                tag: String?,
                message: String,
                t: Throwable?
            ) {
                println("$tag - $message")
            }
        })
    }

    @Test
    fun `invoke returns DOB_CAPTURE when DOB is missing and feature is enabled`() =
        runTest {
            val appointment = mockk<Appointment> {
                coEvery { patient } returns mockk {
                    every { dob } returns null
                }
            }
            coEvery { locationRepository.getFeatureFlagsAsync() } returns mockk {
                every { isDoBCaptureDisabled() } returns false
            }

            val result = useCase(appointment)
            assertEquals(DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.DOB_CAPTURE, result)
        }

    @Test
    fun `invoke returns CHECKOUT_PATIENT when DOB is present and feature is enabled`() =
        runTest {
            val appointment = mockk<Appointment> {
                coEvery { patient } returns mockk {
                    every { dob } returns "1990-01-01"
                }
            }
            coEvery { locationRepository.getFeatureFlagsAsync() } returns mockk {
                every { isDoBCaptureDisabled() } returns false
            }

            val result = useCase(appointment)
            assertEquals(DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.CHECKOUT_PATIENT, result)
        }

    @Test
    fun `invoke returns CHECKOUT_PATIENT when DOB is present and feature is disabled`() =
        runTest {
            val appointment = mockk<Appointment> {
                coEvery { patient } returns mockk {
                    every { dob } returns "1990-01-01"
                }
            }
            coEvery { locationRepository.getFeatureFlagsAsync() } returns mockk {
                every { isDoBCaptureDisabled() } returns true
            }

            val result = useCase(appointment)
            assertEquals(DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.CHECKOUT_PATIENT, result)
        }

    @Test
    fun `invoke returns DOB_CAPTURE when DOB is missing and an exception is thrown`() =
        runTest {
            val appointment = mockk<Appointment> {
                coEvery { patient } returns mockk {
                    every { dob } returns null
                }
            }
            coEvery { locationRepository.getFeatureFlagsAsync() } throws RuntimeException("Test Exception")

            val result = useCase(appointment)
            assertEquals(DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.DOB_CAPTURE, result)
        }

    @Test
    fun `invoke returns CHECKOUT_PATIENT when DOB is present and an exception is thrown`() =
        runTest {
            val appointment = mockk<Appointment> {
                coEvery { patient } returns mockk {
                    every { dob } returns "1990-01-01"
                }
            }
            coEvery { locationRepository.getFeatureFlagsAsync() } throws RuntimeException("Test Exception")

            val result = useCase(appointment)
            assertEquals(DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.CHECKOUT_PATIENT, result)
        }
}
