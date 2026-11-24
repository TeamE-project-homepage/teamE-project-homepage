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

`label` is one of the 10 category labels in `app/labels.py`. The classifier compares the photo with a prompt set for every item in `app/item_prompts.csv` (the item's `clip_prompt` plus two sentences built from its name), then sums the item probabilities per category; `score` is that sum.

`app/item_prompts.csv` is a copy of `docs/data/items.csv` (item key, category label, name, clip_prompt), because the Docker build only sees `ai/`. Update it when items change; `tests/test_labels.py` fails when the two differ.

`GET /health` returns `{"status": "UP"}`.

## Accuracy evaluation

Put labeled photos in sub-folders named after the expected label (`photos/plastic/*.jpg`) or after an item key (`photos/paper_cup/*.jpg`), then run from `ai/`:

```bash
python scripts/evaluate.py --photos ./photos --items ../docs/data/items.csv
```

`--items` maps item-key folders to their category label. It prints overall, per-label and per-item top-1 accuracy, plus coverage (share of photos at or above the threshold) and accuracy (correct share among them) for confidence thresholds from 0.2 to 0.8. The model weights are downloaded on the first run.

### Results (2026-11-24)

158 Wikimedia Commons photos of 20 seed items (6–8 per item), split per item into a dev set (99) and a held-out test set (59). Prompts, model and threshold were chosen on the dev set only; the test set was scored once at the end.

| Setup (ViT-B-32/laion2b_s34b_b79k) | dev top-1 | test top-1 | test top-3 |
| --- | --- | --- | --- |
| 10 category prompts (previous) | 60.6% | 67.8% | 84.7% |
| 88 item prompt sets, summed per category (current) | 83.8% | 81.4% | 96.6% |

Threshold (current setup, test set):

| threshold | coverage | accuracy |
| --- | --- | --- |
| 0.4 | 96.6% | 84.2% |
| **0.5** | **94.9%** | **85.7%** |
| 0.6 | 91.5% | 87.0% |
| 0.7 | 89.8% | 88.7% |

- **Threshold 0.5** (backend `RECOGNITION_MIN_CONFIDENCE`): on the dev set accuracy stops improving above 0.5 (86.5% at 0.5, 87.8% at 0.8) while coverage keeps dropping (89.9% → 82.8%).
- Hand-written extra prompts for confused items lowered dev accuracy (83.8% → 78.8%), so only the generated name sentences are used.
- Other models on the dev set with item prompts: ViT-B-16/laion2b_s34b_b88k 76.8%, ViT-L-14/laion2b_s32b_b82k 85.9%. ViT-L-14 is 2 photos better but about 3× larger, so ViT-B-32 stays.
- Weak spots: paper cups vs instant noodle cups, plastic food containers, broken glass. Some wrong answers have scores near 1.0, so the threshold cannot filter them; show several candidates and let the user confirm.
- Limits: few photos per item, clean stock photos rather than phone photos, and only 20 of 88 items covered. Treat the numbers as rough and re-run with real phone photos when available.

## Tests

```bash
pip install -r requirements-dev.txt
pytest
```

Tests use a fake classifier, so the model is not loaded.
