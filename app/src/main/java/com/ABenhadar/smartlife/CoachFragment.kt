package com.ABenhadar.smartlife

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.AIInsightsResponse
import com.ABenhadar.smartlife.models.ChatRequest
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CoachFragment : Fragment() {

    private lateinit var tvPrediction: TextView
    private lateinit var tvHabits: TextView
    private lateinit var llRecsContainer: LinearLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var fabTalk: ExtendedFloatingActionButton
    
    // Avatar components
    private lateinit var cvAvatarContainer: MaterialCardView
    private lateinit var tvCoachSpeech: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var vPulse: View
    
    private val auth = FirebaseAuth.getInstance()
    private val apiService by lazy { RetrofitClient.getApiService() }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "L'accès au micro est nécessaire pour parler au coach.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_coach, container, false)

        tvPrediction = view.findViewById(R.id.tvAIPrediction)
        tvHabits = view.findViewById(R.id.tvDetectedHabits)
        llRecsContainer = view.findViewById(R.id.llCoachRecsContainer)
        pbLoading = view.findViewById(R.id.pbCoachLoading)
        fabTalk = view.findViewById(R.id.fabTalkToCoach)
        
        cvAvatarContainer = view.findViewById(R.id.cvAvatarContainer)
        tvCoachSpeech = view.findViewById(R.id.tvCoachSpeech)
        ivAvatar = view.findViewById(R.id.ivCoachAvatar)
        vPulse = view.findViewById(R.id.vAvatarPulse)

        setupVoiceAssistant()
        loadCoachInsights()

        return view
    }

    private fun setupVoiceAssistant() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    fabTalk.text = "Je vous écoute..."
                    tvCoachSpeech.text = "Posez-moi une question..."
                    startPulseAnimation()
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    fabTalk.text = "Parler au Coach"
                    stopPulseAnimation()
                }
                override fun onError(error: Int) {
                    fabTalk.text = "Parler au Coach"
                    stopPulseAnimation()
                    Log.e("Coach", "Speech error: $error")
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        processVoiceCommand(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        tts = TextToSpeech(requireContext()) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.language = Locale.FRENCH
            }
        }

        fabTalk.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "fr-FR")
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur lors du démarrage du micro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPulseAnimation() {
        vPulse.visibility = View.VISIBLE
        val pulse = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        pulse.duration = 500
        pulse.repeatCount = Animation.INFINITE
        pulse.repeatMode = Animation.REVERSE
        vPulse.startAnimation(pulse)
    }

    private fun stopPulseAnimation() {
        vPulse.clearAnimation()
        vPulse.visibility = View.INVISIBLE
    }

    private fun processVoiceCommand(command: String) {
        val userId = auth.currentUser?.uid ?: return
        tvCoachSpeech.text = "Réflexion en cours..."
        startPulseAnimation()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chatResponse = apiService.getAIChatResponse(userId, ChatRequest(command))
                withContext(Dispatchers.Main) {
                    val responseText = chatResponse.response
                    tvCoachSpeech.text = responseText
                    tts.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, "CoachSpeech")
                    stopPulseAnimation()
                    
                    // Animation de l'avatar quand il parle
                    ivAvatar.animate().scaleX(1.15f).scaleY(1.15f).setDuration(300).withEndAction {
                        ivAvatar.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300)
                    }.start()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvCoachSpeech.text = "Désolé, j'ai rencontré un problème technique."
                    stopPulseAnimation()
                }
            }
        }
    }

    private fun loadCoachInsights() {
        val userId = auth.currentUser?.uid ?: return
        pbLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val insights = apiService.getAIInsights(userId)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    displayInsights(insights)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    tvPrediction.text = "Je prépare votre analyse personnalisée..."
                }
            }
        }
    }

    private fun displayInsights(insights: AIInsightsResponse) {
        tvPrediction.text = insights.prediction
        tvHabits.text = if (insights.habits.isEmpty()) "Découverte de votre rythme de vie..." else insights.habits.joinToString("\n• ") { it }
        
        llRecsContainer.removeAllViews()
        insights.recommendations.forEach { rec ->
            val card = LayoutInflater.from(context).inflate(R.layout.item_recommendation, llRecsContainer, false)
            card.findViewById<TextView>(R.id.tvRecMessage).text = rec
            llRecsContainer.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
