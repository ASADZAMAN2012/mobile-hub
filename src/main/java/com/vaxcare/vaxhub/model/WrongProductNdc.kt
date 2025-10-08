/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.vaxcare.core.model.inventory.WrongProductNdcDto
import com.vaxcare.core.model.inventory.WrongProductNdcEntity

data class WrongProductNdc(
    val ndc: String,
    val errorMessage: String,
)

fun WrongProductNdcDto.toWrongProductNdc() =
    WrongProductNdc(
        ndc,
        errorMessage
    )

fun WrongProductNdcDto.toWrongProductEntity() =
    WrongProductNdcEntity(
        ndc,
        errorMessage
    )

fun WrongProductNdcEntity.toWrongProductNdc() =
    WrongProductNdc(
        ndc = ndc,
        errorMessage = errorMessage
    )
