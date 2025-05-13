package com.example.apz.repository

import com.example.apz.api.ApiService
import com.example.apz.models.*
import retrofit2.Response

class ApiRepository(private val api: ApiService) {

    suspend fun register(request: RegisterRequest) = api.register(request)

    suspend fun login(request: LoginRequest) = api.login(request)

    suspend fun getResults(email: String, role: String) = api.getResults(email, role)

    suspend fun addResult(email: String, role: String, request: ResultRequest) =
        api.addResult(email, role, request)

    suspend fun deleteResult(email: String, resultId: Int, role: String) =
        api.deleteResult(email, resultId, role)

    suspend fun updateResult(email: String, resultId: Int, role: String, request: ResultRequest) =
        api.updateResult(email, resultId, role, request)

    suspend fun getNotes(email: String, role: String): Response<List<Note>> {
        return api.getNotes(email, role)
    }


    suspend fun addNote(email: String, role: String, text: String): Response<MessageResponse> {
        return api.addNote(email, role, NoteRequest(text))
    }


    suspend fun updateNote(noteId: Int, email: String, role: String, text: String): Response<MessageResponse> {
        return api.updateNote(noteId, email, role, NoteRequest(text))
    }

    suspend fun deleteNote(noteId: Int, email: String, role: String): Response<MessageResponse> {
        return api.deleteNote(noteId, email, role)
    }

}
