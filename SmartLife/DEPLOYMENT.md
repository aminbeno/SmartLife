# SmartLife API - Production Deployment Guide

## 🔒 Pre-Deployment Checklist

### Security

- [ ] Update `.env` with production MongoDB URL
- [ ] Change `ALLOWED_ORIGINS` in `.env` to specific domain
- [ ] Set `DEBUG=False` in `.env`
- [ ] Add JWT authentication for API endpoints
- [ ] Enable HTTPS/SSL certificate
- [ ] Set strong database credentials
- [ ] Use environment variables instead of .env file
- [ ] Add rate limiting to prevent abuse
- [ ] Add API key authentication if needed

### Database

- [ ] Set up MongoDB Atlas or self-hosted MongoDB with authentication
- [ ] Create unique index on `uid` field:
  ```python
  await users_collection.create_index("uid", unique=True)
  await users_collection.create_index("email", unique=True)  # Optional
  ```
- [ ] Set up database backups
- [ ] Configure MongoDB replication for high availability
- [ ] Enable MongoDB authentication

### Monitoring & Logging

- [ ] Set up application logging
- [ ] Set up error tracking (e.g., Sentry)
- [ ] Set up application performance monitoring (APM)
- [ ] Set up health check monitoring
- [ ] Configure log rotation

## 🚀 Deployment Options

### Option 1: Docker Deployment (Recommended)

**Create Dockerfile:**

```dockerfile
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Build and run:**

```bash
docker build -t smartlife-api .
docker run -p 8000:8000 --env-file .env smartlife-api
```

### Option 2: Gunicorn + Uvicorn (Traditional VPS)

**Install Gunicorn:**

```bash
pip install gunicorn
```

**Run with Gunicorn:**

```bash
gunicorn main:app \
  --workers 4 \
  --worker-class uvicorn.workers.UvicornWorker \
  --bind 0.0.0.0:8000 \
  --access-logfile - \
  --error-logfile -
```

### Option 3: AWS/Cloud Deployment

**AWS Elastic Beanstalk:**

```bash
eb init -p python-3.12 smartlife-api
eb create production
eb deploy
```

**Azure App Service:**

```bash
az webapp create --resource-group myResourceGroup \
  --plan myAppServicePlan \
  --name smartlife-api \
  --runtime "PYTHON|3.12"
```

**Google Cloud Run:**

```bash
gcloud run deploy smartlife-api --source .
```

## 🔧 Production Environment Setup

### Update main.py for Production

```python
import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes.user_routes import router
from dotenv import load_dotenv

load_dotenv()

DEBUG = os.getenv("DEBUG", "False").lower() == "true"
ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "").split(",")

app = FastAPI(
    title="SmartLife API",
    description="Backend API for SmartLife Mobile App",
    version="1.0.0",
    docs_url="/docs" if DEBUG else None,  # Disable docs in production
    redoc_url="/redoc" if DEBUG else None
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["*"],
)

app.include_router(router)

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "SmartLife API", "environment": "production" if not DEBUG else "development"}
```

### Production .env Template

```env
# Production Environment
DEBUG=False

# MongoDB Configuration
MONGODB_URL=mongodb+srv://username:password@cluster.mongodb.net/?retryWrites=true&w=majority
DATABASE_NAME=SmartLifeDB

# API Configuration
API_HOST=0.0.0.0
API_PORT=8000

# CORS Configuration - Update to your domain
ALLOWED_ORIGINS=https://yourmobileapp.com,https://www.yourmobileapp.com

# Logging
LOG_LEVEL=INFO
```

## 📊 Monitoring & Maintenance

### Set Up Logging

```python
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
```

### Monitor Performance

- Response times
- Error rates
- Database query performance
- API uptime

### Regular Maintenance

- Weekly: Check logs for errors
- Monthly: Review API usage patterns
- Quarterly: Update dependencies
- Yearly: Security audit

## 🔐 Security Measures

### 1. Add Authentication

```python
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthCredentials
import jwt

security = HTTPBearer()

async def verify_token(credentials: HTTPAuthCredentials = Depends(security)):
    try:
        payload = jwt.decode(
            credentials.credentials,
            os.getenv("SECRET_KEY"),
            algorithms=["HS256"]
        )
        return payload
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

# Use in routes:
@router.get("/protected")
async def protected_route(token = Depends(verify_token)):
    return {"message": "This is protected"}
```

### 2. Add Rate Limiting

```python
from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter

@router.post("/register_user")
@limiter.limit("5/minute")
async def register_user(request: Request, user: UserData):
    # Implementation
```

### 3. Input Validation & Sanitization

- Already implemented with Pydantic models
- Add custom validators as needed

### 4. HTTPS/SSL

- Use Let's Encrypt for free certificates
- Configure reverse proxy (Nginx) for SSL termination
- Enforce HTTPS redirect

## 🚨 Error Handling for Production

### Implement Structured Logging

```python
import json
from datetime import datetime

async def log_error(error: Exception, context: dict):
    log_entry = {
        "timestamp": datetime.utcnow().isoformat(),
        "error": str(error),
        "type": type(error).__name__,
        "context": context
    }
    print(json.dumps(log_entry))
```

## 📈 Scaling Considerations

### Database Scaling

- Use MongoDB Atlas for auto-scaling
- Implement connection pooling
- Add read replicas for high traffic

### Application Scaling

- Use load balancer (Nginx/HAProxy)
- Deploy multiple instances
- Implement session management

### Caching

- Add Redis for caching user data
- Cache API responses (if applicable)

## 🎯 Post-Deployment

1. Verify all endpoints are accessible
2. Test with load testing tools (Apache JMeter, Locust)
3. Set up monitoring alerts
4. Document deployment details
5. Create runbooks for common issues
6. Set up on-call rotation

## 📞 Support & Troubleshooting

### Common Issues

**High Response Times**

- Check MongoDB performance
- Verify network connectivity
- Review API logs
- Consider caching

**Database Errors**

- Check connection string
- Verify credentials
- Check MongoDB status
- Review logs

**CORS Issues**

- Verify `ALLOWED_ORIGINS` configuration
- Check browser console for errors
- Test with different origins

---

**Last Updated:** May 4, 2026  
**Status:** Ready for Production Deployment ✅
