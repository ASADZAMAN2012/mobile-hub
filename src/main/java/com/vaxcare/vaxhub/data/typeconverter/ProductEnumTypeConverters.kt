/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.typeconverter

import androidx.room.TypeConverter
import com.vaxcare.vaxhub.model.enums.Gender
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.enums.ProductPresentation
import com.vaxcare.vaxhub.model.enums.ProductStatus
import com.vaxcare.vaxhub.model.enums.RouteCode

class ProductEnumTypeConverters {
    @TypeConverter
    fun fromProductCategory(cat: ProductCategory): Int = cat.id

    @TypeConverter
    fun toProductCategory(int: Int) = ProductCategory.fromInt(int)

    @TypeConverter
    fun fromProductStatus(status: ProductStatus) = status.id

    @TypeConverter
    fun toProductStatus(int: Int) = ProductStatus.fromInt(int)

    @TypeConverter
    fun fromRouteCode(route: RouteCode) = route.ordinal

    @TypeConverter
    fun toRouteCode(int: Int) = RouteCode.fromInt(int)

    @TypeConverter
    fun fromProductPresentation(pres: ProductPresentation) = pres.ordinal

    @TypeConverter
    fun toProductPresentation(int: Int) = ProductPresentation.fromInt(int)

    @TypeConverter
    fun fromGender(gender: Gender) = gender.ordinal

    @TypeConverter
    fun toGender(int: Int) = Gender.fromInt(int)
}
