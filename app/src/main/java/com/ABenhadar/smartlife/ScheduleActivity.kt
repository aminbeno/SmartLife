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

    private var weekDaysData: List<Triple<String, String, String>> = emptyList()
    // Correction ici : On initialise avec les 3 paramètres (userId, weekId, days)
    private var currentWeeklySchedule = weeklySchedule("", "", emptyList()) 
    private var currentDayIndex = 0
    private var namedLocations: List<NamedLocation> = emptyList()
    
    private var currentViewDate = Calendar.getInstance(Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        currentViewDate.firstDayOfWeek = Calendar.MONDAY
        alignToStartOfWeek(currentViewDate)
        refreshWeekData()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnCalendarPicker).setOnClickListener { showDatePicker() }

        tabLayout = findViewById(R.id.tabLayoutDays)
        rvSchedule = findViewById(R.id.rvSchedule)
        fabAdd = findViewById(R.id.fabAddTask)

        setupRecyclerView()
        setupTabs()

        fabAdd.setOnClickListener { showAddTaskDialog() }

        loadSchedule()
        loadNamedLocations()
    }

    private fun alignToStartOfWeek(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
    }

    private fun refreshWeekData() {
        val calendar = currentViewDate.clone() as Calendar
        val displayFormat = SimpleDateFormat("EEE\nd MMM", Locale.getDefault())
        val technicalFormat = SimpleDateFormat("EEEE", Locale.FRANCE) 
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val list = mutableListOf<Triple<String, String, String>>()
        val todayStr = isoFormat.format(Date())

        for (i in 0..6) {
            val technicalName = technicalFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
            val isoDate = isoFormat.format(calendar.time)
            var displayName = displayFormat.format(calendar.time).uppercase()
            
            if (isoDate == todayStr) {
                displayName = "AUJOURD'HUI\n" + SimpleDateFormat("d MMM", Locale.getDefault()).format(calendar.time).uppercase()
            }

            list.add(Triple(technicalName, displayName, isoDate))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        weekDaysData = list
    }

    private fun setupTabs() {
        tabLayout.removeAllTabs()
        tabLayout.clearOnTabSelectedListeners()
        
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

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val index = weekDaysData.indexOfFirst { it.third == todayIso }
        if (index != -1) {
            tabLayout.getTabAt(index)?.select()
            currentDayIndex = index
        } else {
            tabLayout.getTabAt(0)?.select()
            currentDayIndex = 0
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            currentViewDate.set(year, month, day)
            alignToStartOfWeek(currentViewDate)
            refreshWeekData()
            setupTabs()
            loadSchedule()
        }, currentViewDate.get(Calendar.YEAR), currentViewDate.get(Calendar.MONTH), currentViewDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupRecyclerView() {
        adapter = ScheduleAdapter { position -> deleteTask(position) }
        rvSchedule.layoutManager = LinearLayoutManager(this)
        rvSchedule.adapter = adapter
    }

    private fun loadSchedule() {
        val userId = auth.currentUser?.uid ?: return
        val weekId = weekDaysData[0].third // Date du Lundi

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val schedule = apiService.getWeeklySchedule(userId, weekId)
                withContext(Dispatchers.Main) {
                    currentWeeklySchedule = schedule
                    syncScheduleWithCurrentWeek()
                    updateDayList()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Correction ici : Passage des 3 paramètres
                    currentWeeklySchedule = weeklySchedule(userId, weekId, weekDaysData.map { DaySchedule(it.first, it.third) })
                    updateDayList()
                    Toast.makeText(this@ScheduleActivity, "Semaine initialisée localement", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun syncScheduleWithCurrentWeek() {
        val syncedDays = weekDaysData.map { (techName, _, isoDate) ->
            val existingDay = currentWeeklySchedule.days.find { it.day_of_week.equals(techName, ignoreCase = true) }
            existingDay?.copy(date = isoDate) ?: DaySchedule(techName, isoDate)
        }
        currentWeeklySchedule = currentWeeklySchedule.copy(days = syncedDays)
    }

    private fun loadNamedLocations() {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val locations = apiService.getNamedLocations(userId)
                withContext(Dispatchers.Main) { namedLocations = locations }
            } catch (e: Exception) { }
        }
    }

    private fun updateDayList() {
        if (!::adapter.isInitialized || weekDaysData.isEmpty()) return
        val technicalDayName = weekDaysData[currentDayIndex].first
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
                .setPositiveButton("OK") { _, _ -> etDuration.setText(picker.value.toString()) }
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
                val durStr = etDuration.text.toString()
                if (time.isNotEmpty() && durStr.isNotEmpty()) {
                    val selectedPos = spinnerLocation.selectedItemPosition
                    val selectedLoc = if (namedLocations.isNotEmpty() && selectedPos != -1) namedLocations[selectedPos] else null
                    val item = ScheduleItem(time, spinnerType.selectedItem.toString(), selectedLoc?.name ?: "Inconnu", selectedLoc?.lat, selectedLoc?.lng, durStr.toInt())
                    addNewTask(item)
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
                    Toast.makeText(this@ScheduleActivity, "Enregistré avec succès", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScheduleActivity, "Erreur de connexion au backend", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
