import json
import logging
import os
from database import (
    schedules_collection, recommendations_collection, voice_logs_collection,
    users_collection, activities_collection, habits_collection
)
from datetime import datetime
from groq import Groq

# Configuration Groq
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
AI_MODEL = os.getenv("AI_MODEL", "llama-3.3-70b-versatile")

if not GROQ_API_KEY:
    raise EnvironmentError("GROQ_API_KEY est requis pour le service AI.")

client = Groq(api_key=GROQ_API_KEY)

async def generate_ai_coach_insights(user_id: str):
    """Analyse experte basée sur la structure WeeklySchedule et ActivityData."""
    user = await users_collection.find_one({"uid": user_id})
    schedule = await schedules_collection.find_one({"user_id": user_id})
    habits = await habits_collection.find_one({"user_id": user_id})
    recent_activities = await activities_collection.find({"user_id": user_id}).sort("timestamp", -1).to_list(length=10)

    context = {
        "user_name": user.get("firstName", "Ami") if user else "Ami",
        "routine": schedule.get("days") if schedule else [],
        "habitudes": habits if habits else {},
        "activites": recent_activities
    }

    prompt = f"""
    Tu es l'Elite Coach SmartLife. Analyse ces données : {json.dumps(context, default=str)}
    
    MISSIONS :
    1. Compare Routine vs Réalité.
    2. Calcule un 'health_score' (0-100) basé sur la discipline.
    3. Formule 3 recommandations très courtes (max 10 mots).

    RÉPONDS UNIQUEMENT EN JSON :
    {{
        "recommendations": ["..."],
        "habits": ["..."],
        "prediction": "...",
        "health_score": 85
    }}
    """
    try:
        chat_completion = client.chat.completions.create(
            messages=[{"role": "system", "content": "Expert Coach."}, {"role": "user", "content": prompt}],
            model=AI_MODEL,
            response_format={"type": "json_object"},
        )
        data = json.loads(chat_completion.choices[0].message.content)
        return data
    except Exception as e:
        logging.error(f"Groq Error: {e}")
        return {"recommendations": ["Bougez aujourd'hui !"], "habits": ["En analyse"], "prediction": "Super demain", "health_score": 50}

async def get_chat_response(user_id: str, message: str):
    """Réponse vocale courte."""
    prompt = f"Coach SmartLife. Utilisateur dit: '{message}'. Réponds en FR, MAX 15 MOTS, motivant."
    try:
        chat_completion = client.chat.completions.create(
            messages=[{"role": "user", "content": prompt}],
            model=AI_MODEL,
        )
        return chat_completion.choices[0].message.content.strip()
    except:
        return "Je suis là pour vous."
