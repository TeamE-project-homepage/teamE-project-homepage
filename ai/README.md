# BinGo Recognition Server

FastAPI server that classifies waste photos with OpenCLIP (zero-shot).

## Run locally

```bash
docker build -t bingo-ai .
docker run -p 8080:8080 bingo-ai
```

The model weights are downloaded during the image build.

## Deploy (Google Cloud Run)

```bash
gcloud run deploy bingo-ai \
  --source . \
  --region asia-northeast3 \
  --memory 2Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 1 \
  --allow-unauthenticated
```

Run it from the `ai/` folder. The service scales to zero when idle, so the first request after a while is slow.

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

## Tests

```bash
pip install -r requirements-dev.txt
pytest
```

Tests use a fake classifier, so the model is not loaded.
