package com.ABenhadar.smartlife

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ABenhadar.smartlife.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvEmail = findViewById(R.id.tvEmail)
        tvBirthDate = findViewById(R.id.tvBirthDate)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        progressBar = findViewById(R.id.progressBar)

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnEditProfile.isEnabled = !isLoading
        }

        // Observe user data
        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                tvFirstName.text = user.firstName
                tvLastName.text = user.lastName
                tvEmail.text = user.email
                tvBirthDate.text = user.birthDate
            }
        }

        // Load current user data
        auth.currentUser?.uid?.let { viewModel.getUser(it) }

        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Observe success/error messages
        viewModel.successMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
        viewModel.errorMessage.observe(this) { error ->
            error?.let { Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning from EditProfileActivity
        auth.currentUser?.uid?.let { viewModel.getUser(it) }
    }
}
