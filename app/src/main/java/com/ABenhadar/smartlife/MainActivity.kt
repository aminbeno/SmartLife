package com.ABenhadar.smartlife

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.os.Bundle as SpeechBundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.VoiceLogData
import com.ABenhadar.smartlife.service.GpsTrackingService
import com.ABenhadar.smartlife.viewmodel.UserViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val viewModel: UserViewModel by viewModels()
    private var isTracking = false
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val apiService by lazy { RetrofitClient.getApiService() }
    
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionIntent: Intent

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationOverlay.enableMyLocation()
        }
        checkBackgroundLocationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuration indispensable pour OSMDroid avant le setContentView
        Configuration.getInstance().userAgentValue = packageName

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        map = findViewById(R.id.map)
        setupMap()

        // Chargement progressif
        lifecycleScope.launch {
            try {
                setupUI()
                delay(500)
                setupVoice()
                updateFCMToken()
                loadActivityHistory()
                loadHabits()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during async setup", e)
            }
        }
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(15.0)
        
        // Overlay de localisation
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        // Point par défaut si pas de loc (ex: Paris)
        val startPoint = GeoPoint(48.8583, 2.2944)
        mapController.setCenter(startPoint)
    }

    private fun setupUI() {
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnToggleTracking = findViewById<Button>(R.id.btnToggleTracking)
        val tvTrackingStatus = findViewById<TextView>(R.id.tvTrackingStatus)
        val cvRecommendation = findViewById<View>(R.id.cvRecommendation)
        val btnCloseRec = findViewById<View>(R.id.btnCloseRec)
        val fabProfile = findViewById<FloatingActionButton>(R.id.fabProfile)

        tvWelcome.text = getString(R.string.welcome_msg, auth.currentUser?.email ?: "User")

        btnToggleTracking.setOnClickListener {
            if (isTracking) {
                stopTrackingService()
                btnToggleTracking.text = getString(R.string.btn_start)
                tvTrackingStatus.text = getString(R.string.status_stopped)
                isTracking = false
            } else {
                if (checkLocationPermissions()) {
                    startTrackingService()
                    btnToggleTracking.text = getString(R.string.btn_stop)
                    tvTrackingStatus.text = getString(R.string.status_running)
                    isTracking = true
                }
            }
        }

        btnLogout.setOnClickListener {
            stopTrackingService()
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        fabProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnCloseRec.setOnClickListener { cvRecommendation.visibility = View.GONE }
    }

    private fun updateFCMToken() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                viewModel.updateFCMToken(userId, task.result)
            }
        }
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

    private fun setupVoice() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
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
                    Log.e("MainActivity", "Voice Error: $error")
                }
                override fun onResults(results: SpeechBundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) processVoiceInput(matches[0])
                }
                override fun onPartialResults(partialResults: SpeechBundle?) {}
                override fun onEvent(eventType: Int, params: SpeechBundle?) {}
            })

            findViewById<FloatingActionButton>(R.id.fabVoice).setOnClickListener {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if (::speechRecognizer.isInitialized) {
                        speechRecognizer.startListening(recognitionIntent)
                    }
                } else {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Voice setup failed", e)
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
                    Toast.makeText(this@MainActivity, "You said: $input", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {}
        }
    }

    private fun showRecommendation(message: String) {
        val cvRecommendation = findViewById<View>(R.id.cvRecommendation)
        val tvRecommendation = findViewById<TextView>(R.id.tvRecommendation)
        tvRecommendation.text = message
        cvRecommendation.visibility = View.VISIBLE
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            checkBackgroundLocationPermission()
            true
        } else {
            val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            requestPermissionLauncher.launch(permissions.toTypedArray())
            false
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, GpsTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopTrackingService() {
        val intent = Intent(this, GpsTrackingService::class.java)
        stopService(intent)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            try {
                speechRecognizer.destroy()
            } catch (e: Exception) {}
        }
    }
}
