package com.ABenhadar.smartlife

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ABenhadar.smartlife.api.RetrofitClient
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
    
    private val auth = FirebaseAuth.getInstance()
    private val apiService by lazy { RetrofitClient.getApiService() }
    
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

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

        setupVoiceAssistant()
        loadCoachInsights()

        return view
    }

    private fun setupVoiceAssistant() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        
        tts = TextToSpeech(requireContext()) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.language = Locale.FRENCH
            }
        }

        val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                fabTalk.text = "Je vous écoute..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                fabTalk.text = "Parler au Coach"
            }
            override fun onError(error: Int) {
                fabTalk.text = "Parler au Coach"
                Toast.makeText(context, "Erreur vocale", Toast.LENGTH_SHORT).show()
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

        fabTalk.setOnClickListener {
            speechRecognizer.startListening(recognitionIntent)
        }
    }

    private fun processVoiceCommand(command: String) {
        // Logique simple pour répondre à l'utilisateur
        val response = when {
            command.lowercase().contains("conseil") -> "Je vous suggère de faire une petite marche aujourd'hui."
            command.lowercase().contains("bravo") -> "Merci ! Vous faites de l'excellent travail."
            else -> "C'est noté. Je continue d'apprendre de vos habitudes."
        }
        
        tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
        Toast.makeText(context, "Vous: $command", Toast.LENGTH_LONG).show()
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
                Log.e("CoachFragment", "Error loading insights", e)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(context, "Impossible de joindre le coach", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayInsights(insights: com.ABenhadar.smartlife.models.AIInsightsResponse) {
        tvPrediction.text = insights.prediction
        tvHabits.text = insights.habits.joinToString("\n• ") { it }
        
        llRecsContainer.removeAllViews()
        insights.recommendations.forEach { rec ->
            val card = LayoutInflater.from(context).inflate(R.layout.item_recommendation, llRecsContainer, false)
            card.findViewById<TextView>(R.id.tvRecMessage).text = rec
            llRecsContainer.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
    }
}
