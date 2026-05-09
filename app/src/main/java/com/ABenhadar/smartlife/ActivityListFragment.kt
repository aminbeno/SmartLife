package com.ABenhadar.smartlife

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.ActivityData
import com.ABenhadar.smartlife.models.Location
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityListFragment : Fragment() {

    private lateinit var rvActivities: RecyclerView
    private lateinit var adapter: ActivitiesAdapter
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private val auth = FirebaseAuth.getInstance()
    private val apiService by lazy { RetrofitClient.getApiService() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_list, container, false)

        rvActivities = view.findViewById(R.id.rvActivities)
        fabAdd = view.findViewById(R.id.fabAddActivity)

        setupRecyclerView()

        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), ScheduleActivity::class.java)
            startActivity(intent)
        }

        loadAllData()

        return view
    }

    private fun setupRecyclerView() {
        adapter = ActivitiesAdapter()
        rvActivities.layoutManager = LinearLayoutManager(requireContext())
        rvActivities.adapter = adapter
    }

    private fun loadAllData() {
        val userId = auth.currentUser?.uid ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Charger l'historique GPS réel
                val activities = apiService.getActivities(userId)
                
                // 2. Charger le programme (Schedules)
                val schedule = try { 
                    apiService.getWeeklySchedule(userId) 
                } catch (e: Exception) { 
                    null 
                }
                
                // 3. Transformer les tâches planifiées en format compatible pour l'affichage
                val plannedActivities = mutableListOf<ActivityData>()
                schedule?.days?.forEach { day ->
                    day.items.forEach { item ->
                        plannedActivities.add(ActivityData(
                            user_id = userId,
                            type = "[Planifié] ${item.activity_type}",
                            location = Location(0.0, 0.0), // Placeholder
                            timestamp = "${day.day_of_week} à ${item.time}",
                            duration = item.duration,
                            locationName = item.location_name
                        ))
                    }
                }

                withContext(Dispatchers.Main) {
                    // On fusionne les listes : activités réelles (récentes en haut) + programme
                    val combinedList = activities.reversed() + plannedActivities
                    adapter.updateActivities(combinedList)
                }
            } catch (e: Exception) {
                Log.e("ActivityList", "Error loading data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur lors du chargement des données", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
