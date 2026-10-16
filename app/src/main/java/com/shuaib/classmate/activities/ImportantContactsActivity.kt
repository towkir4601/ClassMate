package com.shuaib.classmate.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.adapters.ContactsAdapter
import com.shuaib.classmate.databinding.ActivityImportantContactsBinding
import com.shuaib.classmate.models.User

class ImportantContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportantContactsBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var allContacts = listOf<User>()
    private var searchQuery = ""
    private lateinit var adapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportantContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ContactsAdapter(emptyList())
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter

        setupSearch()

        binding.swipeRefresh.setOnRefreshListener {
            loadContacts()
        }

        loadContacts()
    }

    private fun setupSearch() {
        binding.etSearchContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                filterContacts()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadContacts() {
        binding.progressBar.visibility = View.VISIBLE
        firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                allContacts = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)?.copy(uid = doc.id)
                    val role = user?.role?.lowercase() ?: ""
                    if (role == "teacher" || role == "admin" || role == "superadmin") {
                        user
                    } else null
                }.sortedBy { it.fullName.ifBlank { it.name } }

                filterContacts()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterContacts() {
        val filtered = allContacts.filter { user ->
            if (searchQuery.isBlank()) true
            else {
                user.name.lowercase().contains(searchQuery) ||
                user.fullName.lowercase().contains(searchQuery) ||
                user.department.lowercase().contains(searchQuery) ||
                user.email.lowercase().contains(searchQuery)
            }
        }

        adapter.updateList(filtered)
        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
        }
    }
}
