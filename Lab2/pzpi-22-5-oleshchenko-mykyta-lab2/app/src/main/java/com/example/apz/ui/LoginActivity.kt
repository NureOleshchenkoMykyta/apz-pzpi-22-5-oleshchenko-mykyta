package com.example.apz.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.apz.R
import com.example.apz.api.RetrofitInstance
import com.example.apz.repository.ApiRepository
import com.example.apz.storage.saveEmail
import com.example.apz.viewmodel.LoginViewModel
import com.example.apz.viewmodel.ViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonToRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonToRegister = findViewById(R.id.buttonToRegister)

        val repository = ApiRepository(RetrofitInstance.api)
        val factory = ViewModelFactory(repository, application)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Введіть email і пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password) { success, role ->
                runOnUiThread {
                    if (success) {
                        val prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                        prefs.edit()
                            .putString("email", email)
                            .putString("role", role)
                            .apply()
                        val intent = Intent(this, ResultsActivity::class.java)
                        intent.putExtra("ROLE", role)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Неправильний email або пароль", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        buttonToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
