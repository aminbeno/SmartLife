package com.ABenhadar.smartlife

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.models.ActivityData
import java.text.SimpleDateFormat
import java.util.*

class ActivitiesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ActivityListItem> = emptyList()

    sealed class ActivityListItem {
        data class Header(val dateTitle: String, val sortDate: String) : ActivityListItem()
        data class Activity(val data: ActivityData) : ActivityListItem()
    }

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    fun updateActivities(newActivities: List<ActivityData>) {
        val groupedList = mutableListOf<ActivityListItem>()
        
        // 1. Groupement par date (YYYY-MM-DD)
        val groupedMap = newActivities.groupBy { activity ->
            val ts = activity.timestamp ?: ""
            if (ts.contains("T")) {
                ts.split("T")[0]
            } else if (ts.length >= 10 && ts.contains("-")) {
                ts.substring(0, 10)
            } else {
                "Autre"
            }
        }

        // 2. Tri des dates décroissant
        val sortedDateKeys = groupedMap.keys.sortedDescending()

        sortedDateKeys.forEach { dateKey ->
            val headerTitle = formatHeaderDate(dateKey)
            groupedList.add(ActivityListItem.Header(headerTitle, dateKey))
            
            // 3. Tri des activités de la journée par heure décroissante
            val dayActivities = groupedMap[dateKey]?.sortedByDescending { it.timestamp }
            dayActivities?.forEach { 
                groupedList.add(ActivityListItem.Activity(it))
            }
        }

        items = groupedList
        notifyDataSetChanged()
    }

    private fun formatHeaderDate(dateStr: String): String {
        if (dateStr == "Autre") return "Divers"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (dateStr == today) {
                "Aujourd'hui"
            } else {
                date?.let { outputFormat.format(it).replaceFirstChar { c -> c.uppercase() } } ?: dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ActivityListItem.Header -> VIEW_TYPE_HEADER
            is ActivityListItem.Activity -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
            ActivityViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ActivityListItem.Header -> (holder as HeaderViewHolder).bind(item.dateTitle)
            is ActivityListItem.Activity -> (holder as ActivityViewHolder).bind(item.data)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvDateHeader)
        fun bind(title: String) {
            tvDate.text = title
        }
    }

    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivActivityIcon)
        private val tvType: TextView = view.findViewById(R.id.tvActivityType)
        private val tvLocation: TextView = view.findViewById(R.id.tvActivityLocation)
        private val tvDuration: TextView = view.findViewById(R.id.tvActivityDuration)
        private val tvTime: TextView = view.findViewById(R.id.tvActivityTime)

        fun bind(activity: ActivityData) {
            val isPlanned = activity.type.startsWith("[Planifié]")
            
            // Nettoyage du titre
            val cleanType = activity.type.replace("[Planifié] ", "").replaceFirstChar { it.uppercase() }
            tvType.text = cleanType
            
            if (isPlanned) {
                tvType.setTextColor(Color.parseColor("#FF9800")) // Orange pour planifié
            } else {
                tvType.setTextColor(Color.parseColor("#212121"))
            }

            tvDuration.text = "${activity.duration} min"
            
            // Formatage de l'heure
            val ts = activity.timestamp ?: ""
            try {
                if (ts.contains("T")) {
                    val timePart = ts.split("T")[1]
                    tvTime.text = if (timePart.length >= 5) timePart.substring(0, 5) else timePart
                } else if (ts.contains(" à ")) {
                    tvTime.text = ts.substringAfter(" à ")
                } else {
                    tvTime.text = ts
                }
            } catch (e: Exception) {
                tvTime.text = "--:--"
            }

            // Icônes
            val typeLower = activity.type.lowercase()
            when {
                typeLower.contains("walk") || typeLower.contains("marche") -> ivIcon.setImageResource(android.R.drawable.ic_menu_directions)
                typeLower.contains("run") || typeLower.contains("course") -> ivIcon.setImageResource(android.R.drawable.ic_menu_compass)
                typeLower.contains("stat") || typeLower.contains("repos") -> ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
                else -> ivIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            }
            
            // Localisation
            if (!activity.locationName.isNullOrEmpty()) {
                tvLocation.text = activity.locationName
            } else {
                tvLocation.text = String.format(Locale.getDefault(), "Position: %.2f, %.2f", activity.location.lat, activity.location.lng)
            }
        }
    }
}
