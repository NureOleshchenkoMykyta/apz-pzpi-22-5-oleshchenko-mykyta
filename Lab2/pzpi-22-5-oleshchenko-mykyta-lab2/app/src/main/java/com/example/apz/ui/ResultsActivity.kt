package com.example.apz.ui

import com.example.apz.models.NoteRequest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.example.apz.R
import com.example.apz.api.RetrofitInstance
import com.example.apz.repository.ApiRepository
import com.example.apz.storage.getEmail
import com.example.apz.viewmodel.NotesViewModel
import com.example.apz.viewmodel.ResultsViewModel
import com.example.apz.viewmodel.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResultsActivity : ComponentActivity() {

    private lateinit var resultViewModel: ResultsViewModel
    private lateinit var noteViewModel: NotesViewModel
    private lateinit var resultsContainer: LinearLayout
    private lateinit var notesContainer: LinearLayout
    private var sortDescending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val email = getEmail(this)
        if (email == null) {
            Toast.makeText(this, "Email відсутній, повторіть вхід", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val factory = ViewModelFactory(ApiRepository(RetrofitInstance.api), application)
        resultViewModel = ViewModelProvider(this, factory)[ResultsViewModel::class.java]
        noteViewModel = ViewModelProvider(this, factory)[NotesViewModel::class.java]

        resultsContainer = findViewById(R.id.resultsContainer)
        notesContainer = findViewById(R.id.notesContainer)
        val input = findViewById<EditText>(R.id.stressLevelInput)
        val addButton = findViewById<Button>(R.id.addResultButton)
        val logoutButton = findViewById<Button>(R.id.buttonLogout)
        val sortSpinner = findViewById<Spinner>(R.id.sortSpinner)
        val addNoteButton = findViewById<Button>(R.id.addNoteButton)

        val sortOptions = listOf("Сортувати: нові → старі", "Сортувати: старі → нові")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortOptions)
        sortSpinner.adapter = spinnerAdapter
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                sortDescending = position == 0
                resultViewModel.getResults(this@ResultsActivity)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        resultViewModel.results.observe(this) { results -> displayResults(results) }
        noteViewModel.notes.observe(this) { notes -> displayNotes(notes) }
        resultViewModel.message.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        noteViewModel.message.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }

        addButton.setOnClickListener {
            val level = input.text.toString().toIntOrNull()
            if (level != null) {
                resultViewModel.addResult(this, level)
                input.text.clear()
            } else {
                Toast.makeText(this, "Некоректне значення", Toast.LENGTH_SHORT).show()
            }
        }

        addNoteButton.setOnClickListener {
            val emailVal = getEmail(this)
            val role = getSharedPreferences("APP_PREFS", MODE_PRIVATE).getString("role", null)

            if (emailVal == null || role == null) {
                Toast.makeText(this, "Неможливо створити нотатку — не знайдено email або роль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Нова нотатка")
                .setView(input)
                .setPositiveButton("Зберегти") { _, _ ->
                    val text = input.text.toString()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = RetrofitInstance.api.addNote(
                                email = emailVal,
                                role = role,
                                request = NoteRequest(text)
                            )
                            if (response.isSuccessful) {
                                runOnUiThread {
                                    noteViewModel.getNotes()
                                    Toast.makeText(this@ResultsActivity, "Нотатку додано", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this@ResultsActivity, "Помилка при створенні нотатки", Toast.LENGTH_SHORT).show()
                                }
                            }

                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(this@ResultsActivity, "Помилка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Скасувати", null)
                .show()
        }

        logoutButton.setOnClickListener {
            getSharedPreferences("APP_PREFS", MODE_PRIVATE).edit().remove("email").remove("role").apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        resultViewModel.getResults(this)
        noteViewModel.getNotes()
    }

    private fun displayResults(results: List<com.example.apz.models.Result>) {
        resultsContainer.removeAllViews()

        val header = TextView(this).apply {
            text = "Результати аналізу"
            textSize = 20f
            setPadding(16, 16, 16, 16)
        }
        resultsContainer.addView(header)

        val sorted = if (sortDescending) results.sortedByDescending { it.AnalysisDate } else results.sortedBy { it.AnalysisDate }

        for (res in sorted) {
            val view = layoutInflater.inflate(R.layout.result_item, null)
            val resultText = view.findViewById<TextView>(R.id.resultTextView)
            resultText.text = "Стрес: ${res.StressLevel} | Стан: ${res.EmotionalState} (${res.AnalysisDate})"

            view.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Видалити результат?")
                    .setMessage("Цей результат буде видалено назавжди.")
                    .setPositiveButton("Так") { _, _ ->
                        resultViewModel.deleteResult(this, res.ResultID)
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
                true
            }
            resultsContainer.addView(view)
        }
    }

    private fun displayNotes(notes: List<com.example.apz.models.Note>) {
        notesContainer.removeAllViews()

        if (notes.isEmpty()) return

        val header = TextView(this).apply {
            text = "Ваші нотатки"
            textSize = 20f
            setPadding(16, 32, 16, 16)
        }
        notesContainer.addView(header)

        for (note in notes) {
            val noteView = layoutInflater.inflate(R.layout.note_item, null)
            val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
            val editNoteBtn = noteView.findViewById<Button>(R.id.editNoteButton)
            val deleteNoteBtn = noteView.findViewById<Button>(R.id.deleteNoteButton)

            noteTextView.text = "${note.Text} (${note.CreationDate})"

            editNoteBtn.setOnClickListener {
                showEditNoteDialog(note.NoteID, note.AccountID, note.Text)
            }

            deleteNoteBtn.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Видалити нотатку?")
                    .setMessage("Ця нотатка буде видалена назавжди.")
                    .setPositiveButton("Так") { _, _ ->
                        noteViewModel.deleteNote(note.NoteID)
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            }

            notesContainer.addView(noteView)
        }
    }

    private fun showEditNoteDialog(noteId: Int?, accountId: Int, oldText: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Змінити нотатку")

        val input = EditText(this)
        input.setText(oldText)
        builder.setView(input)

        builder.setPositiveButton("Зберегти") { _, _ ->
            val newText = input.text.toString()
            if (noteId != null) {
                noteViewModel.updateNote(noteId, newText)
            } else {
                noteViewModel.addNote(newText)
            }
        }
        builder.setNegativeButton("Скасувати", null)
        builder.show()
    }
}