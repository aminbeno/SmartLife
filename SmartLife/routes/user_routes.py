from fastapi import APIRouter, HTTPException, status
from models import UserData, UserResponse, SuccessResponse, ErrorResponse
from database import users_collection
from pymongo.errors import DuplicateKeyError
from bson import ObjectId

router = APIRouter(prefix="/api", tags=["users"])

@router.post(
    "/register_user",
    response_model=SuccessResponse,
    status_code=status.HTTP_201_CREATED,
    responses={
        400: {"model": ErrorResponse},
        500: {"model": ErrorResponse}
    }
)
async def register_user(user: UserData):
    """
    Register a new user.
    
    - **uid**: Unique user identifier
    - **email**: User email address
    - **firstName**: User's first name
    - **lastName**: User's last name
    - **birthDate**: User's birth date (YYYY-MM-DD)
    """
    try:
        # Check if user already exists
        existing_user = await users_collection.find_one({"uid": user.uid})
        if existing_user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="User with this UID already exists"
            )
        
        user_dict = user.model_dump()
        result = await users_collection.insert_one(user_dict)
        
        return {
            "status": "success",
            "message": "User registered successfully",
            "data": {"id": str(result.inserted_id)}
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Registration error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to register user"
        )

@router.get(
    "/get_user/{uid}",
    response_model=UserResponse,
    responses={
        404: {"model": ErrorResponse},
        500: {"model": ErrorResponse}
    }
)
async def get_user(uid: str):
    """
    Retrieve a user by UID.
    
    - **uid**: Unique user identifier
    """
    try:
        user = await users_collection.find_one({"uid": uid})
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        user["_id"] = str(user["_id"])
        return user
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve user"
        )

@router.put(
    "/update_user/{uid}",
    response_model=SuccessResponse,
    status_code=status.HTTP_200_OK,
    responses={
        404: {"model": ErrorResponse},
        500: {"model": ErrorResponse}
    }
)
async def update_user(uid: str, user: UserData):
    """
    Update user information.
    
    - **uid**: Unique user identifier
    """
    try:
        result = await users_collection.update_one(
            {"uid": uid},
            {"$set": user.model_dump()}
        )
        
        if result.matched_count == 0:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        return {
            "status": "success",
            "message": "User updated successfully"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update user"
        )

@router.delete(
    "/delete_user/{uid}",
    response_model=SuccessResponse,
    status_code=status.HTTP_200_OK,
    responses={
        404: {"model": ErrorResponse},
        500: {"model": ErrorResponse}
    }
)
async def delete_user(uid: str):
    """
    Delete a user by UID.
    
    - **uid**: Unique user identifier
    """
    try:
        result = await users_collection.delete_one({"uid": uid})
        
        if result.deleted_count == 0:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        return {
            "status": "success",
            "message": "User deleted successfully"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to delete user"
        )
