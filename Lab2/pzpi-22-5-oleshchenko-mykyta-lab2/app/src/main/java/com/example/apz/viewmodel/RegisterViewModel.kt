package com.example.apz.viewmodel

import androidx.lifecycle.*
import com.example.apz.models.RegisterRequest
import com.example.apz.repository.ApiRepository
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: ApiRepository) : ViewModel() {

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            val response = repository.register(request)
            _message.value = if (response.isSuccessful) {
                response.body()?.message ?: "Успішно зареєстровано"
            } else {
                "Помилка при реєстрації"
            }
        }
    }
}
