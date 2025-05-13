package com.example.apz.ui

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.apz.R
import com.example.apz.api.RetrofitInstance
import com.example.apz.models.RegisterRequest
import com.example.apz.repository.ApiRepository
import com.example.apz.viewmodel.RegisterViewModel
import com.example.apz.viewmodel.ViewModelFactory

class RegisterActivity : ComponentActivity() {

    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameInput = findViewById<EditText>(R.id.editTextName)
        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        val factory = ViewModelFactory(ApiRepository(RetrofitInstance.api), application)
        viewModel = ViewModelProvider(this, factory)[RegisterViewModel::class.java]

        registerButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                viewModel.register(RegisterRequest(email, password, name))
            } else {
                Toast.makeText(this, "Заповніть усі поля", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.message.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
