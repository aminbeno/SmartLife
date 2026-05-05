# SmartLife Backend - Quick Start Guide

## 🚀 Running the API

### Start the Server

1. Navigate to the backend directory:
```powershell
cd C:\Users\dell\AndroidStudioProjects\SmartLife\SmartLife
```

2. Activate the virtual environment:
```powershell
# PowerShell
.\.venv\Scripts\Activate.ps1
# CMD
.venv\Scripts\activate
```

3. Run the server:
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at: **http://localhost:8000**

### Access Documentation

- **Swagger UI:** http://localhost:8000/docs
- **ReDoc:** http://localhost:8000/redoc

## 📋 Available Endpoints

| Method | Endpoint                 | Purpose                 |
| ------ | ------------------------ | ----------------------- |
| GET    | `/health`                | Check API status        |
| POST   | `/api/register_user`     | Create new user in DB   |
| GET    | `/api/get_user/{uid}`    | Retrieve user data      |
| PUT    | `/api/update_user/{uid}` | Update user information |
| DELETE | `/api/delete_user/{uid}` | Delete user             |

## 📱 For Your Kotlin Mobile App

### Hybrid Authentication Flow
The app uses a dual-registration approach:
1. **Firebase Auth**: Manages credentials (Email/Password) and provides a unique `uid`.
2. **FastAPI Backend**: Stores profile data (Name, BirthDate) linked by the Firebase `uid`.

### Base URL (RetrofitClient.kt)
```kotlin
// In RetrofitClient.kt
private const val BASE_URL = "http://10.0.2.2:8000/" // For Android Emulator
```

## 🔧 Configuration

### .env File
Located at: `C:\Users\dell\AndroidStudioProjects\SmartLife\SmartLife\.env`

Key settings:
- `MONGODB_URL` - MongoDB connection string
- `DATABASE_NAME` - Database name (e.g., smartlife_db)

## 📁 Project Structure
```
SmartLife/ (Root)
├── app/                  # Android Project (Kotlin)
└── SmartLife/ (Backend)  # FastAPI Backend
    ├── main.py           # FastAPI entry point
    ├── models.py         # Pydantic models
    ├── routes/           # API routes
    ├── .env              # Backend config
    └── .venv/            # Python Virtual Env
```

## 🐛 Troubleshooting

**Fatal error in launcher (venv)?**
If you moved the project, recréate the venv in PowerShell:
```powershell
Remove-Item -Recurse -Force .venv
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

---
**Last Updated:** May 2026
**Status:** Integrated with Firebase Auth ✅