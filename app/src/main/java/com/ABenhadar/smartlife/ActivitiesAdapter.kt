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
                // Pour les activités réelles (Format ISO)
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = inputFormat.parse(it)
                holder.tvTime.text = date?.let { d -> outputFormat.format(d) } ?: it
            } catch (e: Exception) {
                // Pour les activités planifiées (Format texte personnalisé)
                holder.tvTime.text = it
            }
        }

        // Icon based on type
        val typeLower = activity.type.lowercase()
        when {
            typeLower.contains("walking") || typeLower.contains("marche") -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_directions)
            typeLower.contains("running") || typeLower.contains("course") -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_compass)
            typeLower.contains("stationary") || typeLower.contains("repos") -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
            else -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_info_details)
        }
        
        // Affichage du lieu : Priorité au nom du lieu s'il existe (NamedLocation ou Planifié)
        if (!activity.locationName.isNullOrEmpty()) {
            holder.tvLocation.text = activity.locationName
        } else {
            // Sinon on affiche les coordonnées GPS
            holder.tvLocation.text = String.format(Locale.getDefault(), "Position: %.3f, %.3f", activity.location.lat, activity.location.lng)
        }
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