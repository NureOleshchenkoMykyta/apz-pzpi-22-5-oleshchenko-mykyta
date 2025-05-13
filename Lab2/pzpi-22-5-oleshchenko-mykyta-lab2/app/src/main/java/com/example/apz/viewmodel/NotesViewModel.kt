package com.example.apz.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.apz.models.Note
import com.example.apz.repository.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: ApiRepository, application: Application) : AndroidViewModel(application) {

    private val _notes = MutableLiveData<List<Note>>().apply { postValue(emptyList()) }
    val notes: LiveData<List<Note>> get() = _notes

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    @OptIn(UnstableApi::class)
    fun getNotes() {
        val email = getEmailFromPrefs()
        val role = getRoleFromPrefs()
        Log.d("NOTE_DEBUG", "getNotes called with email=$email, role=$role")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.getNotes(email, role)
                if (response.isSuccessful) {
                    _notes.postValue(response.body() ?: emptyList())
                } else {
                    _notes.postValue(emptyList())
                    _message.postValue("Не вдалося отримати нотатки")
                }
            } catch (e: Exception) {
                _notes.postValue(emptyList())
                _message.postValue("Помилка: ${e.localizedMessage}")
            }
        }
    }

    fun addNote(text: String) {
        val email = getEmailFromPrefs()
        val role = getRoleFromPrefs()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.addNote(email, role, text)
                if (response.isSuccessful) {
                    getNotes()
                    _message.postValue("Нотатку додано")
                } else {
                    _message.postValue("Не вдалося додати нотатку")
                }
            } catch (e: Exception) {
                _message.postValue("Помилка: ${e.localizedMessage}")
            }
        }
    }

    fun updateNote(noteId: Int, text: String) {
        val email = getEmailFromPrefs()
        val role = getRoleFromPrefs()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.updateNote(noteId, email, role, text)
                if (response.isSuccessful) {
                    getNotes()
                    _message.postValue("Нотатку оновлено")
                } else {
                    _message.postValue("Не вдалося оновити нотатку")
                }
            } catch (e: Exception) {
                _message.postValue("Помилка: ${e.localizedMessage}")
            }
        }
    }

    fun deleteNote(noteId: Int) {
        val email = getEmailFromPrefs()
        val role = getRoleFromPrefs()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.deleteNote(noteId, email, role)
                if (response.isSuccessful) {
                    getNotes()
                    _message.postValue("Нотатку видалено")
                } else {
                    _message.postValue("Не вдалося видалити нотатку")
                }
            } catch (e: Exception) {
                _message.postValue("Помилка: ${e.localizedMessage}")
            }
        }
    }

    private fun getEmailFromPrefs(): String {
        return getApplication<Application>().getSharedPreferences("APP_PREFS", 0).getString("email", "") ?: ""
    }

    private fun getRoleFromPrefs(): String {
        return getApplication<Application>().getSharedPreferences("APP_PREFS", 0).getString("role", "") ?: ""
    }
}