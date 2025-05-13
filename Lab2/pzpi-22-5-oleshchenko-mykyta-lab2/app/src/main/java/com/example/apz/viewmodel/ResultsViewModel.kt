package com.example.apz.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.apz.models.Result
import com.example.apz.models.ResultRequest
import com.example.apz.repository.ApiRepository
import kotlinx.coroutines.launch

class ResultsViewModel(
    private val repository: ApiRepository,
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)

    private val _results = MutableLiveData<List<Result>>()
    val results: LiveData<List<Result>> = _results

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private fun getEmail(): String? = prefs.getString("email", null)
    private fun getRole(): String? = prefs.getString("role", null)

    fun getResults(context: Context) {
        val email = getEmail()
        val role = getRole()

        if (email == null || role == null) {
            _message.value = "Email або роль не знайдені"
            return
        }

        viewModelScope.launch {
            try {
                val response = repository.getResults(email, role)
                if (response.isSuccessful) {
                    _results.value = response.body() ?: emptyList()
                } else {
                    _message.value = "Помилка при отриманні результатів: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                _message.value = "Помилка: ${e.localizedMessage}"
                Log.e("RESULTS_DEBUG", "Exception: ${e.stackTraceToString()}")
            }
        }
    }

    fun addResult(context: Context, level: Int) {
        val email = getEmail()
        val role = getRole()

        if (email == null || role == null) {
            _message.value = "Email або роль не знайдені"
            return
        }

        val request = ResultRequest(stress_level = level)

        viewModelScope.launch {
            try {
                val response = repository.addResult(email, role, request)
                _message.value = if (response.isSuccessful) {
                    "Результат додано"
                } else {
                    "Помилка при додаванні результату: ${response.code()} ${response.message()}"
                }
                getResults(context)
            } catch (e: Exception) {
                _message.value = "Помилка: ${e.localizedMessage}"
                Log.e("RESULTS_DEBUG", "Exception: ${e.stackTraceToString()}")
            }
        }
    }

    fun deleteResult(context: Context, resultId: Int) {
        val email = getEmail()
        val role = getRole()

        if (email == null || role == null) {
            _message.value = "Email або роль не знайдені"
            return
        }

        viewModelScope.launch {
            try {
                val response = repository.deleteResult(email, resultId, role)
                _message.value = if (response.isSuccessful) {
                    "Результат видалено"
                } else {
                    "Помилка при видаленні результату"
                }
                getResults(context)
            } catch (e: Exception) {
                _message.value = "Помилка: ${e.localizedMessage}"
                Log.e("RESULTS_DEBUG", "Exception: ${e.stackTraceToString()}")
            }
        }
    }
}
