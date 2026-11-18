from io import BytesIO

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.main import app


class FakeClassifier:
    name = "fake"

    def predict(self, image: Image.Image) -> tuple[str, float]:
        return "plastic", 0.9


@pytest.fixture
def client() -> TestClient:
    # Not used as a context manager, so the lifespan (real model load) is skipped.
    app.state.classifier = FakeClassifier()
    return TestClient(app)


def png_bytes() -> bytes:
    buffer = BytesIO()
    Image.new("RGB", (32, 32), "red").save(buffer, format="PNG")
    return buffer.getvalue()


def test_predict_valid_image(client: TestClient) -> None:
    response = client.post("/predict", files={"file": ("ok.png", png_bytes(), "image/png")})

    assert response.status_code == 200
    assert response.json() == {"model": "fake", "label": "plastic", "score": 0.9}


def test_predict_unidentified_image_returns_400(client: TestClient) -> None:
    response = client.post("/predict", files={"file": ("bad.png", b"not an image", "image/png")})

    assert response.status_code == 400


def test_predict_truncated_image_returns_400(client: TestClient) -> None:
    # Header is valid so Image.open() succeeds, but image.load() raises OSError.
    truncated = png_bytes()[:-30]
    with Image.open(BytesIO(truncated)):
        pass

    response = client.post("/predict", files={"file": ("broken.png", truncated, "image/png")})

    assert response.status_code == 400
    assert response.json()["detail"] == "Could not read the image."
