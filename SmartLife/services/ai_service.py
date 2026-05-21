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
    """Analyse prédictive et coaching personnalisé basé sur les habitudes réelles vs planning."""
    user = await users_collection.find_one({"uid": user_id})
    schedule = await schedules_collection.find_one({"user_id": user_id})
    habits = await habits_collection.find_one({"user_id": user_id})
    # Récupérer plus d'activités pour détecter des tendances
    recent_activities = await activities_collection.find({"user_id": user_id}).sort("timestamp", -1).to_list(length=20)

    context = {
        "user_name": user.get("firstName", "Ami") if user else "Ami",
        "planning_hebdomadaire": schedule.get("days") if schedule else [],
        "habitudes_detectees": habits if habits else {},
        "historique_recent": recent_activities,
        "date_actuelle": datetime.now().strftime("%A %d %B %Y")
    }

    prompt = f"""
    Tu es l'Elite Wellness Coach de SmartLife. Ton rôle est d'analyser le décalage entre ce que l'utilisateur PRÉVOIT et ce qu'il FAIT réellement.
    
    DONNÉES : {json.dumps(context, default=str)}
    
    MISSIONS :
    1. ANALYSE DES HABITUDES : Identifie 2 habitudes fortes ou points de friction (ex: "Tu es très actif le matin mais tu délaisses tes séances du soir").
    2. SCORE DE DISCIPLINE : Calcule un 'health_score' (0-100). Sois sévère mais juste.
    3. PRÉDICTION : Prédis l'état de forme de l'utilisateur pour la fin de semaine s'il continue sur ce rythme.
    4. CONSEILS : Donne 3 conseils ultra-personnalisés et exploitables immédiatement.

    RÉPONDS UNIQUEMENT EN JSON :
    {{
        "recommendations": ["Conseil 1", "Conseil 2", "Conseil 3"],
        "habits": ["Analyse habitude 1", "Analyse habitude 2"],
        "prediction": "Ta phrase de prédiction ici",
        "health_score": 85
    }}
    """
    try:
        chat_completion = client.chat.completions.create(
            messages=[
                {"role": "system", "content": "Tu es un coach en performance humaine et bien-être. Tu parles de manière directe, inspirante et basée sur les données."},
                {"role": "user", "content": prompt}
            ],
            model=AI_MODEL,
            response_format={"type": "json_object"},
            temperature=0.4 # Moins de hasard pour l'analyse de données
        )
        return json.loads(chat_completion.choices[0].message.content)
    except Exception as e:
        logging.error(f"Groq Insights Error: {e}")
        return {"recommendations": ["Analyse tes priorités"], "habits": ["Données insuffisantes"], "prediction": "Maintiens tes efforts", "health_score": 50}

async def get_chat_response(user_id: str, message: str):
    """Assistant intelligent avec mémoire contextuelle et vision globale de la semaine."""

    user = await users_collection.find_one({"uid": user_id})
    schedule = await schedules_collection.find_one({"user_id": user_id})
    
    # Augmentation de la mémoire à 10 messages (5 échanges) pour un meilleur suivi
    history = await voice_logs_collection.find({"user_id": user_id}).sort("timestamp", -1).to_list(length=10)
    history.reverse()

    user_name = user.get("firstName", "Ami") if user else "Ami"
    routine_info = json.dumps(schedule.get("days") if schedule else [], default=str)
    current_time = datetime.now().strftime("%H:%M")
    current_day = datetime.now().strftime("%A")

    # Prompt système beaucoup plus directif sur l'utilisation de l'historique et du planning
    system_prompt = f"""Tu es SmartAI, l'assistant expert de {user_name}. 
    Nous sommes aujourd'hui {current_day}, il est {current_time}.
    
    TON SAVOIR ABSOLU (Planning de la semaine) :
    {routine_info}
    
    TES MISSIONS :
    1. MÉMOIRE : Référencie TOUJOURS ce que l'utilisateur vient de dire dans les messages précédents pour construire ta réponse. Ne sois pas répétitif.
    2. PLANNING : Si l'utilisateur est perdu, rappelle-lui sa prochaine tâche.
    3. STYLE : Sois court, percutant et amical. Maximum 35 mots.
    4. LANGUE : Uniquement en Français.
    """

    messages = [{"role": "system", "content": system_prompt}]

    # Construction de la mémoire
    for log in history:
        messages.append({"role": "user", "content": log.get("input")})
        messages.append({"role": "assistant", "content": log.get("response")})

    # Message actuel
    messages.append({"role": "user", "content": message})

    try:
        chat_completion = client.chat.completions.create(
            messages=messages,
            model=AI_MODEL,
            temperature=0.7
        )
        ai_response = chat_completion.choices[0].message.content.strip()

        # Sauvegarde de l'échange pour la mémoire future
        await voice_logs_collection.insert_one({
            "user_id": user_id,
            "input": message,
            "response": ai_response,
            "timestamp": datetime.utcnow()
        })

        return ai_response
    except Exception as e:
        logging.error(f"Groq Chat Error: {e}")
        return f"Désolé {user_name}, j'ai un petit souci technique, mais je me rappelle que nous parlions de ton planning !"
