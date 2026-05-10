from pydantic import BaseModel, EmailStr, Field
from typing import Optional, List, Dict
from datetime import datetime

# --- User Models ---
class UserData(BaseModel):
    uid: str = Field(..., min_length=1)
    email: EmailStr
    firstName: str
    lastName: str
    birthDate: str
    fcmToken: Optional[str] = None

class UserResponse(BaseModel):
    uid: str
    email: EmailStr
    firstName: str
    lastName: str
    birthDate: str
    fcmToken: Optional[str] = None
    id: Optional[str] = Field(None, alias="_id")

class FcmTokenUpdate(BaseModel):
    fcmToken: str

# --- Activities Models ---
class Location(BaseModel):
    lat: float
    lng: float

class ActivityData(BaseModel):
    user_id: str
    type: str  # e.g., "walking", "running"
    location: Location
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    duration: int  # in minutes

# --- Habits Models ---
class FrequentPlace(BaseModel):
    name: str
    lat: float
    lng: float
    visits: int

class HabitData(BaseModel):
    user_id: str
    frequent_places: List[FrequentPlace]
    active_hours: List[str]  # e.g., ["08:00", "18:00"]

# --- Recommendations Models ---
class RecommendationData(BaseModel):
    user_id: str
    message: str
    type: str  # e.g., "health", "activity"
    created_at: datetime = Field(default_factory=datetime.utcnow)

# --- Voice Logs Models ---
class VoiceLogData(BaseModel):
    user_id: str
    input: str
    response: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)

# --- Named Location Model ---
class NamedLocation(BaseModel):
    user_id: str
    name: str
    lat: float
    lng: float
    created_at: datetime = Field(default_factory=datetime.utcnow)

# --- Schedule Models ---
class ScheduleItem(BaseModel):
    time: str  # e.g., "08:30"
    activity_type: str
    location_name: Optional[str] = None
    lat: Optional[float] = None
    lng: Optional[float] = None
    duration: int

class DaySchedule(BaseModel):
    day_of_week: str
    items: List[ScheduleItem] = []

class WeeklySchedule(BaseModel):
    user_id: str
    days: List[DaySchedule]

# --- AI Coach Models ---
class AIInsightsResponse(BaseModel):
    recommendations: List[str]
    habits: List[str]
    prediction: str

# --- Generic Response ---
class SuccessResponse(BaseModel):
    status: str = "success"
    message: str
    data: Optional[Dict] = None

class ErrorResponse(BaseModel):
    status: str = "error"
    message: str
