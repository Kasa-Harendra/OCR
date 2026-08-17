from datetime import datetime
import uuid
from pydantic import BaseModel, Field
from typing import * 

class ChatBaseModel(BaseModel):
    title: str = Field(..., description="Title for the Chat", max_length=30, min_length=1) 

class GetChatsResponseModel(ChatBaseModel):
    chat_id: uuid.UUID = Field(..., description="Unique ID for the Chat")
    created_at: datetime = Field(..., description="Date and time of chat creation")

class GetChatResponseModel(GetChatsResponseModel):
    messages: List[Dict[str, str]] = Field(default=[], description="Messages in the chat")

class CreateChatRequestModel(ChatBaseModel):
    ...

class UpdateChatRequestModel(ChatBaseModel):
    ...
