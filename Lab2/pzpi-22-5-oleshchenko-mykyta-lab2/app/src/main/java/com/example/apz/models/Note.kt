package com.example.apz.models

data class Note(
    val NoteID: Int,
    val Text: String,
    val CreationDate: String,
    val AccountID: Int
)

data class NoteRequest(
    val text: String
)
