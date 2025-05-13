package com.example.apz.api

import com.example.apz.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("results")
    suspend fun getResults(
        @Query("email") email: String,
        @Header("Role") role: String
    ): Response<List<Result>>

    @POST("results")
    suspend fun addResult(
        @Query("email") email: String,
        @Header("Role") role: String,
        @Body request: ResultRequest
    ): Response<MessageResponse>

    @DELETE("results")
    suspend fun deleteResult(
        @Query("email") email: String,
        @Query("result_id") resultId: Int,
        @Header("Role") role: String
    ): Response<MessageResponse>

    @PUT("results")
    suspend fun updateResult(
        @Query("email") email: String,
        @Query("result_id") resultId: Int,
        @Header("Role") role: String,
        @Body request: ResultRequest
    ): Response<MessageResponse>

    @GET("accounts/notes")
    suspend fun getNotes(
        @Query("email") email: String,
        @Header("Role") role: String
    ): Response<List<Note>>



    @POST("notes")
    suspend fun addNote(
        @Header("Email") email: String,
        @Header("Role") role: String,
        @Body request: NoteRequest
    ): Response<MessageResponse>


    @PUT("notes/{noteId}")
    suspend fun updateNote(
        @Path("noteId") noteId: Int,
        @Header("Email") email: String,
        @Header("Role") role: String,
        @Body request: NoteRequest
    ): Response<MessageResponse>


    @DELETE("notes/{noteId}")
    suspend fun deleteNote(
        @Path("noteId") noteId: Int,
        @Header("Email") email: String,
        @Header("Role") role: String
    ): Response<MessageResponse>

}
