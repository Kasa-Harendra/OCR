from fastapi.params import Path
from fastapi.responses import JSONResponse, Response
from typing import Literal, List, Dict
from fastapi.routing import APIRouter
from fastapi import status, Header, Query, Body
import uuid
from datetime import datetime

from db.data import chats 

from models.chat import (
    GetChatsResponseModel,
    GetChatResponseModel,
    CreateChatRequestModel,
    UpdateChatRequestModel,
)

router: APIRouter = APIRouter(
    prefix='/chats',
)

@router.get('/', status_code=status.HTTP_200_OK)
def get_chats(
    limit: int = Query(default=10, ge=1, le=10, description="Maximum number of chats to get"),
    offset: int = Query(default=0, ge=0, description="Number of items to skip for pagination."),
    sort: Literal["desc", "asc"] = Query(default="desc", description="Sort order for the chats."),
) -> List[GetChatsResponseModel] | Dict:
    # Returns list of chats
    try:
        return chats
    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={ "message": str(e) } 
        )

@router.get('/{chat_id}', status_code=status.HTTP_200_OK, response_model=GetChatResponseModel)
def get_chat(
    chat_id: str = Path(..., description="ID of the chat to get")
) -> GetChatResponseModel:
    try:
        for i in range(len(chats)):
            if chats[i]['chat_id'] == chat_id:
                return 
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content = { "message": "Chat not found" }
        )

    except Exception as e:
        return {
            "message": str(e)
        }

@router.post('/', status_code=status.HTTP_201_CREATED, response_model=GetChatResponseModel)
def create_chat(
    payload: CreateChatRequestModel
) -> GetChatResponseModel | Dict:
    # Create a chat
    try:
        chat_id = str(uuid.uuid4())
        chats.append({
            "chat_id": chat_id,
            "title": payload.title,
            "created_at": str(datetime.now()),
            "messages": []
        })
        return JSONResponse(
            status_code=status.HTTP_201_CREATED,
            content=chats[-1]
        )
    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={ "message": str(e) } 
        )

@router.patch('/{chat_id}', status_code=status.HTTP_200_OK, response_model=GetChatResponseModel)
def update_chat(
    payload: UpdateChatRequestModel,
    chat_id: str = Path(..., description="ID of the chat to update"),
) -> GetChatResponseModel | Dict[str, str]:
    # Returns list of chats
    try:
        for i in range(len(chats)):
            if chats[i]['chat_id'] == chat_id:
                chats[i]['title'] = payload.title
                return chats[i]
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={ "message": "Chat not found" } 
        )
    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={ "message": str(e) } 
        )

@router.delete('/{chat_id}')
def delete_chat(
    chat_id: str = Path(..., description="ID of the chat to be deleted")
) -> None | Dict[str, str]:
    try:
        for i in range(len(chats)):
            if chats[i]['chat_id'] == chat_id:
                chats.pop(i)
                return JSONResponse(
                    status_code=status.HTTP_204_NO_CONTENT
                )
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={ "message": "Chat not found" }
        )
    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "error": str(e)
            }
        )