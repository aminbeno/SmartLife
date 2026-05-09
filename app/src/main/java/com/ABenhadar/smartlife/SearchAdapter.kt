package com.ABenhadar.smartlife

import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(private val onResultClick: (Address) -> Unit) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    private var results: List<Address> = emptyList()

    fun updateResults(newResults: List<Address>) {
        results = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val address = results[position]
        val name = address.getAddressLine(0) ?: "Lieu inconnu"
        holder.tvName.text = name
        holder.itemView.setOnClickListener { onResultClick(address) }
    }

    override fun getItemCount(): Int = results.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvResultName)
    }
}