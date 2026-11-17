from contextlib import asynccontextmanager
from io import BytesIO

from fastapi import FastAPI, File, HTTPException, Request, UploadFile
from PIL import Image, UnidentifiedImageError

from app.classifier import Classifier
from app.schemas import PredictResponse

MAX_IMAGE_BYTES = 10 * 1024 * 1024


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.classifier = Classifier()
    yield


app = FastAPI(title="BinGo Recognition", lifespan=lifespan)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/predict", response_model=PredictResponse)
async def predict(request: Request, file: UploadFile = File(...)) -> PredictResponse:
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image.")

    data = await file.read()
    if len(data) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="Image must be 10MB or smaller.")

    try:
        image = Image.open(BytesIO(data))
        image.load()
    except UnidentifiedImageError:
        raise HTTPException(status_code=400, detail="Could not read the image.")

    classifier: Classifier = request.app.state.classifier
    label, score = classifier.predict(image)
    return PredictResponse(model=classifier.name, label=label, score=score)
