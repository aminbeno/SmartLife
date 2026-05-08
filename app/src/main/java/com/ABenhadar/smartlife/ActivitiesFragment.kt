package com.ABenhadar.smartlife

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.speech.RecognitionListener
import android.os.Bundle as SpeechBundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.VoiceLogData
import com.ABenhadar.smartlife.service.GpsTrackingService
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.util.*

class ActivitiesFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val apiService by lazy { RetrofitClient.getApiService() }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionIntent: Intent
    
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var searchView: SearchView
    private var searchMarker: Marker? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationOverlay.enableMyLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activities, container, false)
        auth = FirebaseAuth.getInstance()

        map = view.findViewById(R.id.map)
        setupMap()

        val fabVoice = view.findViewById<FloatingActionButton>(R.id.fabVoice)
        setupVoice(fabVoice)

        btnStart = view.findViewById(R.id.btnStartTracking)
        btnStop = view.findViewById(R.id.btnStopTracking)
        searchView = view.findViewById(R.id.searchView)
        
        setupTrackingButtons()
        setupSearchBar()

        view.findViewById<ImageButton>(R.id.btnCloseRec).setOnClickListener {
            view.findViewById<View>(R.id.cvRecommendation).visibility = View.GONE
        }

        lifecycleScope.launch {
            loadActivityHistory()
            loadHabits()
        }

        return view
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(15.0)

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        val startPoint = GeoPoint(48.8583, 2.2944)
        mapController.setCenter(startPoint)
    }

    private fun setupSearchBar() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    performSearch(query)
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun performSearch(locationName: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addressList: List<Address>? = geocoder.getFromLocationName(locationName, 1)
                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    val geoPoint = GeoPoint(address.latitude, address.longitude)

                    withContext(Dispatchers.Main) {
                        // Remove previous search marker
                        searchMarker?.let { map.overlays.remove(it) }
                        
                        // Add new marker
                        searchMarker = Marker(map).apply {
                            position = geoPoint
                            title = address.getAddressLine(0) ?: locationName
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        map.overlays.add(searchMarker)
                        
                        // Center map and zoom
                        map.controller.animateTo(geoPoint)
                        map.controller.setZoom(17.0)
                        map.invalidate()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Lieu non trouvé", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur de recherche", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTrackingButtons() {
        updateTrackingButtonsState()

        btnStart.setOnClickListener {
            startTracking()
        }

        btnStop.setOnClickListener {
            stopTracking()
        }
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(requireContext(), GpsTrackingService::class.java)
            ContextCompat.startForegroundService(requireContext(), intent)
            updateTrackingButtonsState()
            Toast.makeText(requireContext(), "Tracking started", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun stopTracking() {
        val intent = Intent(requireContext(), GpsTrackingService::class.java)
        requireContext().stopService(intent)
        updateTrackingButtonsState()
        Toast.makeText(requireContext(), "Tracking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateTrackingButtonsState() {
        val isRunning = isServiceRunning(GpsTrackingService::class.java)
        if (isRunning) {
            btnStart.visibility = View.GONE
            btnStop.visibility = View.VISIBLE
        } else {
            btnStart.visibility = View.VISIBLE
            btnStop.visibility = View.GONE
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private suspend fun loadHabits() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val habits = apiService.getHabits(userId)
            withContext(Dispatchers.Main) {
                habits.frequent_places.forEach { place ->
                    val marker = Marker(map)
                    marker.position = GeoPoint(place.lat, place.lng)
                    marker.title = place.name
                    marker.subDescription = "${place.visits} visits"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
        } catch (e: Exception) {}
    }

    private suspend fun loadActivityHistory() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val activities = apiService.getActivities(userId)
            if (activities.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val line = Polyline()
                    line.outlinePaint.color = Color.BLUE
                    line.outlinePaint.strokeWidth = 5f

                    val points = activities.map { GeoPoint(it.location.lat, it.location.lng) }
                    line.setPoints(points)
                    map.overlays.add(line)

                    if (points.isNotEmpty()) {
                        map.controller.animateTo(points.last())
                    }
                    map.invalidate()
                }
            }
        } catch (e: Exception) {}
    }

    private fun setupVoice(fabVoice: FloatingActionButton) {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: SpeechBundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    Log.e("ActivitiesFragment", "Voice Error: $error")
                }
                override fun onResults(results: SpeechBundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) processVoiceInput(matches[0])
                }
                override fun onPartialResults(partialResults: SpeechBundle?) {}
                override fun onEvent(eventType: Int, params: SpeechBundle?) {}
            })

            fabVoice.setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    speechRecognizer.startListening(recognitionIntent)
                } else {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            }
        } catch (e: Exception) {
            Log.e("ActivitiesFragment", "Voice setup failed", e)
        }
    }

    private fun processVoiceInput(input: String) {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                apiService.addVoiceLog(VoiceLogData(userId, input, "Processed: $input"))
                if (input.lowercase().contains("conseil") || input.lowercase().contains("recom")) {
                    showRecommendation("AI Advice: You've been stationary for 2 hours. A short walk is recommended!")
                } else {
                    Toast.makeText(requireContext(), "You said: $input", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {}
        }
    }

    private fun showRecommendation(message: String) {
        val cvRecommendation = view?.findViewById<View>(R.id.cvRecommendation)
        val tvRecommendation = view?.findViewById<TextView>(R.id.tvRecommendation)
        tvRecommendation?.text = message
        cvRecommendation?.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        updateTrackingButtonsState()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}