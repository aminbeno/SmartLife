package com.ABenhadar.smartlife

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        auth = FirebaseAuth.getInstance()

        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        val userEmail = auth.currentUser?.email ?: "Utilisateur"
        tvGreeting.text = "Bonjour, ${userEmail.substringBefore("@")} !"

        return view
    }
}