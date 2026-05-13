from fastapi import APIRouter, HTTPException, status
from models import (
    UserData, UserResponse, SuccessResponse, ErrorResponse,
    ActivityData, HabitData, RecommendationData, VoiceLogData, FcmTokenUpdate, NamedLocation,
    WeeklySchedule, AIInsightsResponse
)
from database import (
    users_collection, activities_collection, habits_collection,
    recommendations_collection, voice_logs_collection, named_locations_collection,
    schedules_collection
)
from services.ai_service import generate_ai_coach_insights, get_chat_response
from bson import ObjectId
from typing import List, Optional
from pydantic import BaseModel
from datetime import datetime, timedelta

router = APIRouter(prefix="/api", tags=["smartlife"])

class ChatRequest(BaseModel):
    message: str

# --- SCHEDULE ENDPOINTS ---

@router.get("/schedule/{user_id}", response_model=WeeklySchedule)
async def get_weekly_schedule(user_id: str, week_id: Optional[str] = None):
    # Si aucun week_id n'est fourni, on cherche la semaine actuelle (Lundi)
    if not week_id:
        now = datetime.now()
        monday = now - timedelta(days=now.weekday())
        week_id = monday.strftime("%Y-%m-%d")
        
    schedule = await schedules_collection.find_one({"user_id": user_id, "week_id": week_id})
    
    if not schedule:
        # Retourner une structure vide synchronisée sur la semaine demandée
        monday_dt = datetime.strptime(week_id, "%Y-%m-%d")
        days_fr = ["Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"]
        days_list = []
        for i, name in enumerate(days_fr):
            date_str = (monday_dt + timedelta(days=i)).strftime("%Y-%m-%d")
            days_list.append({"day_of_week": name, "date": date_str, "items": []})
            
        return {"user_id": user_id, "week_id": week_id, "days": days_list}
        
    if "_id" in schedule:
        schedule["_id"] = str(schedule["_id"])
    return schedule

@router.post("/schedule", response_model=SuccessResponse)
async def save_weekly_schedule(schedule: WeeklySchedule):
    # Mise à jour basée sur l'utilisateur ET la semaine spécifique
    result = await schedules_collection.replace_one(
        {"user_id": schedule.user_id, "week_id": schedule.week_id},
        schedule.model_dump(),
        upsert=True
    )
    return {"status": "success", "message": f"Schedule for week {schedule.week_id} updated"}

# --- AUTRES ENDPOINTS (Conservés) ---
@router.post("/register_user", response_model=SuccessResponse, status_code=status.HTTP_201_CREATED)
async def register_user(user: UserData):
    existing_user = await users_collection.find_one({"uid": user.uid})
    if existing_user:
        raise HTTPException(status_code=400, detail="User already exists")
    result = await users_collection.insert_one(user.model_dump())
    return {"status": "success", "message": "User registered", "data": {"id": str(result.inserted_id)}}

@router.get("/get_user/{uid}", response_model=UserResponse)
async def get_user(uid: str):
    user = await users_collection.find_one({"uid": uid})
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    user["_id"] = str(user["_id"])
    return user

@router.put("/update_user/{uid}", response_model=SuccessResponse)
async def update_user(uid: str, user: UserData):
    result = await users_collection.update_one({"uid": uid}, {"$set": user.model_dump(exclude={"uid"})})
    if result.matched_count == 0:
        raise HTTPException(status_code=404, detail="User not found")
    return {"status": "success", "message": "User updated successfully"}

@router.post("/activity", response_model=SuccessResponse)
async def add_activity(activity: ActivityData):
    result = await activities_collection.insert_one(activity.model_dump())
    return {"status": "success", "message": "Activity saved", "data": {"id": str(result.inserted_id)}}

@router.get("/activities/{user_id}", response_model=List[ActivityData])
async def get_activities(user_id: str):
    cursor = activities_collection.find({"user_id": user_id})
    return await cursor.to_list(length=100)

@router.get("/habits/{user_id}", response_model=HabitData)
async def get_habits(user_id: str):
    habits = await habits_collection.find_one({"user_id": user_id})
    if not habits: raise HTTPException(status_code=404, detail="Habits not found")
    return habits

@router.post("/named_locations", response_model=SuccessResponse, status_code=status.HTTP_201_CREATED)
async def add_named_location(location: NamedLocation):
    result = await named_locations_collection.insert_one(location.model_dump())
    return {"status": "success", "message": "Named location saved", "data": {"id": str(result.inserted_id)}}

@router.get("/named_locations/{user_id}", response_model=List[NamedLocation])
async def get_named_locations(user_id: str):
    cursor = named_locations_collection.find({"user_id": user_id})
    locations = await cursor.to_list(length=100)
    for loc in locations:
        loc['id'] = str(loc['_id'])
        del loc['_id']
    return locations

@router.get("/ai_insights/{user_id}", response_model=AIInsightsResponse)
async def get_ai_insights(user_id: str):
    return await generate_ai_coach_insights(user_id)

@router.post("/ai_chat/{user_id}")
async def ai_chat(user_id: str, request: ChatRequest):
    response = await get_chat_response(user_id, request.message)
    return {"status": "success", "response": response}
