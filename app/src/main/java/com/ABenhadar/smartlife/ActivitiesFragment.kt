package com.ABenhadar.smartlife

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
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
import com.ABenhadar.smartlife.models.ActivityData
import com.ABenhadar.smartlife.models.NamedLocation as SmartLifeNamedLocation
import com.ABenhadar.smartlife.service.GpsTrackingService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
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
    private lateinit var cvLiveStats: MaterialCardView
    private lateinit var tvLiveDist: TextView
    private lateinit var tvLiveSpeed: TextView
    
    private var searchMarker: Marker? = null
    private var longPressMarker: Marker? = null
    private var routingPolyline: Polyline? = null
    private var searchJob: Job? = null
    private var startLocation: Location? = null
    private var totalDistance = 0f

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
        btnStart = view.findViewById(R.id.btnStartTracking)
        btnStop = view.findViewById(R.id.btnStopTracking)
        searchView = view.findViewById(R.id.searchView)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        cvSearchResults = view.findViewById(R.id.cvSearchResults)
        cvLiveStats = view.findViewById(R.id.cvLiveStats)
        tvLiveDist = view.findViewById(R.id.tvLiveDist)
        tvLiveSpeed = view.findViewById(R.id.tvLiveSpeed)
        
        setupMap()
        setupVoice(view.findViewById(R.id.fabVoice))
        setupTrackingButtons()
        setupSearchBar()

        val cvRecommendation = view.findViewById<MaterialCardView>(R.id.cvRecommendation)
        view.findViewById<ImageButton>(R.id.btnCloseRec)?.setOnClickListener {
            cvRecommendation?.visibility = View.GONE
        }

        fabRecenter.setOnClickListener {
            locationOverlay.enableFollowLocation()
            locationOverlay.myLocation?.let { 
                map.controller.animateTo(it)
                map.controller.setZoom(18.0)
            }
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

        val provider = GpsMyLocationProvider(requireContext())
        locationOverlay = object : MyLocationNewOverlay(provider, map) {
            override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
                super.onLocationChanged(location, source)
                location?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateLiveStats(it)
                    }
                }
            }
        }
        
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        locationOverlay.runOnFirstFix {
            lifecycleScope.launch(Dispatchers.Main) {
                map.controller.animateTo(locationOverlay.myLocation)
                map.controller.setZoom(17.5)
                loadNextActivityAndRoute()
            }
        }
        
        map.overlays.add(locationOverlay)
        map.overlays.add(0, MapEventsOverlay(this))
        
        map.controller.setCenter(GeoPoint(33.5731, -7.5898))
    }

    private suspend fun loadNextActivityAndRoute() {
        val userId = auth.currentUser?.uid ?: return
        
        val cal = Calendar.getInstance(Locale.FRANCE)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { cal.add(Calendar.DAY_OF_MONTH, -1) }
        val weekId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        try {
            val schedule = apiService.getWeeklySchedule(userId, weekId)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val currentDayFr = SimpleDateFormat("EEEE", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() }
            
            val todayItems = schedule.days.find { it.day_of_week.equals(currentDayFr, ignoreCase = true) }?.items
            val nextItem = todayItems?.filter { it.time > currentTime }?.minByOrNull { it.time }

            if (nextItem?.lat != null && nextItem.lng != null && nextItem.lat != 0.0 && nextItem.lng != 0.0) {
                drawRouteTo(GeoPoint(nextItem.lat, nextItem.lng))
            }
        } catch (e: Exception) { Log.e("Routing", "Error loading schedule", e) }
    }

    private fun drawRouteTo(destination: GeoPoint) {
        val start = locationOverlay.myLocation ?: run {
            Toast.makeText(context, "Position GPS non disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val distanceToDest = start.distanceToAsDouble(destination)
        if (distanceToDest > 5000000) {
            Log.w("Routing", "Destination trop lointaine ($distanceToDest m), probablement une erreur de localisation.")
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=geojson"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "SmartLifeApp-Android") 
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val routes = json.getJSONArray("routes")
                    
                    if (routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                        val coords = geometry.getJSONArray("coordinates")
                        val points = mutableListOf<GeoPoint>()
                        
                        for (i in 0 until coords.length()) {
                            val c = coords.getJSONArray(i)
                            points.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
                        }
                        
                        withContext(Dispatchers.Main) {
                            routingPolyline?.let { map.overlays.remove(it) }
                            routingPolyline = Polyline().apply {
                                setPoints(points)
                                outlinePaint.color = Color.parseColor("#1A73E8")
                                outlinePaint.strokeWidth = 10f
                            }
                            map.overlays.add(routingPolyline)
                            map.invalidate()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Routing", "Route calculation failed", e)
            }
        }
    }

    private fun updateLiveStats(location: Location) {
        if (!isServiceRunning(GpsTrackingService::class.java)) return
        
        if (startLocation == null) {
            startLocation = location
            totalDistance = 0f
        } else {
            totalDistance += startLocation!!.distanceTo(location)
            startLocation = location
        }

        val distKm = totalDistance / 1000
        val speedKmh = location.speed * 3.6

        tvLiveDist.text = String.format(Locale.getDefault(), "%.2f km", distKm)
        tvLiveSpeed.text = String.format(Locale.getDefault(), "%.0f km/h", speedKmh)
    }

    private fun setupTrackingButtons() {
        updateTrackingButtonsState()
        btnStart.setOnClickListener { 
            startTracking()
            cvLiveStats.visibility = View.VISIBLE
            startLocation = null
            totalDistance = 0f
        }
        btnStop.setOnClickListener { 
            stopTracking()
            cvLiveStats.visibility = View.GONE
        }
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

    private fun setupSearchBar() {
        searchAdapter = SearchAdapter { address -> onSearchResultSelected(address) }
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
            val results = withContext(Dispatchers.IO) {
                try { 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        null
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 5)
                    }
                } catch (e: Exception) { null }
            }
            
            val finalResults = results ?: withContext(Dispatchers.IO) {
                try { @Suppress("DEPRECATION") geocoder.getFromLocationName(query, 5) } catch(e: Exception) { null }
            }

            if (!finalResults.isNullOrEmpty()) {
                searchAdapter.updateResults(finalResults)
                cvSearchResults.visibility = View.VISIBLE
            } else {
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
        locationOverlay.disableFollowLocation()
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
                    @Suppress("DEPRECATION")
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
                setOnMarkerClickListener { marker, _ ->
                    drawRouteTo(marker.position)
                    marker.showInfoWindow()
                    true
                }
            }
            map.overlays.add(searchMarker)
            map.controller.animateTo(geoPoint)
            map.controller.setZoom(17.5)
            map.invalidate()
        }
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
            longPressMarker = Marker(map).apply {
                position = p
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Nouveau lieu"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_star)
            }
            map.overlays.add(longPressMarker)
            map.controller.animateTo(p)
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
            val name = input.text.toString().trim()
            if (name.isNotBlank()) saveNamedLocation(name, geoPoint)
        }
        builder.setNegativeButton("Annuler") { _, _ -> 
            if (initialName.isEmpty()) { map.overlays.remove(longPressMarker); map.invalidate() }
        }
        builder.show()
    }

    private fun saveNamedLocation(name: String, geoPoint: GeoPoint) {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loc = SmartLifeNamedLocation(userId, name, geoPoint.latitude, geoPoint.longitude)
                if (apiService.saveNamedLocation(loc).isSuccessful) {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(requireContext(), "Lieu enregistré !", Toast.LENGTH_SHORT).show()
                        loadNamedLocations() 
                    }
                }
            } catch (e: Exception) { Log.e("API", "Error", e) }
        }
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
                        subDescription = String.format(Locale.getDefault(), "%d visites", place.visits)
                        setOnMarkerClickListener { marker, _ ->
                            drawRouteTo(marker.position)
                            marker.showInfoWindow()
                            true
                        }
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
            val response = apiService.getActivities(userId)
            val activities: List<ActivityData> = response.data ?: emptyList()
            if (activities.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val line = Polyline().apply {
                        outlinePaint.color = Color.parseColor("#4A90E2")
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
            val locs = apiService.getNamedLocations(userId)
            withContext(Dispatchers.Main) {
                locs.forEach { location ->
                    val m = Marker(map).apply {
                        position = GeoPoint(location.lat, location.lng)
                        title = location.name
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_star)
                        setOnMarkerClickListener { marker, _ ->
                            drawRouteTo(marker.position)
                            marker.showInfoWindow()
                            true
                        }
                    }
                    map.overlays.add(m)
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

    override fun onResume() { 
        super.onResume()
        map.onResume()
        updateTrackingButtonsState()
        if (isServiceRunning(GpsTrackingService::class.java)) cvLiveStats.visibility = View.VISIBLE
    }
    
    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
