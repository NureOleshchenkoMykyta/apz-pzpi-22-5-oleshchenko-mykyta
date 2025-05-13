package com.example.apz.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.apz.models.LoginRequest
import com.example.apz.repository.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: ApiRepository) : ViewModel() {

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        val request = LoginRequest(email, password)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("LOGIN_DEBUG", "Надсилаємо запит: $email")
                val response = repository.login(request)
                if (response.isSuccessful) {
                    val role = response.body()?.role
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(true, role)
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(false, null)
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(false, null)
                }
            }
        }
    }
}
