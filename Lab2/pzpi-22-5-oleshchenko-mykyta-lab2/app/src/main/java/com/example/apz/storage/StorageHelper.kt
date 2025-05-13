package com.example.apz.storage

import android.content.Context
import android.content.SharedPreferences

fun saveEmail(email: String, context: Context) {
    val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
    prefs.edit().putString("email", email).apply()
}

fun getEmail(context: Context): String? {
    val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
    return prefs.getString("email", null)
}
