import google.generativeai as genai
import json
import logging
# Il manquait 'os' pour charger la clé API de manière sécurisée, je l'ai ajouté
import os
from database import schedules_collection, recommendations_collection
from models import RecommendationData
from datetime import datetime

# It is important not to hardcode secrets in source code.
# Use environment variables like AI_STUDIO_KEY for a secure configuration.
AI_STUDIO_KEY = os.getenv("AI_STUDIO_KEY")
AI_MODEL = os.getenv("AI_MODEL", "models/gemini-2.5-flash")

if not AI_STUDIO_KEY:
    raise EnvironmentError("AI_STUDIO_KEY environment variable is required for the AI service.")

genai.configure(api_key=AI_STUDIO_KEY)
model = genai.GenerativeModel(AI_MODEL)

async def generate_ai_coach_insights(user_id: str):
    # 1. Récupérer le programme (schedules) de l'utilisateur
    schedule = await schedules_collection.find_one({"user_id": user_id})

    if not schedule:
        return {
            "recommendations": ["Commencez par planifier votre semaine dans l'onglet Journal pour recevoir des conseils personnalisés !"],
            "habits": ["Pas encore assez de données pour détecter des habitudes."],
            "prediction": "Planifiez votre journée pour que je puisse vous aider !"
        }

    # 2. Préparer le contexte pour l'IA
    context = "Voici le programme hebdomadaire de l'utilisateur :\n"
    for day in schedule.get('days', []):
        day_name = day.get('day_of_week')
        items = day.get('items', [])
        context += f"- {day_name}: "
        for item in items:
            context += f"{item.get('time')} {item.get('activity_type')} à {item.get('location_name')}; "
        context += "\n"

    prompt = f"""
    En tant que coach de vie IA bienveillant pour l'application SmartLife, analyse ce programme :
    {context}
    
    Fournis :
    1. 3 recommandations proactives (ex: 'Pense à prendre une bouteille d'eau pour ta marche de 8h').
    2. 1 habitude détectée (ex: 'Tu sembles être une personne matinale').
    3. 1 prédiction encourageante pour demain.

    RÉPONDS UNIQUEMENT AU FORMAT JSON SUIVANT :
    {{
        "recommendations": ["rec1", "rec2", "rec3"],
        "habits": ["ton analyse d'habitude"],
        "prediction": "ta prédiction"
    }}
    """

    try:
        # Use the async generation API provided by google.generativeai.
        # This avoids blocking the FastAPI event loop.
        response = await model.generate_content_async(prompt)
        content = response.text

        # Nettoyage pour extraire le JSON au cas où l'IA ajoute du texte superflu
        if "{" in content:
            content = content[content.find("{"):content.rfind("}")+1]

        data = json.loads(content)

        # Log des recommandations dans la collection (optionnel)
        for rec_text in data.get("recommendations", []):
            try:
                await recommendations_collection.insert_one({
                    "user_id": user_id,
                    "message": rec_text,
                    "type": "ai_coach",
                    "created_at": datetime.utcnow()
                })
            except Exception as e: # Il est bon de logger l'exception même si on l'ignore
                logging.warning(f"Failed to insert recommendation for user {user_id}: {e}")

        return data
    except Exception as e:
        logging.error(f"AI Service Error: {e}")
        return {
            "recommendations": ["Continuez à prendre soin de vous !", "Une petite marche fait toujours du bien.", "Restez hydraté !"],
            "habits": ["Analyse des habitudes en cours..."],
            "prediction": "Demain sera une journée pleine de potentiel !"
        }