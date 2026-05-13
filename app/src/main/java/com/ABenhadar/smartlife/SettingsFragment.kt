package com.ABenhadar.smartlife

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        auth = FirebaseAuth.getInstance()

        val switchDarkMode = view.findViewById<MaterialSwitch>(R.id.switchDarkMode)
        val switchNotifications = view.findViewById<MaterialSwitch>(R.id.switchNotifications)
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnSetGoals = view.findViewById<MaterialButton>(R.id.btnSetGoals)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)
        val btnAbout = view.findViewById<MaterialButton>(R.id.btnAbout)

        // Gestion du Mode Sombre
        val sharedPref = requireActivity().getSharedPreferences("smartlife_prefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Gestion des Notifications/Recommandations IA (Persistance simple)
        switchNotifications.isChecked = sharedPref.getBoolean("ai_recommendations", true)
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("ai_recommendations", isChecked).apply()
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        btnSetGoals.setOnClickListener {
            startActivity(Intent(requireContext(), ScheduleActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        btnAbout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("À propos de SmartLife")
                .setMessage("SmartLife v1.1\nVotre compagnon bienveillant pour une vie équilibrée.\n\nDéveloppé pour vous aider à suivre vos habitudes et atteindre vos objectifs au quotidien.")
                .setPositiveButton("OK", null)
                .show()
        }

        return view
    }
}
