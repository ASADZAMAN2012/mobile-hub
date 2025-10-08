/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel.checkout.appointment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.core.dispatcher.TestDispatcherProvider
import com.vaxcare.vaxhub.model.AdministeredVaccine
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.AppointmentDetailDto
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.EncounterMessageJson
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateJson
import com.vaxcare.vaxhub.model.checkout.CheckoutOptions
import com.vaxcare.vaxhub.model.checkout.toCheckoutStockOptions
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.ShotStatus
import com.vaxcare.vaxhub.model.order.OrderEntity
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.OrdersRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.ShotAdministratorRepository
import com.vaxcare.vaxhub.repository.SimpleOnHandInventoryRepository
import com.vaxcare.vaxhub.repository.WrongProductRepository
import com.vaxcare.vaxhub.viewmodel.CheckoutPatientViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutPatientViewModel.CheckoutPatientState
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.State
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutPatientViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val appointmentRepository: AppointmentRepository = mockk()
    private val shotAdministratorRepository: ShotAdministratorRepository = mockk()
    private val ordersRepository: OrdersRepository = mockk()
    private val analytics: AnalyticReport = mockk()
    private val productRepository: ProductRepository = mockk()
    private val locationRepository: LocationRepository = mockk()
    private val wrongProductRepository: WrongProductRepository = mockk()
    private val simpleOnHandInventoryRepository: SimpleOnHandInventoryRepository = mockk()
    private lateinit var viewModel: CheckoutPatientViewModel

    private val apiAppointment = getApiAppointmentResponse()
    private val roomAppointment = getCheckedOutRoomAppointment()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CheckoutPatientViewModel(
            appointmentRepository = appointmentRepository,
            shotAdministratorRepository = shotAdministratorRepository,
            orderRepository = ordersRepository,
            analytics = analytics,
            productRepository = productRepository,
            locationRepository = locationRepository,
            wrongProductRepository = wrongProductRepository,
            simpleOnHandInventoryRepository = simpleOnHandInventoryRepository,
            dispatcherProvider = TestDispatcherProvider(),
            partDService = mockk()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Verify appointment loaded from api`() =
        runTest {
            coEvery { locationRepository.getFeatureFlagsAsync() } returns listOf()
            coEvery { locationRepository.getInventorySourcesAsync() } returns listOf(InventorySource.PRIVATE)
            coEvery { shotAdministratorRepository.getAllAsync() } returns listOf()
            coEvery { appointmentRepository.getAndInsertUpdatedAppointment(1) } returns apiAppointment
            coEvery { appointmentRepository.getAppointmentByIdAsync(1) } returns roomAppointment
            coEvery { analytics.saveMetric(any()) } returns Unit

            viewModel.fetchUpdatedAppointment(appointmentId = 1, forceFetch = true)

            val collectedStates = mutableListOf<State>()
            viewModel.uiFlow.take(2).collect { collectedStates.add(it) }
            println("states: $collectedStates")
            assert(
                collectedStates.toList() == listOf(
                    LoadingState,
                    CheckoutPatientState.AppointmentLoaded(
                        appointment = apiAppointment.toAppointment(),
                        flags = listOf(),
                        shotAdmin = null,
                        availableCheckoutOptions = null,
                        inventorySources = listOf(InventorySource.PRIVATE).toCheckoutStockOptions()
                            .toList(),
                        isForceRefresh = true
                    )
                )
            )
        }

    @Test
    fun `Verify appointment loaded from api contain orders`() =
        runTest {
            val orms = listOf(buildOrderEntity())
            coEvery { locationRepository.getFeatureFlagsAsync() } returns listOf(
                FeatureFlag(16, "RightPatientRightDose")
            )
            coEvery { locationRepository.getInventorySourcesAsync() } returns listOf(InventorySource.PRIVATE)
            coEvery { shotAdministratorRepository.getAllAsync() } returns listOf()
            coEvery { appointmentRepository.getAndInsertUpdatedAppointment(1) } returns apiAppointment
            coEvery { appointmentRepository.getAppointmentByIdAsync(1) } returns roomAppointment
            coEvery { ordersRepository.getOrdersByPatient(1) } returns orms
            coEvery { analytics.saveMetric(any()) } returns Unit

            viewModel.fetchUpdatedAppointment(appointmentId = 1, forceFetch = true)

            val collectedStates = mutableListOf<State>()
            viewModel.uiFlow.take(2).collect { collectedStates.add(it) }
            assert(
                collectedStates.toList() == listOf(
                    LoadingState,
                    CheckoutPatientState.AppointmentLoaded(
                        appointment = apiAppointment.toAppointment().apply { orders = orms },
                        flags = listOf(FeatureFlag(16, "RightPatientRightDose")),
                        shotAdmin = null,
                        availableCheckoutOptions = listOf(CheckoutOptions.REFRESH),
                        inventorySources = listOf(InventorySource.PRIVATE).toCheckoutStockOptions()
                            .toList(),
                        isForceRefresh = true
                    )
                )
            )
        }

    @Test
    fun `Verify appointment loaded from room`() =
        runTest {
            coEvery { locationRepository.getFeatureFlagsAsync() } returns listOf()
            coEvery { locationRepository.getInventorySourcesAsync() } returns listOf(InventorySource.PRIVATE)
            coEvery { shotAdministratorRepository.getAllAsync() } returns listOf()
            coEvery { appointmentRepository.getAndInsertUpdatedAppointment(1) } returns null
            coEvery { appointmentRepository.getAppointmentByIdAsync(1) } returns roomAppointment
            coEvery { analytics.saveMetric(any()) } returns Unit

            viewModel.fetchUpdatedAppointment(appointmentId = 1, forceFetch = true)

            val collectedStates = mutableListOf<State>()
            viewModel.uiFlow.take(2).collect { collectedStates.add(it) }
            assert(
                collectedStates.toList() == listOf(
                    LoadingState,
                    CheckoutPatientState.AppointmentLoaded(
                        appointment = roomAppointment,
                        flags = listOf(),
                        shotAdmin = null,
                        availableCheckoutOptions = null,
                        inventorySources = listOf(InventorySource.PRIVATE).toCheckoutStockOptions()
                            .toList(),
                        isForceRefresh = true
                    )
                )
            )
        }

    private fun getApiAppointmentResponse() =
        AppointmentDetailDto(
            id = 1,
            clinicId = 1,
            appointmentTime = LocalDateTime.now(),
            patient = getPatient(),
            paymentType = null,
            paymentMethod = PaymentMethod.InsurancePay,
            stock = "Private",
            visitType = "Well",
            checkedOut = false,
            checkedOutTime = null,
            provider = getProvider(),
            administeredBy = null,
            isEditable = true,
            encounterState = getEncounterStateJson(),
            administeredVaccines = null
        )

    private fun getCheckedOutRoomAppointment() =
        Appointment(
            id = 1,
            clinicId = 1,
            vaccineSupply = "Private",
            appointmentTime = LocalDateTime.now().minusDays(1),
            patient = getPatient(),
            paymentType = null,
            paymentMethod = PaymentMethod.InsurancePay,
            visitType = "Well",
            checkedOut = true,
            checkedOutTime = LocalDateTime.now().minusDays(1),
            provider = getProvider(),
            administeredBy = 1,
            isEditable = false,
            encounterState = getEncounterStateEntity(),
            administeredVaccines = getAdministeredVaccines(),
            isProcessing = null,
            orders = listOf()
        )

    private fun getPatient() =
        Patient(
            id = 1,
            originatorPatientId = "One",
            firstName = "First",
            lastName = "Last",
            dob = "01/01/1980",
            middleInitial = null,
            race = null,
            ethnicity = null,
            gender = null,
            ssn = null,
            address1 = null,
            address2 = null,
            city = null,
            state = null,
            zip = null,
            phoneNumber = null,
            email = null,
            paymentInformation = getPaymentInformation()
        )

    private fun getProvider() = Provider(id = 1, firstName = "Dr", lastName = "Pepper")

    private fun getEncounterStateJson() =
        EncounterStateJson(
            id = 1,
            appointmentId = 1,
            shotStatus = ShotStatus.PreShot,
            isClosed = false,
            createdUtc = LocalDateTime.now().minusDays(1),
            messages = getEncounterMessages()
        )

    private fun getEncounterMessages() =
        listOf(
            EncounterMessageJson(
                riskAssessmentId = 1,
                appointmentId = 1,
                status = AppointmentStatus.RISK_FREE,
                icon = AppointmentIcon.STAR,
                primaryMessage = null,
                secondaryMessage = null,
                callToAction = CallToAction.None,
                topRejectCode = null,
                serviceType = AppointmentServiceType.VACCINE
            )
        )

    private fun getEncounterStateEntity() =
        EncounterStateEntity(
            id = 1,
            appointmentId = 1,
            shotStatus = ShotStatus.PostShot,
            isClosed = true,
            createdUtc = LocalDateTime.now().minusDays(1)
        )

    private fun getAdministeredVaccines() =
        listOf(
            AdministeredVaccine(
                id = 1,
                checkInVaccinationId = 1,
                appointmentId = 1,
                lotNumber = "1000",
                ageIndicated = true,
                method = null,
                site = null,
                productId = 1,
                synced = null,
                doseSeries = null,
                deletedDate = null,
                isDeleted = false
            )
        )

    private fun getPaymentInformation() =
        PaymentInformation(
            id = 1,
            insuranceName = null,
            primaryInsuranceId = null,
            primaryInsurancePlanId = null,
            primaryMemberId = null,
            primaryGroupId = null,
            uninsured = false,
            paymentMode = PaymentMethod.InsurancePay,
            vfcFinancialClass = null,
            insuredFirstName = null,
            insuredLastName = null,
            insuredDob = null,
            insuredGender = null,
            appointmentId = 1,
            relationshipToInsured = null,
            portalInsuranceMappingId = null,
            mbi = null,
            stock = null
        )

    private fun buildOrderEntity() =
        OrderEntity(
            orderId = 10,
            partnerId = 10,
            clinicId = 10,
            patientVisitId = null,
            patientId = 1,
            isDeleted = null,
            shortDescription = "RSV",
            orderNumber = "orderNumber",
            satisfyingProductIds = listOf(354, 355, 420),
            serverSyncDateTimeUtc = Instant.now(),
            durationInDays = 15,
            expirationDate = LocalDateTime.now().plusDays(15),
            orderDate = LocalDateTime.now().minusDays(1)
        )
}
