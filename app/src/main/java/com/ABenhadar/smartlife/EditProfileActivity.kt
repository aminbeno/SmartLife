package com.ABenhadar.smartlife

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ABenhadar.smartlife.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var etEditFirstName: EditText
    private lateinit var etEditLastName: EditText
    private lateinit var etEditBirthDate: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        val userEmail = auth.currentUser?.email

        etEditFirstName = findViewById(R.id.etEditFirstName)
        etEditLastName = findViewById(R.id.etEditLastName)
        etEditBirthDate = findViewById(R.id.etEditBirthDate)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        progressBar = findViewById(R.id.progressBar)

        // Load existing user data
        userId?.let { viewModel.getUser(it) }

        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                etEditFirstName.setText(user.firstName)
                etEditLastName.setText(user.lastName)
                etEditBirthDate.setText(user.birthDate)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSaveProfile.isEnabled = !isLoading
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let { 
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                finish() // Go back to ProfileActivity after saving
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let { Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show() }
        }

        etEditBirthDate.setOnClickListener { showDatePicker() }
        etEditBirthDate.keyListener = null // Disable keyboard input for birth date

        btnSaveProfile.setOnClickListener {
            val newFirstName = etEditFirstName.text.toString().trim()
            val newLastName = etEditLastName.text.toString().trim()
            val newBirthDate = etEditBirthDate.text.toString().trim()

            if (newFirstName.isEmpty() || newLastName.isEmpty() || newBirthDate.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            userId?.let {
                userEmail?.let {
                    viewModel.updateUser(it, userEmail, newFirstName, newLastName, newBirthDate)
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                etEditBirthDate.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }
}
