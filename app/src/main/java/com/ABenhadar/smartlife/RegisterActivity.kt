package com.ABenhadar.smartlife

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ABenhadar.smartlife.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialisation de la logique
        setupRegisterLogic()
        
        // Application des animations
        applyAnimations()
    }

    private fun applyAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)

        findViewById<ImageView>(R.id.ivRegisterLogo).startAnimation(fadeIn)
        findViewById<TextView>(R.id.tvRegisterTitle).startAnimation(fadeInUp)
        findViewById<TextView>(R.id.tvRegisterSubtitle).startAnimation(fadeInUp)
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvRegisterForm).startAnimation(fadeInUp)
        findViewById<TextView>(R.id.tvLogin).startAnimation(fadeIn)
    }

    private fun setupRegisterLogic() {
        auth = FirebaseAuth.getInstance()

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etBirthDate = findViewById<EditText>(R.id.etBirthDate)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnRegister.isEnabled = !isLoading
        }

        viewModel.successMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, "Compte créé avec succès", Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
                updateFCMToken()
                
                lifecycleScope.launch {
                    delay(500)
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, "Erreur : $error", Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            
            if (email.isEmpty() || password.isEmpty() || etConfirmPassword.text.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != etConfirmPassword.text.toString()) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        auth?.currentUser?.let {
                            viewModel.registerUser(
                                it.uid, email, 
                                etFirstName.text.toString().trim(), 
                                etLastName.text.toString().trim(), 
                                etBirthDate.text.toString().trim()
                            )
                        }
                    } else {
                        Toast.makeText(this, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvLogin.setOnClickListener {
            onBackPressed() // Retour au Login
        }
    }

    private fun updateFCMToken() {
        try {
            val userId = auth?.currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModel.updateFCMToken(userId, task.result)
                }
            }
        } catch (e: Exception) {
            Log.e("RegisterActivity", "FCM Token error: ${e.message}")
        }
    }
}