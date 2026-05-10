package com.ABenhadar.smartlife

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.ActivityData
import com.ABenhadar.smartlife.models.Location
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ActivityListFragment : Fragment() {

    private lateinit var rvActivities: RecyclerView
    private lateinit var adapter: ActivitiesAdapter
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup

    private val auth = FirebaseAuth.getInstance()
    private val apiService by lazy { RetrofitClient.getApiService() }

    private var allActivities = mutableListOf<ActivityData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_list, container, false)

        rvActivities = view.findViewById(R.id.rvActivities)
        fabAdd = view.findViewById(R.id.fabAddActivity)
        searchView = view.findViewById(R.id.searchViewActivities)
        chipGroup = view.findViewById(R.id.chipGroupFilters)

        setupRecyclerView()
        setupFilters()

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

    private fun setupFilters() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters()
                return true
            }
        })

        chipGroup.setOnCheckedStateChangeListener { _, _ ->
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = searchView.query.toString().lowercase()
        val checkedChipId = chipGroup.checkedChipId

        val filtered = allActivities.filter { activity ->
            val matchesQuery = activity.type.lowercase().contains(query) || 
                              (activity.locationName?.lowercase()?.contains(query) ?: false)
            
            val matchesType = when (checkedChipId) {
                R.id.chipReal -> !activity.type.startsWith("[Planifié]")
                R.id.chipPlanned -> activity.type.startsWith("[Planifié]")
                else -> true
            }
            
            matchesQuery && matchesType
        }
        
        // Tri par date décroissante (plus récent en haut)
        val sortedList = filtered.sortedByDescending { it.timestamp ?: "" }
        adapter.updateActivities(sortedList)
    }

    private fun loadAllData() {
        val userId = auth.currentUser?.uid ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Charger l'historique GPS réel
                val realActivities = apiService.getActivities(userId)
                
                // 2. Charger le programme planifié
                val schedule = try { apiService.getWeeklySchedule(userId) } catch (e: Exception) { null }
                
                val plannedActivities = mutableListOf<ActivityData>()
                schedule?.days?.forEach { day ->
                    val dayDate = getDateForWeekday(day.day_of_week)
                    day.items.forEach { item ->
                        plannedActivities.add(ActivityData(
                            user_id = userId,
                            type = "[Planifié] ${item.activity_type}",
                            location = Location(0.0, 0.0),
                            timestamp = "${dayDate}T${item.time}:00", // Format ISO pour le tri
                            duration = item.duration,
                            locationName = item.location_name
                        ))
                    }
                }

                allActivities.clear()
                allActivities.addAll(realActivities)
                allActivities.addAll(plannedActivities)

                withContext(Dispatchers.Main) {
                    applyFilters()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur de chargement", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Calcule la date réelle pour un nom de jour (ex: "Lundi") dans la semaine en cours
    private fun getDateForWeekday(dayName: String): String {
        val calendar = Calendar.getInstance(Locale.FRANCE)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
        val index = days.indexOfFirst { it.equals(dayName, ignoreCase = true) }
        if (index != -1) {
            calendar.add(Calendar.DAY_OF_MONTH, index)
        }
        
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}
