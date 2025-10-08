/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.R
import kotlinx.android.parcel.Parcelize
import java.time.Instant

@Parcelize
@Entity(tableName = "Payers")
@JsonClass(generateAdapter = true)
data class Payer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Json(name = "portalInsuranceMappingId")
    val portalInsuranceMappingId: Int? = null,
    @Json(name = "insuranceName")
    val insuranceName: String? = null,
    @Json(name = "extensionFlags")
    val extensionFlags: String? = null,
    @Json(name = "state")
    val state: String? = null,
    @Json(name = "insuranceId")
    val insuranceId: Int? = null,
    @Json(name = "insurancePlanId")
    val insurancePlanId: Int? = null,
    // Used for save recent payers
    var updatedTime: Instant? = null
) : Parcelable {
    enum class PayerType(val id: Int) {
        EMPLOYER(-4),
        UNINSURED(-3),
        OTHER(-2),
        SELF(-1)
    }

    fun isEmployerPayer(): Boolean {
        return id == PayerType.EMPLOYER.id
    }

    fun isUninsuredPayer(): Boolean {
        return id == PayerType.UNINSURED.id
    }

    fun isSelfPayer(): Boolean {
        return id == PayerType.SELF.id
    }

    fun isOtherPayer(): Boolean {
        return id == PayerType.OTHER.id
    }

    fun isNormalPayer(): Boolean {
        return id >= 0
    }

    fun getName(): String =
        when {
            isEmployerPayer() -> "Employer Covered"
            isUninsuredPayer() -> "Uninsured"
            isOtherPayer() -> "Other Payer"
            isSelfPayer() -> "Self Pay"
            else -> {
                insuranceName ?: "Unexpected Insurance Name"
            }
        }

    fun getName(context: Context?): String? =
        context?.let {
            when {
                isEmployerPayer() -> {
                    it.resources.getString(R.string.patient_add_select_payer_employer_pay)
                }
                isUninsuredPayer() -> {
                    it.resources.getString(R.string.patient_add_select_payer_uninsured)
                }
                isOtherPayer() -> {
                    it.resources.getString(R.string.patient_add_select_payer_other_payer)
                }
                isSelfPayer() -> {
                    it.resources.getString(R.string.patient_add_select_payer_self_pay)
                }
                else -> {
                    insuranceName
                }
            } ?: insuranceName
        }
}
