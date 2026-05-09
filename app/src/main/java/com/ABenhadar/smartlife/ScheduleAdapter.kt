package com.ABenhadar.smartlife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.models.ScheduleItem

class ScheduleAdapter(private val onDeleteClick: (Int) -> Unit) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private var items: MutableList<ScheduleItem> = mutableListOf()

    fun updateItems(newItems: List<ScheduleItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTime.text = item.time
        holder.tvActivity.text = item.activity_type
        holder.tvLocation.text = item.location_name ?: "Lieu non défini"
        holder.tvDuration.text = "${item.duration} min"
        
        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvScheduleTime)
        val tvActivity: TextView = view.findViewById(R.id.tvScheduleActivity)
        val tvLocation: TextView = view.findViewById(R.id.tvScheduleLocation)
        val tvDuration: TextView = view.findViewById(R.id.tvScheduleDuration)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTask)
    }
}