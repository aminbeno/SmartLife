package com.ABenhadar.smartlife

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.NamedLocation as SmartLifeNamedLocation
import com.ABenhadar.smartlife.service.GpsTrackingService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class ActivitiesFragment : Fragment(), MapEventsReceiver {

    private lateinit var auth: FirebaseAuth
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val apiService by lazy { RetrofitClient.getApiService() }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionIntent: Intent
    
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var searchView: SearchView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var cvSearchResults: MaterialCardView
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var fabRecenter: FloatingActionButton
    
    private var searchMarker: Marker? = null
    private var longPressMarker: Marker? = null
    private var searchJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activities, container, false)
        auth = FirebaseAuth.getInstance()

        map = view.findViewById(R.id.map)
        fabRecenter = view.findViewById(R.id.fabRecenter)
        setupMap()

        val fabVoice = view.findViewById<FloatingActionButton>(R.id.fabVoice)
        setupVoice(fabVoice)

        btnStart = view.findViewById(R.id.btnStartTracking)
        btnStop = view.findViewById(R.id.btnStopTracking)
        searchView = view.findViewById(R.id.searchView)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        cvSearchResults = view.findViewById(R.id.cvSearchResults)
        
        setupTrackingButtons()
        setupSearchBar()

        view.findViewById<ImageButton>(R.id.btnCloseRec)?.setOnClickListener {
            view.findViewById<View>(R.id.cvRecommendation).visibility = View.GONE
        }

        fabRecenter.setOnClickListener {
            locationOverlay.enableFollowLocation()
            map.controller.animateTo(locationOverlay.myLocation)
            map.controller.setZoom(17.0)
        }

        lifecycleScope.launch {
            loadActivityHistory()
            loadHabits()
            loadNamedLocations()
        }

        return view
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
        
        // --- NOUVEAUTÉ : Affichage direct de la position ---
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation() // La carte suit l'utilisateur dès le début
        locationOverlay.runOnFirstFix {
            lifecycleScope.launch(Dispatchers.Main) {
                map.controller.animateTo(locationOverlay.myLocation)
                map.controller.setZoom(17.5)
            }
        }
        
        map.overlays.add(locationOverlay)

        val mapEventsOverlay = MapEventsOverlay(this)
        map.overlays.add(0, mapEventsOverlay)

        // Point de départ par défaut en attendant le Fix GPS
        val startPoint = GeoPoint(48.8583, 2.2944)
        map.controller.setCenter(startPoint)
    }
    
    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        cvSearchResults.visibility = View.GONE
        p?.let { point ->
            val clickedMarker = map.overlays.filterIsInstance<Marker>().firstOrNull { 
                it.position.latitude == point.latitude && it.position.longitude == point.longitude 
            }
            clickedMarker?.let {
                showNamePlaceDialog(it.position, it.title ?: "")
                return true
            }
        }
        return false
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        p?.let {
            longPressMarker?.let { map.overlays.remove(it) }
            searchMarker?.let { map.overlays.remove(it) }

            longPressMarker = Marker(map).apply {
                position = p
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Nouveau lieu"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_star)
            }
            map.overlays.add(longPressMarker)
            map.controller.animateTo(p)
            map.invalidate()

            showNamePlaceDialog(p)
        }
        return true
    }

    private fun showNamePlaceDialog(geoPoint: GeoPoint, initialName: String = "") {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nommer ce lieu")

        val input = EditText(requireContext())
        input.hint = "Ex: Maison, Travail..."
        if (initialName.isNotEmpty()) input.setText(initialName)
        builder.setView(input)

        builder.setPositiveButton("Enregistrer") { _, _ ->
            val placeName = input.text.toString().trim()
            if (placeName.isNotBlank()) {
                saveNamedLocation(placeName, geoPoint)
            } else {
                Toast.makeText(requireContext(), "Nom vide", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Annuler") { dialog, _ ->
            if (initialName.isEmpty()) {
                longPressMarker?.let { map.overlays.remove(it) }
                map.invalidate()
            }
            dialog.cancel()
        }
        builder.show()
    }

    private fun saveNamedLocation(name: String, geoPoint: GeoPoint) {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val namedLocation = SmartLifeNamedLocation(userId, name, geoPoint.latitude, geoPoint.longitude)
                val response = apiService.saveNamedLocation(namedLocation)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Lieu enregistré !", Toast.LENGTH_SHORT).show()
                        loadNamedLocations()
                    } else {
                        Toast.makeText(requireContext(), "Erreur serveur: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSearchBar() {
        searchAdapter = SearchAdapter { address ->
            onSearchResultSelected(address)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = searchAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) performSearch(query)
                searchView.clearFocus()
                cvSearchResults.visibility = View.GONE
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrBlank() && newText.length > 2) {
                    getSuggestions(newText)
                } else {
                    cvSearchResults.visibility = View.GONE
                }
                return true
            }
        })
    }

    private fun getSuggestions(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(600) 
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            try {
                val results = withContext(Dispatchers.IO) {
                    try {
                        geocoder.getFromLocationName(query, 5)
                    } catch (e: Exception) { null }
                }
                if (!results.isNullOrEmpty()) {
                    searchAdapter.updateResults(results)
                    cvSearchResults.visibility = View.VISIBLE
                } else {
                    cvSearchResults.visibility = View.GONE
                }
            } catch (e: Exception) {
                cvSearchResults.visibility = View.GONE
            }
        }
    }

    private fun onSearchResultSelected(address: Address) {
        val geoPoint = GeoPoint(address.latitude, address.longitude)
        handleGeocodeResults(listOf(address), address.getAddressLine(0) ?: "")
        cvSearchResults.visibility = View.GONE
        searchView.setQuery(address.getAddressLine(0), false)
        searchView.clearFocus()
        locationOverlay.disableFollowLocation() // On arrête de suivre si on cherche ailleurs
    }

    private fun performSearch(locationName: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(locationName, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: List<Address>) {
                            handleGeocodeResults(addresses, locationName)
                        }
                        override fun onError(error: String?) {
                            Log.e("Search", "Error: $error")
                        }
                    })
                } else {
                    val results = withContext(Dispatchers.IO) { geocoder.getFromLocationName(locationName, 1) }
                    handleGeocodeResults(results, locationName)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de recherche", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGeocodeResults(addressList: List<Address>?, locationName: String) {
        if (!addressList.isNullOrEmpty()) {
            val address = addressList[0]
            val geoPoint = GeoPoint(address.latitude, address.longitude)
            
            searchMarker?.let { map.overlays.remove(it) }
            searchMarker = Marker(map).apply {
                position = geoPoint
                title = address.getAddressLine(0) ?: locationName
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_search)
            }
            map.overlays.add(searchMarker)
            map.controller.animateTo(geoPoint)
            map.controller.setZoom(17.5)
            map.invalidate()
        } else {
            Toast.makeText(requireContext(), "Lieu non trouvé", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTrackingButtons() {
        updateTrackingButtonsState()
        btnStart.setOnClickListener { startTracking() }
        btnStop.setOnClickListener { stopTracking() }
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(requireContext(), GpsTrackingService::class.java)
            ContextCompat.startForegroundService(requireContext(), intent)
            updateTrackingButtonsState()
            locationOverlay.enableFollowLocation()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    private fun stopTracking() {
        requireContext().stopService(Intent(requireContext(), GpsTrackingService::class.java))
        updateTrackingButtonsState()
    }

    private fun updateTrackingButtonsState() {
        val isRunning = isServiceRunning(GpsTrackingService::class.java)
        btnStart.visibility = if (isRunning) View.GONE else View.VISIBLE
        btnStop.visibility = if (isRunning) View.VISIBLE else View.GONE
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val runningServices = manager.getRunningServices(Int.MAX_VALUE)
        return runningServices.any { it.service.className == serviceClass.name }
    }

    private suspend fun loadHabits() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val habits = apiService.getHabits(userId)
            withContext(Dispatchers.Main) {
                habits.frequent_places.forEach { place ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(place.lat, place.lng)
                        title = place.name
                        subDescription = "${place.visits} visites"
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
        } catch (e: Exception) { Log.e("Habits", "Error", e) }
    }

    private suspend fun loadActivityHistory() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val activities = apiService.getActivities(userId)
            if (activities.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val line = Polyline().apply {
                        outlinePaint.color = Color.parseColor("#4A90E2") // Bleu plus doux
                        outlinePaint.strokeWidth = 8f
                        setPoints(activities.map { GeoPoint(it.location.lat, it.location.lng) })
                    }
                    map.overlays.add(line)
                    map.invalidate()
                }
            }
        } catch (e: Exception) { Log.e("History", "Error", e) }
    }

    private suspend fun loadNamedLocations() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val namedLocations = apiService.getNamedLocations(userId)
            withContext(Dispatchers.Main) {
                namedLocations.forEach { location ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(location.lat, location.lng)
                        title = location.name
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_star)
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
        } catch (e: Exception) { Log.e("NamedLoc", "Error", e) }
    }

    private fun setupVoice(fabVoice: FloatingActionButton) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        fabVoice.setOnClickListener { speechRecognizer.startListening(recognitionIntent) }
    }

    override fun onResume() { super.onResume(); map.onResume(); updateTrackingButtonsState() }
    override fun onPause() { super.onPause(); map.onPause() }
}
