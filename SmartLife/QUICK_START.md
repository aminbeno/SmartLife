# SmartLife Backend - Quick Start Guide

## 🚀 Running the API

### Start the Server

```bash
cd c:\Users\dell\Desktop\SmartLife
.venv\Scripts\activate  # If not already activated
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
| POST   | `/api/register_user`     | Create new user         |
| GET    | `/api/get_user/{uid}`    | Retrieve user data      |
| PUT    | `/api/update_user/{uid}` | Update user information |
| DELETE | `/api/delete_user/{uid}` | Delete user             |

## 📱 For Your Kotlin Mobile App

### Base URL

```kotlin
const val API_BASE_URL = "http://your-server-ip:8000/"
```

### Retrofit Interface Example

```kotlin
interface SmartLifeApiService {
    @POST("api/register_user")
    suspend fun registerUser(@Body user: UserData): Response<SuccessResponse>

    @GET("api/get_user/{uid}")
    suspend fun getUser(@Path("uid") uid: String): Response<UserResponse>

    @PUT("api/update_user/{uid}")
    suspend fun updateUser(
        @Path("uid") uid: String,
        @Body user: UserData
    ): Response<SuccessResponse>

    @DELETE("api/delete_user/{uid}")
    suspend fun deleteUser(@Path("uid") uid: String): Response<SuccessResponse>
}
```

### Data Classes for Kotlin

```kotlin
data class UserData(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String
)

data class SuccessResponse(
    val status: String,
    val message: String,
    val data: Map<String, Any>? = null
)

data class UserResponse(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val _id: String? = null
)
```

## 🔧 Configuration

### .env File

Located at: `c:\Users\dell\Desktop\SmartLife\.env`

Key settings:

- `MONGODB_URL` - MongoDB connection string
- `DATABASE_NAME` - Database name
- `API_PORT` - Server port (default: 8000)

## 📁 Project Structure

```
SmartLife/
├── main.py              # FastAPI app setup
├── models.py            # Data models (Pydantic)
├── database.py          # MongoDB connection
├── routes/
│   ├── __init__.py
│   └── user_routes.py   # User endpoints
├── requirements.txt     # Python dependencies
├── .env                 # Configuration
├── .venv/              # Virtual environment
├── README.md           # Full documentation
└── TEST_RESULTS.md     # Test results
```

## ✅ Status

✅ API fully functional  
✅ All CRUD operations working  
✅ Error handling implemented  
✅ MongoDB integration verified  
✅ CORS enabled for mobile access  
✅ Ready for Kotlin app integration

## 🐛 Troubleshooting

**MongoDB Connection Error?**

- Ensure MongoDB is running: `mongod`
- Check `MONGODB_URL` in `.env`

**Port 8000 already in use?**

- Change `API_PORT` in `.env` or use:
  ```bash
  uvicorn main:app --port 8001
  ```

**Module not found errors?**

- Reinstall dependencies:
  ```bash
  pip install -r requirements.txt
  ```

## 📞 Next Steps

1. ✅ Set up MongoDB (if not already done)
2. ✅ Configure `.env` for your environment
3. ✅ Start the API server
4. ✅ Connect from your Kotlin mobile app
5. ✅ Deploy to production with proper security measures

---

**Created:** May 4, 2026  
**Backend Status:** Production Ready ✅
