package com.ABenhadar.smartlife

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.ScheduleItem
import com.ABenhadar.smartlife.models.weeklySchedule
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
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
    private lateinit var llWeeklyChart: LinearLayout
    private lateinit var cvLastActivity: View
    private lateinit var tvLastActTitle: TextView
    private lateinit var tvLastActDetails: TextView
    private lateinit var ivLastActIcon: ImageView
    private lateinit var ivUserAvatar: ShapeableImageView

    private val DAILY_GOAL_MINUTES = 60

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        auth = FirebaseAuth.getInstance()

        // Initialisation des vues
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
        llWeeklyChart = view.findViewById(R.id.llWeeklyChart)
        cvLastActivity = view.findViewById(R.id.cvLastActivity)
        tvLastActTitle = view.findViewById(R.id.tvLastActTitle)
        tvLastActDetails = view.findViewById(R.id.tvLastActDetails)
        ivLastActIcon = view.findViewById(R.id.ivLastActIcon)
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar)

        setupQuickActions(view)
        setupSwipeRefresh()

        ivUserAvatar.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        cvLastActivity.setOnClickListener {
            // On peut rediriger vers le planning complet
            startActivity(Intent(requireContext(), ScheduleActivity::class.java))
        }

        swipeRefresh.post {
            swipeRefresh.isRefreshing = true
            loadDashboardData()
        }

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

        // Calcul de l'ID de la semaine (Lundi actuel)
        val cal = Calendar.getInstance(Locale.FRANCE)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { cal.add(Calendar.DAY_OF_MONTH, -1) }
        val weekId = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        lifecycleScope.launch {
            try {
                val healthJob = async(Dispatchers.IO) { try { apiService.checkHealth() } catch (e: Exception) { null } }
                val userJob = async(Dispatchers.IO) { try { apiService.getUser(userId) } catch (e: Exception) { null } }
                val insightsJob = async(Dispatchers.IO) { try { apiService.getAIInsights(userId) } catch (e: Exception) { null } }
                val scheduleJob = async(Dispatchers.IO) { try { apiService.getWeeklySchedule(userId, weekId) } catch (e: Exception) { null } }

                val isOnline = healthJob.await() != null
                val user = userJob.await()
                val insights = insightsJob.await()
                val schedule = scheduleJob.await()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    swipeRefresh.isRefreshing = false

                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                    // 1. Header & Greeting
                    vStatusIndicator.alpha = if (isOnline) 1.0f else 0.2f
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greetingBase = if (hour in 5..17) "Bonjour" else "Bonsoir"

                    val todaySchedule = schedule?.days?.find { it.date == todayIso }
                    val todayItems = todaySchedule?.items ?: emptyList()
                    val nextTask = todayItems.filter { it.time > currentTime }.minByOrNull { it.time }

                    tvGreeting.text = if (nextTask != null)
                        "$greetingBase, ${user?.firstName ?: "Ami"} ! Prêt pour ${nextTask.activity_type} ?"
                    else "$greetingBase, ${user?.firstName ?: "Ami"} !"

                    tvDate.text = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() }

                    // 2. Avatar logic
                    val profileImageUrl = user?.profileImageUrl
                    if (!profileImageUrl.isNullOrEmpty()) {
                        val fullUrl = if (profileImageUrl.startsWith("http")) profileImageUrl else RetrofitClient.BASE_URL + profileImageUrl
                        Glide.with(this@HomeFragment).load(fullUrl).placeholder(R.mipmap.ic_logo).error(R.mipmap.ic_logo).circleCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(ivUserAvatar)
                    } else { ivUserAvatar.setImageResource(R.mipmap.ic_logo) }

                    // 3. IA Insights
                    tvMainRecommendation.text = insights?.recommendations?.firstOrNull() ?: getString(R.string.default_recommendation)
                    val hScore = insights?.healthScore ?: 0
                    pbHealthScore.setProgress(hScore, true)
                    tvHealthScore.text = "$hScore%"
                    tvHabitInsight.text = insights?.habits?.firstOrNull() ?: "Analyse de votre planning..."

                    // 4. Statistiques du jour (Basées sur le planning passé aujourd'hui)
                    val completedMin = todayItems.filter { it.time <= currentTime }.sumOf { it.duration.toDouble() }
                    tvTotalDuration.text = "${completedMin.toInt()} min"
                    val progress = ((completedMin.toFloat() / DAILY_GOAL_MINUTES) * 100).toInt().coerceAtMost(100)
                    pbActivityGoal.setProgress(progress, true)
                    pbActivityGoal.setIndicatorColor(ContextCompat.getColor(requireContext(), if (completedMin >= DAILY_GOAL_MINUTES) R.color.success_main else R.color.tertiary_color))

                    updateWeeklyChartFromSchedule(schedule)

                    // 5. Logique de la "Dernière Session" (Récupérée du planning)
                    val lastSession = schedule?.days?.flatMap { day ->
                        day.items.map { day.date to it }
                    }?.filter { (date, item) ->
                        date != null && "$date ${item.time}" <= "$todayIso $currentTime"
                    }?.maxByOrNull { (date, item) -> "$date ${item.time}" }

                    if (lastSession != null) {
                        val (date, item) = lastSession
                        cvLastActivity.visibility = View.VISIBLE
                        tvLastActTitle.text = item.activity_type.replaceFirstChar { it.uppercase() }
                        val timeLabel = if (date == todayIso) "Aujourd'hui à ${item.time}" else "$date à ${item.time}"
                        tvLastActDetails.text = "$timeLabel • ${item.duration} min • ${item.location_name ?: "Lieu prévu"}"
                        ivLastActIcon.setImageResource(getActivityIcon(item.activity_type))
                    } else {
                        cvLastActivity.visibility = View.GONE
                    }

                    // 6. Agenda (Prochaines tâches de la journée)
                    val upcomingToday = todayItems.filter { it.time > currentTime }
                    updateScheduleUI(upcomingToday)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Log.e("HomeFragment", "Load error", e)
                }
            }
        }
    }

    private fun updateWeeklyChartFromSchedule(schedule: weeklySchedule?) {
        llWeeklyChart.removeAllViews()
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val days = schedule?.days ?: emptyList()
        if (days.isEmpty()) return

        val maxMinutes = days.maxOfOrNull { d -> d.items.sumOf { it.duration.toDouble() } }?.coerceAtLeast(DAILY_GOAL_MINUTES.toDouble()) ?: DAILY_GOAL_MINUTES.toDouble()

        for (day in days) {
            val dayTotal = day.items.sumOf { it.duration.toDouble() }
            val barContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            val barHeightDp = ((dayTotal / maxMinutes) * 85.0).toFloat().coerceAtLeast(10f)
            val bar = View(context).apply {
                val widthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics).toInt()
                val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, barHeightDp, resources.displayMetrics).toInt()
                layoutParams = LinearLayout.LayoutParams(widthPx, heightPx).apply { bottomMargin = 15 }
                background = GradientDrawable().apply {
                    cornerRadius = 20f
                    setColor(ContextCompat.getColor(requireContext(), if (dayTotal >= DAILY_GOAL_MINUTES) R.color.primary_color else R.color.primary_light))
                }
            }
            val label = TextView(context).apply {
                text = day.day_of_week.first().toString().uppercase()
                textSize = 10f
                gravity = Gravity.CENTER
                alpha = if (day.date == todayIso) 1f else 0.5f
                if (day.date == todayIso) setTypeface(null, Typeface.BOLD)
            }
            barContainer.addView(bar); barContainer.addView(label); llWeeklyChart.addView(barContainer)
        }
    }

    private fun updateScheduleUI(items: List<ScheduleItem>) {
        llTodaySchedule.removeAllViews()
        val upcoming = items.sortedBy { it.time }.take(3)
        if (upcoming.isEmpty()) {
            llTodaySchedule.addView(TextView(context).apply { text = getString(R.string.free_day_msg); gravity = Gravity.CENTER; setPadding(0, 40, 0, 40); alpha = 0.5f })
        } else {
            upcoming.forEach { item ->
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_dashboard_activity, llTodaySchedule, false)
                itemView.findViewById<TextView>(R.id.tvTime).text = item.time
                itemView.findViewById<TextView>(R.id.tvTitle).text = item.activity_type
                itemView.findViewById<TextView>(R.id.tvSubtitle).text = item.location_name ?: "Lieu à venir"
                itemView.findViewById<TextView>(R.id.tvDuration).text = "${item.duration} min"
                llTodaySchedule.addView(itemView)
            }
        }
    }

    private fun getActivityIcon(type: String): Int {
        val t = type.lowercase()
        return when {
            t.contains("walk") || t.contains("marche") -> R.drawable.ic_round_directions_run_24
            t.contains("run") || t.contains("course") -> R.drawable.ic_round_my_location_24
            else -> R.drawable.ic_round_history_24
        }
    }
}