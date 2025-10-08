/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.vaxhub.model.AdministeredVaccine
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.AppointmentCheckout
import com.vaxcare.vaxhub.model.AppointmentData
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.appointment.AppointmentHubMetaData
import com.vaxcare.vaxhub.model.appointment.EncounterMessageEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import java.time.Instant
import java.time.ZoneOffset

@Suppress("ktlint:standard:max-line-length")
@Dao
abstract class AppointmentDao {
    // insert transactions
    @Transaction
    @Insert
    suspend fun upsert(appointments: List<Appointment>) {
        val encounterState = mutableListOf<Pair<Int, EncounterStateEntity?>>()
        val vaccineData = mutableListOf<Pair<Int, List<AdministeredVaccine>>>()
        val tempList = appointments.toMutableList()
        // Sqlite can only handle 999 vars at a time - so this is a staggered list
        var current = tempList.size
        while (current > 0) {
            vaccineData.clear()
            encounterState.clear()
            tempList.subList(
                0,
                if (current > 999) {
                    999
                } else {
                    current
                }
            ).forEach {
                vaccineData.add(Pair(it.id, it.administeredVaccines))
                encounterState.add(it.id to EncounterStateEntity.fromEncounterState(it.encounterState))
            }

            upsertAdministeredVaccines(vaccineData)
            upsertEncounterStates(encounterState)
            insertAppointments(tempList.map { it.toAppointmentData() })
            tempList.subList(
                0,
                if (current > 999) {
                    999
                } else {
                    current
                }
            ).clear()
            current = tempList.size
        }
    }

    @Transaction
    @Insert
    suspend fun upsertAdministeredVaccines(data: List<Pair<Int, List<AdministeredVaccine>>>) {
        val map = data.map { it.first }
        deleteAdministeredVaccinesByAppointmentIds(map)

        val temp = data.map { sec ->
            sec.second.map {
                AdministeredVaccine(
                    id = 0,
                    checkInVaccinationId = it.checkInVaccinationId,
                    appointmentId = it.appointmentId,
                    lotNumber = it.lotNumber,
                    ageIndicated = it.ageIndicated,
                    method = it.method,
                    site = it.site,
                    doseSeries = it.doseSeries,
                    productId = it.productId,
                    synced = it.synced,
                    deletedDate = it.deletedDate,
                    isDeleted = it.isDeleted
                ).apply {
                    paymentMode = it.paymentMode
                }
            }
        }

        val list = mutableListOf<AdministeredVaccine>()
        temp.forEach { list.addAll(it) }
        insertAdministeredVaccines(list)
    }

    @Transaction
    @Insert
    suspend fun upsertAdministeredVaccines(
        appointmentId: Int,
        administeredVaccines: List<AdministeredVaccine>,
        appointmentCheckout: AppointmentCheckout? = null
    ) {
        deleteAdministeredVaccinesByAppointmentId(appointmentId)

        if (!administeredVaccines.isNullOrEmpty()) {
            insertAdministeredVaccines(
                administeredVaccines
                    .map {
                        AdministeredVaccine(
                            id = 0,
                            checkInVaccinationId = it.checkInVaccinationId,
                            appointmentId = it.appointmentId,
                            lotNumber = it.lotNumber,
                            ageIndicated = it.ageIndicated,
                            method = it.method,
                            site = it.site,
                            productId = it.productId,
                            synced = it.synced,
                            doseSeries = it.doseSeries,
                            deletedDate = it.deletedDate,
                            isDeleted = it.isDeleted
                        ).apply {
                            paymentMode = it.paymentMode
                        }
                    }
            )
        }

        if (appointmentCheckout != null) {
            updateAppointmentCheckoutData(
                appointmentId,
                administeredVaccines.isNotEmpty(),
                appointmentCheckout.administered.toInstant(ZoneOffset.UTC),
                appointmentCheckout.administeredBy
            )
        }
    }

    private suspend fun upsertEncounterStates(data: MutableList<Pair<Int, EncounterStateEntity?>>) {
        val ids = data.map { it.first }
        deleteEncounterDataByAppointmentIds(ids)
        val newStates = data.mapNotNull { pair ->
            val incomingAppointmentId = pair.first
            val state = pair.second?.apply {
                appointmentId = incomingAppointmentId
                messages.forEach {
                    it.appointmentId = incomingAppointmentId
                }
            }

            state
        }

        insertEncounterStates(newStates)
        insertEncounterMessages(newStates.flatMap { it.messages })
    }

    private suspend fun deleteEncounterDataByAppointmentIds(appointmentIds: List<Int>) {
        deleteEncounterMessageByAppointmentIds(appointmentIds)
        deleteEncounterStateByAppointmentIds(appointmentIds)
    }

