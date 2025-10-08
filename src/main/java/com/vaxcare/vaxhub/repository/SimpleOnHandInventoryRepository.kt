/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import com.vaxcare.vaxhub.data.dao.SimpleOnHandInventoryDao
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProduct
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProductDTO
import com.vaxcare.vaxhub.model.inventory.toSimpleOnHandProduct
import com.vaxcare.vaxhub.web.InventoryApi
import javax.inject.Inject

interface SimpleOnHandInventoryRepository {
    suspend fun fetchAndSaveSimpleOnHandInventory(isCalledByJob: Boolean = false)

    suspend fun getSimpleOnHandProductsByLotName(lotNumberName: String): List<SimpleOnHandProduct>
}

class SimpleOnHandInventoryRepositoryImpl @Inject constructor(
    private val simpleOnHandInventoryDao: SimpleOnHandInventoryDao,
    private val inventoryApi: InventoryApi
) : SimpleOnHandInventoryRepository {
    // This function has some filtering logic that will need to be removed after server side fix. TODO: Remove this after fix
    // Context here: https://teams.microsoft.com/l/message/19:_YN47-_1y3hLTQD1Me_VpL_NRO26G262cDeArNKov8E1@thread.tacv2/1697034469205?tenantId=809ba6be-b3bb-4f08-a260-65732b2a2b36&groupId=805cdabb-d1d9-4ddb-9d13-2c26f3f676a0&parentMessageId=1697034469205&teamName=Mobile%20Scrum%20Team&channelName=General&createdTime=1697034469205
    override suspend fun fetchAndSaveSimpleOnHandInventory(isCalledByJob: Boolean) {
        val onHandInventory = inventoryApi.getSimpleOnHandInventory(isCalledByJob)
            .groupBy { it.lotNumberName } // TODO: After fix remove from here
            .flatMap { onHandProductsByLotNumber ->
                onHandProductsByLotNumber.value
                    .groupBy { it.inventorySource }
                    .map { onHandProductsByInventorySource ->
                        val listOfOnHandProduct = onHandProductsByInventorySource.value

                        val lotNumberName = listOfOnHandProduct.first().lotNumberName
                        val inventorySource = onHandProductsByInventorySource.key
                        val onHandAmount = listOfOnHandProduct.sumOf { it.onHandAmount }

                        SimpleOnHandProductDTO(lotNumberName, inventorySource, onHandAmount)
                    }
            } // TODO: Until here
        simpleOnHandInventoryDao.insertAll(onHandInventory)
    }

    override suspend fun getSimpleOnHandProductsByLotName(lotNumberName: String): List<SimpleOnHandProduct> =
        simpleOnHandInventoryDao.getAllAsync()
            .filter { it.lotNumberName == lotNumberName }
            .map { it.toSimpleOnHandProduct() }
}
