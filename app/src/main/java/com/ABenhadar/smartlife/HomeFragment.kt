package com.ABenhadar.smartlife

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.FrequentPlace
import com.ABenhadar.smartlife.models.ScheduleItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val apiService by lazy { RetrofitClient.getApiService() }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var vStatusIndicator: View
    private lateinit var tvMainRecommendation: TextView
    private lateinit var pbHealthScore: CircularProgressIndicator
    private lateinit var tvHealthScore: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var pbActivityGoal: CircularProgressIndicator
    private lateinit var tvHabitInsight: TextView
    private lateinit var llTodaySchedule: LinearLayout
    private lateinit var cgFrequentPlaces: ChipGroup
    private lateinit var llWeeklyChart: LinearLayout
    private lateinit var cvLastActivity: View
    private lateinit var tvLastActTitle: TextView
    private lateinit var tvLastActDetails: TextView
    private lateinit var ivLastActIcon: ImageView

    private val DAILY_GOAL_MINUTES = 60

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        auth = FirebaseAuth.getInstance()

        // Initialize Views
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvDate = view.findViewById(R.id.tvDate)
        vStatusIndicator = view.findViewById(R.id.vStatusIndicator)
        tvMainRecommendation = view.findViewById(R.id.tvMainRecommendation)
        pbHealthScore = view.findViewById(R.id.pbHealthScore)
        tvHealthScore = view.findViewById(R.id.tvHealthScore)
        tvTotalDuration = view.findViewById(R.id.tvTotalDuration)
        pbActivityGoal = view.findViewById(R.id.pbActivityGoal)
        tvHabitInsight = view.findViewById(R.id.tvHabitInsight)
        llTodaySchedule = view.findViewById(R.id.llTodaySchedule)
        cgFrequentPlaces = view.findViewById(R.id.cgFrequentPlaces)
        llWeeklyChart = view.findViewById(R.id.llWeeklyChart)
        cvLastActivity = view.findViewById(R.id.cvLastActivity)
        tvLastActTitle = view.findViewById(R.id.tvLastActTitle)
        tvLastActDetails = view.findViewById(R.id.tvLastActDetails)
        ivLastActIcon = view.findViewById(R.id.ivLastActIcon)

        setupQuickActions(view)
        setupSwipeRefresh()
        loadDashboardData()

        return view
    }

    private fun setupQuickActions(view: View) {
        view.findViewById<View>(R.id.btnQuickTrack).setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_map
        }
        view.findViewById<View>(R.id.btnQuickPlan).setOnClickListener {
            startActivity(Intent(requireContext(), ScheduleActivity::class.java))
        }
        view.findViewById<View>(R.id.btnQuickCoach).setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_coach
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener { loadDashboardData() }
        swipeRefresh.setColorSchemeResources(R.color.primary_color)
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return
        
        val cal = Calendar.getInstance(Locale.FRANCE)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { cal.add(Calendar.DAY_OF_MONTH, -1) }
        val weekId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        lifecycleScope.launch {
            try {
                val healthJob = async(Dispatchers.IO) { try { apiService.checkHealth() } catch (e: Exception) { null } }
                val userJob = async(Dispatchers.IO) { try { apiService.getUser(userId) } catch (e: Exception) { null } }
                val insightsJob = async(Dispatchers.IO) { try { apiService.getAIInsights(userId) } catch (e: Exception) { null } }
                val activitiesJob = async(Dispatchers.IO) { try { apiService.getActivities(userId) } catch (e: Exception) { emptyList() } }
                val scheduleJob = async(Dispatchers.IO) { try { apiService.getWeeklySchedule(userId, weekId) } catch (e: Exception) { null } }
                val habitsJob = async(Dispatchers.IO) { try { apiService.getHabits(userId) } catch (e: Exception) { null } }

                val isOnline = healthJob.await() != null
                val user = userJob.await()
                val insights = insightsJob.await()
                val activities = activitiesJob.await()
                val schedule = scheduleJob.await()
                val habits = habitsJob.await()

                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    
                    // 1. Connection & Header
                    vStatusIndicator.alpha = if (isOnline) 1.0f else 0.2f
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greetingBase = if (hour in 5..17) "Bonjour" else "Bonsoir"
                    
                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val currentDayFr = SimpleDateFormat("EEEE", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() }
                    val nextTask = schedule?.days?.find { it.day_of_week.equals(currentDayFr, ignoreCase = true) }
                        ?.items?.filter { it.time > currentTime }?.minByOrNull { it.time }

                    tvGreeting.text = if (nextTask != null) 
                        "$greetingBase, ${user?.firstName ?: "Ami"} ! Prêt pour ${nextTask.activity_type} ?"
                    else "$greetingBase, ${user?.firstName ?: "Ami"} !"
                    
                    tvDate.text = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() }

                    // 2. IA Insights & Wellness
                    tvMainRecommendation.text = insights?.recommendations?.firstOrNull() ?: getString(R.string.default_recommendation)
                    val hScore = insights?.healthScore ?: 0
                    pbHealthScore.setProgress(hScore, true)
                    tvHealthScore.text = "$hScore%"
                    tvHabitInsight.text = insights?.habits?.firstOrNull() ?: "Analyse en cours..."

                    // 3. Stats & Chart
                    val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayActs = activities.filter { it.timestamp?.startsWith(todayIso) == true }
                    val totalMin = todayActs.sumOf { it.duration }
                    tvTotalDuration.text = "$totalMin min"
                    pbActivityGoal.setProgress(((totalMin.toFloat() / DAILY_GOAL_MINUTES) * 100).toInt().coerceAtMost(100), true)
                    
                    updateWeeklyChart(activities)

                    // 4. Last Activity
                    val lastAct = activities.maxByOrNull { it.timestamp ?: "" }
                    if (lastAct != null) {
                        cvLastActivity.visibility = View.VISIBLE
                        tvLastActTitle.text = lastAct.type.replaceFirstChar { it.uppercase() }
                        tvLastActDetails.text = "${formatTimeAgo(lastAct.timestamp)} • ${lastAct.duration} min"
                        ivLastActIcon.setImageResource(getActivityIcon(lastAct.type))
                    } else {
                        cvLastActivity.visibility = View.GONE
                    }

                    // 5. Planning & Habits
                    updateScheduleUI(schedule?.days?.find { it.day_of_week.equals(currentDayFr, ignoreCase = true) }?.items ?: emptyList())
                    updateHabitsUI(habits?.frequent_places ?: emptyList())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { swipeRefresh.isRefreshing = false }
            }
        }
    }

    private fun updateWeeklyChart(activities: List<com.ABenhadar.smartlife.models.ActivityData>) {
        llWeeklyChart.removeAllViews()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayInitialFormat = SimpleDateFormat("EE", Locale.FRENCH)

        for (i in 0..6) {
            val dateStr = sdf.format(calendar.time)
            val dayMin = activities.filter { it.timestamp?.startsWith(dateStr) == true }.sumOf { it.duration }
            
            val barContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }

            val bar = View(context).apply {
                val heightPx = (dayMin * 2).coerceAtMost(200) + 10
                layoutParams = LinearLayout.LayoutParams(15, heightPx).apply { bottomMargin = 4 }
                setBackgroundColor(ContextCompat.getColor(requireContext(), 
                    if (dayMin >= DAILY_GOAL_MINUTES) R.color.primary_color else R.color.primary_light))
            }

            val label = TextView(context).apply {
                text = dayInitialFormat.format(calendar.time).first().toString()
                textSize = 10f
                gravity = Gravity.CENTER
            }

            barContainer.addView(bar)
            barContainer.addView(label)
            llWeeklyChart.addView(barContainer)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun updateScheduleUI(items: List<ScheduleItem>) {
        llTodaySchedule.removeAllViews()
        val upcoming = items.sortedBy { it.time }.take(3)
        if (upcoming.isEmpty()) {
            view?.findViewById<TextView>(R.id.tvNoSchedule)?.visibility = View.VISIBLE
        } else {
            view?.findViewById<TextView>(R.id.tvNoSchedule)?.visibility = View.GONE
            upcoming.forEach { item ->
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_dashboard_activity, llTodaySchedule, false)
                itemView.findViewById<TextView>(R.id.tvTime).text = item.time
                itemView.findViewById<TextView>(R.id.tvTitle).text = item.activity_type
                itemView.findViewById<TextView>(R.id.tvSubtitle).text = item.location_name ?: "Lieu à définir"
                itemView.findViewById<TextView>(R.id.tvDuration).text = "${item.duration} min"
                llTodaySchedule.addView(itemView)
            }
        }
    }

    private fun updateHabitsUI(places: List<FrequentPlace>) {
        cgFrequentPlaces.removeAllViews()
        places.take(5).forEach { place ->
            val chip = Chip(context)
            chip.text = place.name
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_light))
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
            cgFrequentPlaces.addView(chip)
        }
    }

    private fun getActivityIcon(type: String): Int {
        return when (type.lowercase()) {
            "walking", "marche" -> android.R.drawable.ic_menu_directions
            "running", "course" -> android.R.drawable.ic_menu_mylocation
            else -> android.R.drawable.ic_menu_today
        }
    }

    private fun formatTimeAgo(timestamp: String?): String {
        if (timestamp == null) return "Récemment"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(timestamp) ?: return "Récemment"
            val diff = Date().time - date.time
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            if (hours < 1) "À l'instant" else "Il y a ${hours}h"
        } catch (e: Exception) { "Récemment" }
    }
}
