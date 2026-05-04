from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes.user_routes import router
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="SmartLife API",
    description="Backend API for SmartLife Mobile App",
    version="1.0.0"
)

# CORS configuration for mobile app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Change to specific domains in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)

@app.get("/health")
async def health_check():
    """Health check endpoint compliant with SuccessResponse model"""
    return {
        "status": "success",
        "message": "API is healthy",
        "data": {"service": "SmartLife API", "status": "online"}
    }
