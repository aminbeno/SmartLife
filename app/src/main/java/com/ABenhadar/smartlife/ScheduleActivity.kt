package com.ABenhadar.smartlife

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.DaySchedule
import com.ABenhadar.smartlife.models.NamedLocation
import com.ABenhadar.smartlife.models.ScheduleItem
import com.ABenhadar.smartlife.models.weeklySchedule
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var rvSchedule: RecyclerView
    private lateinit var adapter: ScheduleAdapter
    private lateinit var fabAdd: FloatingActionButton
    private val auth = FirebaseAuth.getInstance()
    private val apiService by lazy { RetrofitClient.getApiService() }

    private val daysOfWeek = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    private var currentWeeklySchedule = weeklySchedule("", emptyList())
    private var currentDayIndex = 0
    private var namedLocations: List<NamedLocation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tabLayout = findViewById(R.id.tabLayoutDays)
        rvSchedule = findViewById(R.id.rvSchedule)
        fabAdd = findViewById(R.id.fabAddTask)

        setupTabs()
        setupRecyclerView()

        fabAdd.setOnClickListener { showAddTaskDialog() }

        loadSchedule()
        loadNamedLocations()
    }

    private fun setupTabs() {
        daysOfWeek.forEach { day ->
            tabLayout.addTab(tabLayout.newTab().setText(day))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentDayIndex = tab?.position ?: 0
                updateDayList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = ScheduleAdapter { position ->
            deleteTask(position)
        }
        rvSchedule.layoutManager = LinearLayoutManager(this)
        rvSchedule.adapter = adapter
    }

    private fun loadSchedule() {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val schedule = apiService.getWeeklySchedule(userId)
                withContext(Dispatchers.Main) {
                    currentWeeklySchedule = schedule
                    updateDayList()
                }
            } catch (e: Exception) {
                currentWeeklySchedule = weeklySchedule(userId, daysOfWeek.map { DaySchedule(it) })
                withContext(Dispatchers.Main) { updateDayList() }
            }
        }
    }

    private fun loadNamedLocations() {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val locations = apiService.getNamedLocations(userId)
                withContext(Dispatchers.Main) {
                    namedLocations = locations
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScheduleActivity, "Impossible de charger vos lieux", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDayList() {
        val dayName = daysOfWeek[currentDayIndex]
        val daySchedule = currentWeeklySchedule.days.find { it.day_of_week == dayName }
        adapter.updateItems(daySchedule?.items ?: emptyList())
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etTime = dialogView.findViewById<EditText>(R.id.etTaskTime)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerActivityType)
        val spinnerLocation = dialogView.findViewById<Spinner>(R.id.spinnerTaskLocation)
        val etDuration = dialogView.findViewById<EditText>(R.id.etTaskDuration)

        // 1. Gestion de l'heure via TimePicker
        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                etTime.setText(timeStr)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // 2. Spinner pour les types d'activités
        val activities = listOf("Walking", "Running", "Stationary", "Sport", "Repos")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activities)

        // 3. Spinner pour les lieux nommés (fetchés depuis la DB)
        val locationNames = if (namedLocations.isEmpty()) listOf("Aucun lieu enregistré") else namedLocations.map { it.name }
        spinnerLocation.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locationNames)

        AlertDialog.Builder(this)
            .setTitle("Planifier une activité")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val time = etTime.text.toString()
                val type = spinnerType.selectedItem.toString()
                val loc = if (namedLocations.isNotEmpty()) spinnerLocation.selectedItem.toString() else "Inconnu"
                val dur = etDuration.text.toString().toIntOrNull() ?: 0

                if (time.isNotEmpty()) {
                    addNewTask(ScheduleItem(time, type, loc, dur))
                } else {
                    Toast.makeText(this, "Veuillez choisir une heure", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun addNewTask(item: ScheduleItem) {
        val dayName = daysOfWeek[currentDayIndex]
        val newDays = currentWeeklySchedule.days.map { day ->
            if (day.day_of_week == dayName) {
                day.copy(items = day.items + item)
            } else day
        }
        currentWeeklySchedule = currentWeeklySchedule.copy(days = newDays)
        saveSchedule()
    }

    private fun deleteTask(position: Int) {
        val dayName = daysOfWeek[currentDayIndex]
        val newDays = currentWeeklySchedule.days.map { day ->
            if (day.day_of_week == dayName) {
                val newItems = day.items.toMutableList().apply { removeAt(position) }
                day.copy(items = newItems)
            } else day
        }
        currentWeeklySchedule = currentWeeklySchedule.copy(days = newDays)
        saveSchedule()
    }

    private fun saveSchedule() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apiService.saveWeeklySchedule(currentWeeklySchedule)
                withContext(Dispatchers.Main) {
                    updateDayList()
                    Toast.makeText(this@ScheduleActivity, "Programme enregistré", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScheduleActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
