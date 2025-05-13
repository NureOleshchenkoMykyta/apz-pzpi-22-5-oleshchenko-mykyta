package com.example.apz.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apz.repository.ApiRepository

class ViewModelFactory(
    private val repository: ApiRepository,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(repository) as T

            modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
                RegisterViewModel(repository) as T

            modelClass.isAssignableFrom(ResultsViewModel::class.java) ->
                ResultsViewModel(repository, application) as T

            modelClass.isAssignableFrom(NotesViewModel::class.java) ->
                NotesViewModel(repository, application) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
