package com.ABenhadar.smartlife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.models.ActivityData
import java.text.SimpleDateFormat
import java.util.*

class ActivitiesAdapter : RecyclerView.Adapter<ActivitiesAdapter.ViewHolder>() {

    private var activities: List<ActivityData> = emptyList()

    fun updateActivities(newActivities: List<ActivityData>) {
        activities = newActivities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        
        holder.tvType.text = activity.type.replaceFirstChar { it.uppercase() }
        holder.tvDuration.text = "${activity.duration} min"
        
        // Format timestamp
        activity.timestamp?.let {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = inputFormat.parse(it)
                holder.tvTime.text = date?.let { d -> outputFormat.format(d) } ?: "--:--"
            } catch (e: Exception) {
                holder.tvTime.text = it.takeLast(5)
            }
        }

        // Icon based on type
        when (activity.type.lowercase()) {
            "walking" -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_directions)
            "running" -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_compass)
            "stationary" -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
            else -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_info_details)
        }
        
        // Location placeholder (can be improved by looking up nearest named location)
        holder.tvLocation.text = "Position: ${String.format("%.3f", activity.location.lat)}, ${String.format("%.3f", activity.location.lng)}"
    }

    override fun getItemCount(): Int = activities.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivActivityIcon)
        val tvType: TextView = view.findViewById(R.id.tvActivityType)
        val tvLocation: TextView = view.findViewById(R.id.tvActivityLocation)
        val tvDuration: TextView = view.findViewById(R.id.tvActivityDuration)
        val tvTime: TextView = view.findViewById(R.id.tvActivityTime)
    }
}