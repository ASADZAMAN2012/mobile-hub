/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    fun getAll(): LiveData<List<User>> = repository.getAll()

    suspend fun getUserAsync(id: Int) = repository.getUserAsync(id)

    suspend fun getUserByPin(pin: String): User? = repository.getUserByPin(pin)

    fun insert(user: User) =
        viewModelScope.safeLaunch {
            repository.insert(user)
        }

    fun insertAll(users: List<User>) =
        viewModelScope.safeLaunch {
            repository.insertAll(users)
        }

    fun deleteAll() =
        viewModelScope.safeLaunch {
            repository.deleteAll()
        }
}
