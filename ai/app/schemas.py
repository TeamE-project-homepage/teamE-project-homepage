from pydantic import BaseModel


class PredictResponse(BaseModel):
    model: str
    label: str
    score: float
