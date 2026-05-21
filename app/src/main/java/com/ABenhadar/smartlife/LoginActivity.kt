package com.ABenhadar.smartlife

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val ivLogo = findViewById<ImageView>(R.id.ivLoginLogo)
        val tvTitle = findViewById<TextView>(R.id.tvLoginTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvLoginSubtitle)
        val loginContainer = findViewById<LinearLayout>(R.id.loginContainer)

        // Animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)

        ivLogo.startAnimation(fadeIn)
        tvTitle.startAnimation(fadeInUp)
        tvSubtitle.startAnimation(fadeInUp)
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvLoginForm).startAnimation(fadeInUp)
        tvRegister.startAnimation(fadeIn)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Erreur : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            // No finish() here to allow back button to return to login
        }
    }
}