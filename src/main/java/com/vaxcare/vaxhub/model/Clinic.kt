/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.core.extension.isBetween
import java.time.LocalDate

@Entity(tableName = "Clinics")
@JsonClass(generateAdapter = true)
data class Clinic(
    @PrimaryKey val id: Long,
    val name: String,
    val state: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val type: ClinicType,
    val locationId: Long,
    val locationNodeId: String,
    val isIntegrated: Boolean,
    val parentClinicId: Long,
    val isSchoolCaresEnabled: Boolean,
    val temporaryClinicType: String?,
) {
    fun isTemporaryClinic(): Boolean {
        return type == ClinicType.Temporary
    }

    /**
     * Check if the clinic is temp AND start/enddate are within a threshold
     *
     * @param oldest lower threshold (exclusive)
     * @param mostRecent upper threshold (exclusive)
     * @return true if the above criteria is met
     */
    fun isTempDatesBetween(
        oldest: LocalDate = LocalDate.now().minusDays(91),
        mostRecent: LocalDate = LocalDate.now().plusDays(4)
    ): Boolean =
        isTemporaryClinic() && startDate?.isBetween(oldest, mostRecent) == true &&
            (endDate == null || endDate.isBetween(oldest, mostRecent))

    /**
     * This is specifically for the Clinic List adapter
     */
    @Ignore
    var selected: Boolean = false
}
