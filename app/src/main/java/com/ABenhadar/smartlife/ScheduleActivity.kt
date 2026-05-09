package com.ABenhadar.smartlife

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var rvSchedule: RecyclerView
    private lateinit var adapter: ScheduleAdapter
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private val auth = FirebaseAuth.getInstance()
    private val apiService by lazy { RetrofitClient.getApiService() }

    // Paire de (Clé technique pour DB ex: "Lundi", Titre affiché ex: "LUN.\n12 MAI")
    private var weekDaysData: List<Pair<String, String>> = emptyList()
    private var currentWeeklySchedule = weeklySchedule("", emptyList())
    private var currentDayIndex = 0
    private var namedLocations: List<NamedLocation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        // 1. Initialiser les dates réelles de la semaine actuelle
        weekDaysData = generateWeekDates()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnCalendarPicker).setOnClickListener {
            showDatePicker()
        }

        tabLayout = findViewById(R.id.tabLayoutDays)
        rvSchedule = findViewById(R.id.rvSchedule)
        fabAdd = findViewById(R.id.fabAddTask)

        // --- CORRECTIONS CRASH ---
        // On initialise d'abord l'adapter pour qu'il soit prêt lors de la sélection d'onglet
        setupRecyclerView()
        setupTabs()

        fabAdd.setOnClickListener { showAddTaskDialog() }

        loadSchedule()
        loadNamedLocations()
    }

    private fun generateWeekDates(): List<Pair<String, String>> {
        val calendar = Calendar.getInstance(Locale.FRANCE)
        calendar.firstDayOfWeek = Calendar.MONDAY
        
        // Aligner sur le Lundi de la semaine en cours
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        val displayFormat = SimpleDateFormat("EEE\nd MMM", Locale.getDefault())
        // On utilise le Français pour la DB afin d'être cohérent avec l'ancienne structure
        val technicalFormat = SimpleDateFormat("EEEE", Locale.FRANCE) 
        
        val list = mutableListOf<Pair<String, String>>()
        val todayStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())

        for (i in 0..6) {
            val technicalName = technicalFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
            var displayName = displayFormat.format(calendar.time).uppercase()
            
            if (SimpleDateFormat("dd/MM", Locale.getDefault()).format(calendar.time) == todayStr) {
                displayName = "AUJOURD'HUI\n" + SimpleDateFormat("dd MMM", Locale.getDefault()).format(calendar.time).uppercase()
            }

            list.add(technicalName to displayName)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    private fun setupTabs() {
        tabLayout.removeAllTabs()
        
        // Listener ajouté AVANT les onglets pour capter la sélection par défaut
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentDayIndex = tab?.position ?: 0
                updateDayList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        weekDaysData.forEach { data ->
            val tab = tabLayout.newTab().setText(data.second)
            tabLayout.addTab(tab)
        }

        // Sélection automatique d'aujourd'hui par son nom technique français
        val todayTech = SimpleDateFormat("EEEE", Locale.FRANCE).format(Date()).replaceFirstChar { it.uppercase() }
        val index = weekDaysData.indexOfFirst { it.first == todayTech }
        if (index != -1) {
            tabLayout.getTabAt(index)?.select()
            currentDayIndex = index
        } else {
            updateDayList()
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val selected = Calendar.getInstance()
            selected.set(year, month, day)
            val techName = SimpleDateFormat("EEEE", Locale.FRANCE).format(selected.time).replaceFirstChar { it.uppercase() }
            val index = weekDaysData.indexOfFirst { it.first == techName }
            if (index != -1) {
                tabLayout.getTabAt(index)?.select()
            } else {
                Toast.makeText(this, "Date hors de cette semaine", Toast.LENGTH_SHORT).show()
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupRecyclerView() {
        adapter = ScheduleAdapter { position -> deleteTask(position) }
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
                // Fallback avec les noms techniques en Français
                val defaultDays = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
                currentWeeklySchedule = weeklySchedule(userId, defaultDays.map { DaySchedule(it) })
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
                    Toast.makeText(this@ScheduleActivity, "Lieux non chargés", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDayList() {
        if (!::adapter.isInitialized || weekDaysData.isEmpty()) return
        if (currentDayIndex < 0 || currentDayIndex >= weekDaysData.size) return

        val technicalDayName = weekDaysData[currentDayIndex].first
        // Recherche insensible à la casse pour assurer le mapping
        val daySchedule = currentWeeklySchedule.days.find { it.day_of_week.equals(technicalDayName, ignoreCase = true) }
        adapter.updateItems(daySchedule?.items ?: emptyList())
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etTime = dialogView.findViewById<EditText>(R.id.etTaskTime)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerActivityType)
        val spinnerLocation = dialogView.findViewById<Spinner>(R.id.spinnerTaskLocation)
        val etDuration = dialogView.findViewById<EditText>(R.id.etTaskDuration)

        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        etDuration.setOnClickListener {
            val picker = NumberPicker(this)
            picker.minValue = 5
            picker.maxValue = 180
            picker.value = 30
            AlertDialog.Builder(this)
                .setTitle("Durée (minutes)")
                .setView(picker)
                .setPositiveButton("Valider") { _, _ -> etDuration.setText(picker.value.toString()) }
                .show()
        }

        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Walking", "Running", "Stationary", "Sport", "Repos"))
        val locationNames = if (namedLocations.isEmpty()) listOf("Aucun lieu enregistré") else namedLocations.map { it.name }
        spinnerLocation.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locationNames)

        AlertDialog.Builder(this)
            .setTitle("Planifier pour ${weekDaysData[currentDayIndex].second.replace("\n", " ")}")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val time = etTime.text.toString()
                if (time.isNotEmpty()) {
                    val locName = if (namedLocations.isNotEmpty()) spinnerLocation.selectedItem.toString() else "Inconnu"
                    addNewTask(ScheduleItem(time, spinnerType.selectedItem.toString(), locName, etDuration.text.toString().toIntOrNull() ?: 0))
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun addNewTask(item: ScheduleItem) {
        val dayName = weekDaysData[currentDayIndex].first
        val newDays = currentWeeklySchedule.days.map { 
            if (it.day_of_week.equals(dayName, ignoreCase = true)) it.copy(items = it.items + item) else it 
        }
        currentWeeklySchedule = currentWeeklySchedule.copy(days = newDays)
        saveSchedule()
    }

    private fun deleteTask(position: Int) {
        val dayName = weekDaysData[currentDayIndex].first
        val newDays = currentWeeklySchedule.days.map { 
            if (it.day_of_week.equals(dayName, ignoreCase = true)) {
                val newItems = it.items.toMutableList().apply { removeAt(position) }
                it.copy(items = newItems)
            } else it 
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
                    Toast.makeText(this@ScheduleActivity, "Programme mis à jour", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScheduleActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
