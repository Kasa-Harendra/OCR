from fastapi import FastAPI, Header, Query, status
from fastapi.responses import JSONResponse

from routes.chat import router as chat_router

app: FastAPI = FastAPI(
    title="OCR Project",
    description="API for OCR application",
    root_path='/api/v1',
)

app.include_router(chat_router, tags=["Chats"], include_in_schema=True)

# Base Route
@app.get('/', status_code= status.HTTP_200_OK, response_model=dict)
def home() -> dict:
    try:
        return JSONResponse(
            status_code=status.HTTP_200_OK,
            content={
                "message": "Server Started Successfully"
            }
        )
    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                'error': str(e)
            }
        )