    @Query("DELETE from EncounterMessage WHERE appointmentId in (:appointmentIds)")
    abstract suspend fun deleteEncounterMessageByAppointmentIds(appointmentIds: List<Int>)

    @Query("DELETE from EncounterState WHERE AppointmentId in (:appointmentIds)")
    abstract suspend fun deleteEncounterStateByAppointmentIds(appointmentIds: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEncounterStates(states: List<EncounterStateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEncounterMessages(messages: List<EncounterMessageEntity>)

    // delete transactions
    @Transaction
    @Delete
    suspend fun delete(appointments: List<Appointment>) {
        val ids = appointments.map { it.id }
        deleteAdministeredVaccinesByAppointmentIds(ids)
        deleteEncounterMessageByAppointmentIds(ids)
        deleteEncounterStateByAppointmentIds(ids)
        deleteAppointmentsByIds(ids)
    }

    @Transaction
    @Delete
    suspend fun deleteAll() {
        deleteAllAdministeredVaccines()
        deleteAllEncounterMessages()
        deleteAllEncounterStates()
        deleteAllAppointments()
    }

    @Transaction
    @Delete
    suspend fun deleteAllByAppointmentIds(ids: List<Int>) {
        deleteAdministeredVaccinesByAppointmentIds(ids)
        deleteEncounterMessageByAppointmentIds(ids)
        deleteEncounterStateByAppointmentIds(ids)
        deleteAppointmentsByIds(ids)
    }

    // get transactions
    @Transaction
    @Query("SELECT * FROM AppointmentData WHERE Id = :id")
    abstract fun getById(id: Int): LiveData<Appointment?>

    @Transaction
    @Query("SELECT * FROM AppointmentData WHERE Id = :id")
    abstract suspend fun getByIdAsync(id: Int): Appointment?

    @Transaction
    @Query("SELECT * FROM EncounterMessage WHERE appointmentId = :id")
    abstract suspend fun getMessagesByAppointmentIdAsync(id: Int): List<EncounterMessageEntity>

    @Transaction
    @Query("SELECT * FROM EncounterMessage WHERE appointmentId = :id")
    abstract fun getMessagesByAppointmentId(id: Int): LiveData<List<EncounterMessageEntity>>

    @Transaction
    @Query("SELECT * FROM EncounterMessage WHERE appointmentId in (:ids)")
    abstract suspend fun getMessagesByAppointmentIds(ids: List<Int>): List<EncounterMessageEntity>

    @Transaction
    @Query("SELECT * FROM AppointmentData ORDER BY AppointmentTime, patient_lastName")
    abstract fun getAll(): LiveData<List<Appointment>>

    @Transaction
    @Query("SELECT * FROM AppointmentData ORDER BY AppointmentTime, patient_lastName")
    abstract suspend fun getAllAsync(): List<Appointment>?

    @Transaction
    @Query(
        "SELECT * FROM AppointmentData WHERE AppointmentTime > :startDate AND AppointmentTime < :endDate ORDER BY AppointmentTime, patient_lastName"
    )
    abstract suspend fun getAllByAppointmentAsync(startDate: Long, endDate: Long): List<Appointment>

    @Transaction
    @Query("SELECT COUNT(*) FROM AppointmentData WHERE AppointmentTime > :startDate AND AppointmentTime < :endDate")
    abstract suspend fun getAppointmentCount(startDate: Long, endDate: Long): Int

    @Transaction
    @Query(
        """SELECT * FROM AppointmentData
            WHERE AppointmentTime > :startDate AND AppointmentTime < :endDate
            AND (AppointmentData.patient_firstName LIKE '%' || :identifier || '%'
                OR AppointmentData.patient_lastName LIKE '%' || :identifier || '%'
                OR (AppointmentData.patient_firstName LIKE '%' || :firstPart || '%' 
                   AND AppointmentData.patient_lastName LIKE '%' || :lastPart || '%') 
                OR (AppointmentData.patient_firstName LIKE '%' || :lastPart || '%' 
                   AND AppointmentData.patient_lastName LIKE '%' || :firstPart || '%')
                OR AppointmentData.patient_originatorPatientId LIKE '%' || :identifier || '%'
                OR AppointmentData.id = :identifier)
            ORDER BY AppointmentTime"""
    )
    abstract suspend fun getAppointmentsByIdentifierAsync(
        identifier: String,
        firstPart: String,
        lastPart: String,
        startDate: Long,
        endDate: Long
    ): List<Appointment>

    // protected inserts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAppointment(appointment: AppointmentData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAppointments(appointments: List<AppointmentData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAdministeredVaccines(administeredVaccines: List<AdministeredVaccine>)

    // protected updates
    @Query(
        "UPDATE AppointmentData SET CheckedOut = :checkedOut, CheckedOutTime = :checkedOutTime, AdministeredBy = :administeredBy WHERE Id = :appointmentId"
    )
    protected abstract suspend fun updateAppointmentCheckoutData(
        appointmentId: Int,
        checkedOut: Boolean,
        checkedOutTime: Instant,
        administeredBy: Int
    )

    // protected updates
    @Query("UPDATE AppointmentData SET IsProcessing = :isProcessing WHERE Id = :appointmentId")
    abstract suspend fun updateAppointmentProcessingData(appointmentId: Int, isProcessing: Boolean)

    // protected deletes
    @Delete
    protected abstract suspend fun deleteAppointment(appointment: AppointmentData)

    @Query("DELETE FROM AppointmentData")
    protected abstract suspend fun deleteAllAppointments()

    @Query("DELETE FROM EncounterMessage")
    protected abstract suspend fun deleteAllEncounterMessages()

    @Query("DELETE FROM EncounterState")
    protected abstract suspend fun deleteAllEncounterStates()

    @Query("DELETE from AdministeredVaccine WHERE AppointmentId = :appointmentId")
    protected abstract suspend fun deleteAdministeredVaccinesByAppointmentId(appointmentId: Int)

    @Query("DELETE from AdministeredVaccine WHERE AppointmentId in (:appointmentIds)")
    protected abstract suspend fun deleteAdministeredVaccinesByAppointmentIds(appointmentIds: List<Int>)

    @Delete
    protected abstract suspend fun deleteAdministeredVaccines(administeredVaccines: List<AdministeredVaccine>)

    @Query("DELETE FROM AdministeredVaccine")
    protected abstract suspend fun deleteAllAdministeredVaccines()

    @Query("DELETE FROM AppointmentHubMetaData")
    protected abstract suspend fun deleteAllAppointmentHubMetaData()

    @Transaction
    @Query(
        """SELECT * FROM AppointmentData
            WHERE AppointmentTime >= :startDate 
            AND AppointmentTime <= :endDate
            AND AppointmentData.id != :appointmentId
            AND AppointmentData.patient_lastName = :lastName
            AND AppointmentData.paymentMethod = :paymentMethod
            AND ((AppointmentData.paymentType = :paymentType) OR (AppointmentData.paymentType IS NULL AND :paymentType IS NULL))
            AND AppointmentData.checkedOut = 0
            ORDER BY AppointmentTime"""
    )
    abstract suspend fun getFamilyAppointments(
        appointmentId: Int,
        startDate: Long,
        endDate: Long,
        lastName: String,
        paymentMethod: PaymentMethod,
        paymentType: String?
    ): List<Appointment>

    @Transaction
    @Query(
        "SELECT * FROM AppointmentData WHERE AppointmentTime > :startDate AND AppointmentTime < :endDate ORDER BY patient_lastName, AppointmentTime"
    )
    abstract fun getAppointmentSortByLastName(startDate: Long, endDate: Long): LiveData<List<Appointment>>

    @Transaction
    @Query("""DELETE FROM AppointmentData WHERE Id in (:ids)""")
    abstract suspend fun deleteAppointmentsByIds(ids: List<Int>)

    suspend fun deleteAndUpsert(oldAppointments: List<Appointment>, newAppointments: List<Appointment>) {
        delete(oldAppointments)
        upsert(newAppointments)
    }

    @Transaction
    @Query("""SELECT * FROM AppointmentHubMetaData WHERE context = 'ABANDONED'""")
    abstract suspend fun getAbandonedAppointments(): List<AppointmentHubMetaData>

    @Transaction
    @Query("""DELETE FROM AppointmentHubMetaData where appointmentId in (:ids)""")
    abstract suspend fun deleteAppointmentHubMetaDataByIds(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAppointmentHubMetaData(appts: List<AppointmentHubMetaData>)

    @Query("""SELECT * FROM Providers where id = :providerId""")
    protected abstract suspend fun getProviderById(providerId: Int): Provider?

    @Query(
        """
        UPDATE AppointmentData
        SET 
            provider_id = :providerId,
            provider_firstName = :firstName,
            provider_lastName = :lastName
        WHERE 
            id = :appointmentId
        """
    )
    abstract suspend fun updateProvider(
        appointmentId: Int,
        providerId: Int,
        firstName: String,
        lastName: String
    )

    @Transaction
    open suspend fun updateAppointmentProvider(appointmentId: Int, providerId: Int) {
        getProviderById(providerId)?.let {
            updateProvider(
                appointmentId = appointmentId,
                providerId = it.id,
                firstName = it.firstName,
                lastName = it.lastName
            )
        }
    }
}
