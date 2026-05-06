from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes.user_routes import router
import os
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials

load_dotenv()

app = FastAPI(
    title="SmartLife API",
    description="Backend API for SmartLife Mobile App",
    version="1.0.0"
)

# Initialize Firebase Admin
# Make sure to place your serviceAccountKey.json in the SmartLife directory
service_account_path = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")
if os.path.exists(service_account_path):
    cred = credentials.Certificate(service_account_path)
    firebase_admin.initialize_app(cred)
    print("Firebase Admin initialized successfully")
else:
    print("WARNING: serviceAccountKey.json not found. Push notifications will be disabled.")

# CORS configuration for mobile app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)

@app.get("/health")
async def health_check():
    return {
        "status": "success",
        "message": "API is healthy",
        "data": {"service": "SmartLife API", "status": "online"}
    }
