package com.ABenhadar.smartlife

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.api.RetrofitClient
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityListFragment : Fragment() {

    private lateinit var rvActivities: RecyclerView
    private lateinit var adapter: ActivitiesAdapter
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private val auth = FirebaseAuth.getInstance()
    private val apiService by lazy { RetrofitClient.getApiService() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_list, container, false)

        rvActivities = view.findViewById(R.id.rvActivities)
        fabAdd = view.findViewById(R.id.fabAddActivity)

        setupRecyclerView()

        fabAdd.setOnClickListener {
            // Logique pour ajouter/planifier une activité (peut-être un dialogue ou une nouvelle activité)
            Toast.makeText(requireContext(), "Fonctionnalité de planification bientôt disponible !", Toast.LENGTH_SHORT).show()
        }

        loadActivities()

        return view
    }

    private fun setupRecyclerView() {
        adapter = ActivitiesAdapter()
        rvActivities.layoutManager = LinearLayoutManager(requireContext())
        rvActivities.adapter = adapter
    }

    private fun loadActivities() {
        val userId = auth.currentUser?.uid ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val activities = apiService.getActivities(userId)
                withContext(Dispatchers.Main) {
                    if (activities.isNotEmpty()) {
                        // On inverse pour avoir les plus récentes en haut
                        adapter.updateActivities(activities.reversed())
                    } else {
                        Log.d("ActivityList", "No activities found")
                    }
                }
            } catch (e: Exception) {
                Log.e("ActivityList", "Error loading activities", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur lors du chargement des activités", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
