/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.data.dao.ShotAdministratorDao
import com.vaxcare.vaxhub.model.ShotAdministrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShotAdministratorViewModel @Inject constructor(private val shotAdministratorDao: ShotAdministratorDao) :
    ViewModel() {
        fun getAll(): LiveData<List<ShotAdministrator>> {
            return shotAdministratorDao.getAll()
        }

        fun deleteAll() {
            viewModelScope.safeLaunch {
                shotAdministratorDao.deleteAll()
            }
        }
    }
