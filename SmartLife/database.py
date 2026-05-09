from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase
from dotenv import load_dotenv
import os

load_dotenv()

MONGODB_URL = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
DATABASE_NAME = os.getenv("DATABASE_NAME", "SmartLifeDB")

client = AsyncIOMotorClient(MONGODB_URL)
db: AsyncIOMotorDatabase = client[DATABASE_NAME]

# Collections
users_collection = db["users"]
activities_collection = db["activities"]
habits_collection = db["habits"]
recommendations_collection = db["recommendations"]
voice_logs_collection = db["voice_logs"]
named_locations_collection = db["named_locations"]

async def get_database():
    """Get database connection"""
    return db

async def close_database():
    """Close database connection"""
    client.close()
