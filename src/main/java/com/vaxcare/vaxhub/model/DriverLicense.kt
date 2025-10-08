/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Parcelize
class DriverLicense(
    var firstName: String? = null,
    var middleName: String? = null,
    var lastName: String? = null,
    var gender: Gender? = null,
    var addressStreet: String? = null,
    var addressCity: String? = null,
    var addressState: String? = null,
    var addressZip: String? = null,
    var licenseNumber: String? = null,
    var birthDate: LocalDate? = null
) : Parcelable {
    enum class Gender(val value: String) {
        MALE("Male"),
        FEMALE("Female");

        companion object {
            private val map = values().associateBy(Gender::value)

            fun fromString(type: String?) = map[type]

            fun fromInt(value: Int) =
                when (value) {
                    0 -> MALE
                    else -> FEMALE
                }
        }
    }

    companion object {
        fun convertBarCodeToDriverLicense(barcode: String): DriverLicense {
            val driverLicense = DriverLicense()
            val data: List<String> = barcode.split("\n")

            data.forEachIndexed continuing@{ index, data ->
                val item = if (index == 1) {
                    // Strip header and subfile designators to get first data item
                    data.replace(Regex("^[^\n]+DL"), "")
                } else {
                    data
                }

                if (item.length > 3) {
                    val code = try {
                        BarcodeEnum.valueOf(item.substring(0, 3).trim())
                    } catch (e: Exception) {
                        return@continuing
                    }
                    when (code) {
                        BarcodeEnum.DAA -> {
                            val name = item.substring(3, item.length)
                            val nameArray = name.split(",")
                            driverLicense.lastName = if (nameArray.isNotEmpty()) nameArray[0] else ""
                            driverLicense.firstName = if (nameArray.size > 1) nameArray[1] else ""
                            driverLicense.middleName = if (nameArray.size > 2) nameArray[2] else ""
                        }
                        BarcodeEnum.DAB,
                        BarcodeEnum.DCS -> {
                            driverLicense.lastName = item.substring(3, item.length)
                        }
                        BarcodeEnum.DAC,
                        BarcodeEnum.DCT -> {
                            driverLicense.firstName = item.substring(3, item.length)
                        }
                        BarcodeEnum.DAD -> {
                            driverLicense.middleName = item.substring(3, item.length)
                        }
                        BarcodeEnum.DAG -> {
                            driverLicense.addressStreet = item.substring(3, item.length)
                        }
                        BarcodeEnum.DAI,
                        BarcodeEnum.DAN -> {
                            driverLicense.addressCity = item.substring(3, item.length)
                        }
                        BarcodeEnum.DAJ,
                        BarcodeEnum.DAO -> {
                            driverLicense.addressState = item.substring(3, item.length)
                        }
                        BarcodeEnum.DAK,
                        BarcodeEnum.DAP -> {
                            driverLicense.addressZip = item.substring(3, item.length)
                        }
                        BarcodeEnum.DAQ -> {
                            driverLicense.licenseNumber = item.substring(3, item.length)
                        }
                        BarcodeEnum.DBB -> {
                            driverLicense.birthDate = try {
                                LocalDate.parse(
                                    item.substring(3, item.length),
                                    DateTimeFormatter.ofPattern("yyyyMMdd")
                                )
                            } catch (e: Exception) {
                                try {
                                    LocalDate.parse(
                                        item.substring(3, item.length),
                                        DateTimeFormatter.ofPattern("MMddyyyy")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        BarcodeEnum.DBC -> {
                            // "F" or "M"
                            when (item.substring(3, item.length)) {
                                "F", "2" -> driverLicense.gender = Gender.FEMALE
                                "M", "1" -> driverLicense.gender = Gender.MALE
                            }
                        }
                    }
                }
            }
            return driverLicense
        }
    }
}
