package com.ABenhadar.smartlife

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.FrequentPlace
import com.ABenhadar.smartlife.models.ScheduleItem
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val apiService by lazy { RetrofitClient.getApiService() }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvMainRecommendation: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var pbActivityGoal: CircularProgressIndicator
    private lateinit var tvGoalPercent: TextView
    private lateinit var llTodaySchedule: LinearLayout
    private lateinit var tvNoSchedule: TextView
    private lateinit var llFrequentPlaces: LinearLayout
    private lateinit var tvNoHabits: TextView

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
        tvMainRecommendation = view.findViewById(R.id.tvMainRecommendation)
        tvTotalDuration = view.findViewById(R.id.tvTotalDuration)
        pbActivityGoal = view.findViewById(R.id.pbActivityGoal)
        tvGoalPercent = view.findViewById(R.id.tvGoalPercent)
        llTodaySchedule = view.findViewById(R.id.llTodaySchedule)
        tvNoSchedule = view.findViewById(R.id.tvNoSchedule)
        llFrequentPlaces = view.findViewById(R.id.llFrequentPlaces)
        tvNoHabits = view.findViewById(R.id.tvNoHabits)

        setupSwipeRefresh()
        setupHeader()
        loadDashboardData()

        return view
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
        swipeRefresh.setColorSchemeResources(R.color.black) 
    }

    private fun setupHeader() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingBase = when (hour) {
            in 5..17 -> "Bonjour"
            in 18..22 -> "Bonsoir"
            else -> "Bonne nuit"
        }
        
        val userEmail = auth.currentUser?.email ?: "Ami"
        val name = userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        tvGreeting.text = "$greetingBase, $name !"
        
        val sdf = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)
        tvDate.text = sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                // Chargement parallèle pour plus de fluidité
                val insightsJob = async(Dispatchers.IO) { try { apiService.getAIInsights(userId) } catch (e: Exception) { null } }
                val activitiesJob = async(Dispatchers.IO) { try { apiService.getActivities(userId) } catch (e: Exception) { null } }
                val scheduleJob = async(Dispatchers.IO) { try { apiService.getWeeklySchedule(userId) } catch (e: Exception) { null } }
                val habitsJob = async(Dispatchers.IO) { try { apiService.getHabits(userId) } catch (e: Exception) { null } }

                val insights = insightsJob.await()
                val activities = activitiesJob.await()
                val schedule = scheduleJob.await()
                val habits = habitsJob.await()

                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    
                    // 1. Mise à jour de la recommandation IA
                    tvMainRecommendation.text = insights?.recommendations?.firstOrNull() 
                        ?: getString(R.string.default_recommendation)

                    // 2. Calcul et affichage de la progression (Objectif)
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayActivities = activities?.filter { it.timestamp?.startsWith(today) == true } ?: emptyList()
                    val totalMin = todayActivities.sumOf { it.duration }
                    
                    tvTotalDuration.text = "$totalMin min"
                    val progress = ((totalMin.toFloat() / DAILY_GOAL_MINUTES) * 100).toInt()
                    pbActivityGoal.setProgress(progress.coerceAtMost(100), true)
                    tvGoalPercent.text = "$progress%"

                    // 3. Mise à jour du planning du jour
                    val sdf = SimpleDateFormat("EEEE", Locale.FRENCH)
                    val currentDayFr = sdf.format(Date()).replaceFirstChar { it.uppercase() }
                    val todaySchedule = schedule?.days?.find { 
                        it.day_of_week.equals(currentDayFr, ignoreCase = true) || 
                        translateDay(it.day_of_week).equals(currentDayFr, ignoreCase = true)
                    }
                    updateScheduleUI(todaySchedule?.items ?: emptyList())

                    // 4. Mise à jour des lieux favoris
                    updateHabitsUI(habits?.frequent_places ?: emptyList())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { swipeRefresh.isRefreshing = false }
            }
        }
    }

    private fun updateScheduleUI(items: List<ScheduleItem>) {
        llTodaySchedule.removeAllViews()
        if (items.isEmpty()) {
            tvNoSchedule.visibility = View.VISIBLE
            llTodaySchedule.addView(tvNoSchedule)
        } else {
            tvNoSchedule.visibility = View.GONE
            items.sortedBy { it.time }.forEach { item ->
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_dashboard_activity, llTodaySchedule, false)
                itemView.findViewById<TextView>(R.id.tvTime).text = item.time
                itemView.findViewById<TextView>(R.id.tvTitle).text = item.activity_type
                itemView.findViewById<TextView>(R.id.tvSubtitle).text = item.location_name ?: "Lieu non spécifié"
                itemView.findViewById<TextView>(R.id.tvDuration).text = "${item.duration} min"
                llTodaySchedule.addView(itemView)
            }
        }
    }

    private fun updateHabitsUI(places: List<FrequentPlace>) {
        llFrequentPlaces.removeAllViews()
        if (places.isEmpty()) {
            tvNoHabits.visibility = View.VISIBLE
            llFrequentPlaces.addView(tvNoHabits)
        } else {
            tvNoHabits.visibility = View.GONE
            places.take(5).forEach { place ->
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_frequent_place, llFrequentPlaces, false)
                itemView.findViewById<TextView>(R.id.tvPlaceName).text = place.name
                itemView.findViewById<TextView>(R.id.tvVisitCount).text = "${place.visits} visites"
                llFrequentPlaces.addView(itemView)
            }
        }
    }

    private fun translateDay(day: String): String {
        return when (day.lowercase()) {
            "monday" -> "Lundi"
            "tuesday" -> "Mardi"
            "wednesday" -> "Mercredi"
            "thursday" -> "Jeudi"
            "friday" -> "Vendredi"
            "saturday" -> "Samedi"
            "sunday" -> "Dimanche"
            else -> day
        }
    }
}
