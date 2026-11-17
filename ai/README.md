# BinGo Recognition Server

FastAPI server that classifies waste photos with OpenCLIP (zero-shot).

## Run

```bash
docker build -t bingo-ai .
docker run -p 7860:7860 bingo-ai
```

The model weights are downloaded on first start.

## API

`POST /predict` (multipart, field `file`)

```json
{
  "model": "ViT-B-32/laion2b_s34b_b79k",
  "label": "plastic",
  "score": 0.8123
}
```

Labels are defined in `app/labels.py`.

`GET /health` returns `{"status": "UP"}`.
