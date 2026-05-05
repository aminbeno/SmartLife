# SmartLife API - Mobile Backend

Backend API for the SmartLife mobile application, built with FastAPI and MongoDB. It works in tandem with Firebase Authentication for a secure, hybrid user management system.

## 🌟 Key Features

- **Hybrid Auth**: Integrates seamlessly with Firebase Authentication.
- **FastAPI**: High-performance, asynchronous Python framework.
- **MongoDB**: Flexible NoSQL storage for user profiles and application data.
- **CORS Support**: Pre-configured for mobile app access.
- **Auto-Docs**: Interactive Swagger UI and ReDoc documentation.

## 🛠️ Tech Stack

- **Backend**: Python 3.12+, FastAPI, Motor (Async MongoDB driver).
- **Authentication**: Firebase Auth (Client-side) + UID mapping (Server-side).
- **Database**: MongoDB.

## 🚀 Setup & Installation

### 1. Prerequisites
- Python 3.12 or higher.
- MongoDB instance (Local or Atlas).

### 2. Environment Configuration
Create or edit the `.env` file in the `SmartLife/` directory:
```env
MONGODB_URL=mongodb://localhost:27017
DATABASE_NAME=SmartLifeDB
API_HOST=0.0.0.0
API_PORT=8000
```

### 3. Install Dependencies
```powershell
# Create virtual environment
python -m venv .venv

# Activate (PowerShell)
.\.venv\Scripts\Activate.ps1

# Install requirements
pip install -r requirements.txt
```

## 🏃 Running the API
```powershell
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```
- **Swagger Docs**: [http://localhost:8000/docs](http://localhost:8000/docs)
- **Health Check**: [http://localhost:8000/health](http://localhost:8000/health)

## 📱 Integration with Android (Kotlin)

The mobile app follows this registration flow:
1. **Firebase**: App creates a user via `auth.createUserWithEmailAndPassword()`.
2. **Backend**: App sends the Firebase `uid` and profile details (First Name, Last Name, Birth Date) to `/api/register_user`.

### Important URLs
- **Emulator**: `http://10.0.2.2:8000/`
- **Real Device**: `http://<your-ip>:8000/`

## 📁 Project Structure
```
SmartLife/
├── app/                  # Android Project (Kotlin)
└── SmartLife/ (Backend)  # FastAPI Backend
    ├── main.py           # API Entry Point
    ├── models.py         # Pydantic Schemas
    ├── routes/           # API Endpoints
    ├── database.py       # MongoDB Connection
    └── .env              # Configuration
```

## 📘 Documentation
- [Quick Start Guide](QUICK_START.md) - For rapid setup.
- [Kotlin Integration Guide](KOTLIN_INTEGRATION_GUIDE.md) - For mobile developers.
- [Deployment Guide](DEPLOYMENT.md) - For production setup.

---
**Status:** Active Development 🚀
**Auth:** Firebase + MongoDB Hybrid ✅