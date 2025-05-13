package com.example.apz.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.apz.R
import com.example.apz.api.RetrofitInstance
import com.example.apz.repository.ApiRepository
import com.example.apz.viewmodel.NotesViewModel
import com.example.apz.viewmodel.ViewModelFactory

class NotesActivity : ComponentActivity() {

    private lateinit var viewModel: NotesViewModel
    private lateinit var listView: ListView
    private lateinit var notesAdapter: ArrayAdapter<String>
    private val noteTexts = mutableListOf<String>()
    private val noteIds = mutableListOf<Int>()
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        listView = findViewById(R.id.notesListView)
        notesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, noteTexts)
        listView.adapter = notesAdapter

        val noteEditText = findViewById<EditText>(R.id.noteEditText)
        val addButton = findViewById<Button>(R.id.addNoteButton)
        val logoutButton = findViewById<Button>(R.id.buttonLogout)

        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        email = sharedPrefs.getString("email", null)

        if (email == null) {
            Toast.makeText(this, "Користувач не увійшов", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val factory = ViewModelFactory(ApiRepository(RetrofitInstance.api), application)
        viewModel = ViewModelProvider(this, factory)[NotesViewModel::class.java]

        // --- Завантажити нотатки ---
        viewModel.getNotes()

        // --- Відображення нотаток ---
        viewModel.notes.observe(this) { notes ->
            noteTexts.clear()
            noteIds.clear()
            notes.forEach {
                noteTexts.add(it.Text)
                noteIds.add(it.NoteID)
            }
            notesAdapter.notifyDataSetChanged()
        }

        // --- Повідомлення ---
        viewModel.message.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        // --- Додавання нотатки ---
        addButton.setOnClickListener {
            val text = noteEditText.text.toString()
            if (text.isNotBlank()) {
                viewModel.addNote(text)
                noteEditText.text.clear()
                viewModel.getNotes()
            }
        }

        // --- Видалення нотатки ---
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val id = noteIds[position]
            viewModel.deleteNote(id)
            viewModel.getNotes()
            true
        }

        // --- Вихід з акаунту ---
        logoutButton.setOnClickListener {
            sharedPrefs.edit().remove("email").apply()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
