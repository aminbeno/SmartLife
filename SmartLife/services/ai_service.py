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
    """Analyse experte basée sur la structure WeeklySchedule, ScheduleItem et ActivityData via Groq."""
    user = await users_collection.find_one({"uid": user_id})
    schedule = await schedules_collection.find_one({"user_id": user_id})
    habits = await habits_collection.find_one({"user_id": user_id})
    recent_activities = await activities_collection.find({"user_id": user_id}).sort("timestamp", -1).to_list(length=10)

    context = {
        "user_name": user.get("firstName", "Ami") if user else "Ami",
        "routine_hebdomadaire": [],
        "habitudes_detectees": {
            "heures_actives": habits.get("active_hours", []) if habits else [],
            "lieux_frequents": [p.get("name") for p in habits.get("frequent_places", [])] if habits else []
        },
        "activites_reelles_recentes": [
            {
                "type": a.get("type"),
                "duree_reelle": a.get("duration"),
                "lieu": a.get("locationName"),
                "date": a.get("timestamp")
            } for a in recent_activities
        ]
    }

    if schedule:
        for day in schedule.get('days', []):
            day_data = {"jour": day.get('day_of_week'), "items": []}
            for i in day.get('items', []):
                day_data["items"].append({
                    "heure": i.get('time'),
                    "activite": i.get('activity_type'),
                    "lieu_prevu": i.get('location_name'),
                    "duree_prevue_min": i.get('duration')
                })
            context["routine_hebdomadaire"].append(day_data)

    prompt = f"""
    Tu es l'Elite Coach SmartLife. Analyse ces données pour donner un coaching de HAUTE QUALITÉ.
    DONNÉES : {json.dumps(context, default=str)}

    MISSION :
    1. Compare la Routine vs la Réalité. Si une durée réelle est inférieure à la durée prévue, signale l'écart avec bienveillance.
    2. Vérifie si l'utilisateur respecte ses lieux prévus.
    3. Formule 3 recommandations constructives.

    CONSIGNES DE FORMAT ET STYLE :
    - Réponds UNIQUEMENT en JSON valide.
    - "habits" DOIT être une LISTE de chaînes de caractères.
    - "recommendations" DOIT être une LISTE de chaînes de caractères.
    - Langue : Français.
    - LONGUEUR : Développe un peu plus tes conseils (environ 15 à 25 mots par recommandation) pour qu'ils soient vraiment utiles.

    STRUCTURE JSON ATTENDUE :
    {{
        "recommendations": ["Conseil détaillé 1", "Conseil détaillé 2", "Conseil détaillé 3"],
        "habits": ["Une analyse approfondie de sa discipline cette semaine"],
        "prediction": "Une prédiction motivante et détaillée pour demain"
    }}
    """
    try:
        chat_completion = client.chat.completions.create(
            messages=[
                {
                    "role": "system",
                    "content": "Tu es un coach de vie expert, motivant et précis. Tu réponds uniquement en JSON."
                },
                {
                    "role": "user",
                    "content": prompt,
                }
            ],
            model=AI_MODEL,
            response_format={"type": "json_object"},
        )

        data = json.loads(chat_completion.choices[0].message.content)

        # Sécurités de type
        if isinstance(data.get("habits"), str):
            data["habits"] = [data["habits"]]
        if not isinstance(data.get("recommendations"), list):
            data["recommendations"] = [str(data.get("recommendations"))]

        return data
    except Exception as e:
        logging.error(f"Insights Error Groq: {e}")
        return {
            "recommendations": ["Gardez le cap sur vos objectifs de santé !"],
            "habits": ["Analyse de routine en cours..."],
            "prediction": "Demain sera une excellente journée pour progresser."
        }

async def get_chat_response(user_id: str, message: str):
    """Réponse vocale dynamique et naturelle via Groq."""
    schedule = await schedules_collection.find_one({"user_id": user_id})
    now = datetime.now()
    time_str = now.strftime("%H:%M")
    current_day = ["Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"][now.weekday()]

    plan_info = "Rien de particulier de prévu"
    if schedule:
        for day in schedule.get('days', []):
            if day.get('day_of_week').lower() in current_day.lower():
                items = [f"{i.get('time')} {i.get('activity_type')}" for i in day.get('items', [])]
                if items:
                    plan_info = ", ".join(items)

    prompt = f"Coach SmartLife. Il est {time_str} ({current_day}). Planning : {plan_info}. L'utilisateur te dit : '{message}'. Réponds en français de manière naturelle, chaleureuse et complète (environ 2 à 4 phrases)."

    try:
        chat_completion = client.chat.completions.create(
            messages=[
                {"role": "system", "content": "Tu es un coach de vie bienveillant qui donne des réponses complètes et encourageantes en français."},
                {"role": "user", "content": prompt}
            ],
            model=AI_MODEL,
        )
        return chat_completion.choices[0].message.content.strip()
    except Exception as e:
        logging.error(f"Chat Error Groq: {e}")
        return "Je suis là pour vous accompagner. Comment se passe votre journée par rapport à votre programme ?"