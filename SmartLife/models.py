from pydantic import BaseModel, EmailStr, Field
from typing import Optional

class UserData(BaseModel):
    uid: str = Field(..., min_length=1, description="Unique user ID")
    email: EmailStr = Field(..., description="User email address")
    firstName: str = Field(..., min_length=1, description="First name")
    lastName: str = Field(..., min_length=1, description="Last name")
    birthDate: str = Field(..., description="Birth date (YYYY-MM-DD format)")

    class Config:
        json_schema_extra = {
            "example": {
                "uid": "user123",
                "email": "user@example.com",
                "firstName": "John",
                "lastName": "Doe",
                "birthDate": "1990-01-15"
            }
        }

class UserResponse(BaseModel):
    uid: str
    email: EmailStr
    firstName: str
    lastName: str
    birthDate: str
    _id: Optional[str] = None

class SuccessResponse(BaseModel):
    status: str = "success"
    message: str
    data: Optional[dict] = None

class ErrorResponse(BaseModel):
    status: str = "error"
    message: str
    code: Optional[str] = None
