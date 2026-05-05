from fastapi import APIRouter, HTTPException, status
from models import (
    UserData, UserResponse, SuccessResponse, ErrorResponse,
    ActivityData, HabitData, RecommendationData, VoiceLogData
)
from database import (
    users_collection, activities_collection, habits_collection,
    recommendations_collection, voice_logs_collection
)
from bson import ObjectId
from typing import List

router = APIRouter(prefix="/api", tags=["smartlife"])

# --- USER ENDPOINTS ---

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

# --- ACTIVITY ENDPOINTS ---

@router.post("/activity", response_model=SuccessResponse)
async def add_activity(activity: ActivityData):
    result = await activities_collection.insert_one(activity.model_dump())
    return {"status": "success", "message": "Activity saved", "data": {"id": str(result.inserted_id)}}

@router.get("/activities/{user_id}", response_model=List[ActivityData])
async def get_activities(user_id: str):
    cursor = activities_collection.find({"user_id": user_id})
    activities = await cursor.to_list(length=100)
    return activities

# --- HABITS ENDPOINTS ---

@router.get("/habits/{user_id}", response_model=HabitData)
async def get_habits(user_id: str):
    habits = await habits_collection.find_one({"user_id": user_id})
    if not habits:
        raise HTTPException(status_code=404, detail="Habits not found")
    return habits

# --- RECOMMENDATION ENDPOINTS ---

@router.post("/recommendation", response_model=SuccessResponse)
async def add_recommendation(rec: RecommendationData):
    result = await recommendations_collection.insert_one(rec.model_dump())
    return {"status": "success", "message": "Recommendation saved", "data": {"id": str(result.inserted_id)}}

# --- VOICE ENDPOINTS ---

@router.post("/voice", response_model=SuccessResponse)
async def add_voice_log(voice: VoiceLogData):
    result = await voice_logs_collection.insert_one(voice.model_dump())
    return {"status": "success", "message": "Voice log saved", "data": {"id": str(result.inserted_id)}}
