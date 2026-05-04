# SmartLife API - Mobile Backend

Backend API for SmartLife mobile app (Kotlin).

## Features

✅ FastAPI framework for high performance  
✅ MongoDB database for flexible data storage  
✅ CORS enabled for mobile app access  
✅ Comprehensive error handling  
✅ Input validation with Pydantic  
✅ Async/await for non-blocking operations  
✅ Auto-generated API documentation  
✅ Health check endpoint

## Setup Instructions

### Prerequisites

- Python 3.8+
- MongoDB running locally or on a server
- Virtual environment

### Installation

1. **Create and activate virtual environment:**

```bash
python -m venv .venv
.venv\Scripts\activate  # Windows
# or
source .venv/bin/activate  # Linux/Mac
```

2. **Install dependencies:**

```bash
pip install -r requirements.txt
```

3. **Configure environment (.env file already exists):**
   Edit `.env` to match your setup:

```env
MONGODB_URL=mongodb://localhost:27017
DATABASE_NAME=SmartLifeDB
API_HOST=0.0.0.0
API_PORT=8000
```

4. **Ensure MongoDB is running:**

```bash
# If using local MongoDB
mongod

# Or update MONGODB_URL in .env for your MongoDB instance
```

### Running the API

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## API Endpoints

### Health Check

- **GET** `/health`  
  Check if the API is running

### User Management

#### Register User

- **POST** `/api/register_user`

```json
{
  "uid": "user123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "birthDate": "1990-01-15"
}
```

#### Get User

- **GET** `/api/get_user/{uid}`

#### Update User

- **PUT** `/api/update_user/{uid}`

```json
{
  "uid": "user123",
  "email": "newemail@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "birthDate": "1990-01-15"
}
```

#### Delete User

- **DELETE** `/api/delete_user/{uid}`

## API Documentation

Once running, visit:

- **Swagger UI:** http://localhost:8000/docs
- **ReDoc:** http://localhost:8000/redoc

## Kotlin Mobile App Integration

### Base URL

```kotlin
const val BASE_URL = "http://your-server-ip:8000/api/"
```

### Example with Retrofit

```kotlin
interface SmartLifeApiService {
    @POST("register_user")
    suspend fun registerUser(@Body user: UserData): Response<SuccessResponse>

    @GET("get_user/{uid}")
    suspend fun getUser(@Path("uid") uid: String): Response<UserResponse>

    @PUT("update_user/{uid}")
    suspend fun updateUser(@Path("uid") uid: String, @Body user: UserData): Response<SuccessResponse>

    @DELETE("delete_user/{uid}")
    suspend fun deleteUser(@Path("uid") uid: String): Response<SuccessResponse>
}
```

## Testing the API

### Using cURL

**Register User:**

```bash
curl -X POST "http://localhost:8000/api/register_user" \
  -H "Content-Type: application/json" \
  -d '{
    "uid": "user123",
    "email": "test@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "birthDate": "1990-01-15"
  }'
```

**Get User:**

```bash
curl -X GET "http://localhost:8000/api/get_user/user123"
```

**Update User:**

```bash
curl -X PUT "http://localhost:8000/api/update_user/user123" \
  -H "Content-Type: application/json" \
  -d '{
    "uid": "user123",
    "email": "newemail@example.com",
    "firstName": "Jane",
    "lastName": "Doe",
    "birthDate": "1990-01-15"
  }'
```

**Delete User:**

```bash
curl -X DELETE "http://localhost:8000/api/delete_user/user123"
```

## Production Deployment

For production:

1. **Update CORS origins in `.env`:**

   ```env
   ALLOWED_ORIGINS=["https://your-app.com"]
   ```

2. **Use environment variables for sensitive data**

3. **Set `DEBUG=False` in `.env`**

4. **Deploy with Gunicorn + Uvicorn:**

   ```bash
   pip install gunicorn
   gunicorn main:app --workers 4 --worker-class uvicorn.workers.UvicornWorker
   ```

5. **Use a reverse proxy (Nginx) for production**

6. **Enable HTTPS/SSL**

7. **Set up proper error logging and monitoring**

## File Structure

```
SmartLife/
├── main.py              # FastAPI application entry point
├── models.py            # Pydantic data models
├── database.py          # MongoDB connection
├── routes/
│   ├── __init__.py
│   └── user_routes.py   # User endpoints
├── requirements.txt     # Python dependencies
├── .env                 # Environment configuration
└── README.md           # This file
```

## Troubleshooting

**MongoDB Connection Error:**

- Ensure MongoDB is running
- Check MONGODB_URL in `.env`

**CORS Issues:**

- Verify `ALLOWED_ORIGINS` in `.env`
- For development, it's set to `["*"]`

**Port Already in Use:**

```bash
# Change API_PORT in .env or use:
uvicorn main:app --port 8001
```

## Support

For issues or questions, check the API documentation at http://localhost:8000/docs
