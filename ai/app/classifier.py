import open_clip
import torch
from PIL import Image

from app.labels import LABELS

MODEL_NAME = "ViT-B-32"
PRETRAINED = "laion2b_s34b_b79k"


class Classifier:
    def __init__(self) -> None:
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model, _, self.preprocess = open_clip.create_model_and_transforms(
            MODEL_NAME, pretrained=PRETRAINED, device=self.device
        )
        self.model.eval()
        tokenizer = open_clip.get_tokenizer(MODEL_NAME)
        self.labels = list(LABELS.keys())
        with torch.no_grad():
            text = tokenizer(list(LABELS.values())).to(self.device)
            features = self.model.encode_text(text)
            self.text_features = features / features.norm(dim=-1, keepdim=True)

    @property
    def name(self) -> str:
        return f"{MODEL_NAME}/{PRETRAINED}"

    def predict(self, image: Image.Image) -> tuple[str, float]:
        tensor = self.preprocess(image.convert("RGB")).unsqueeze(0).to(self.device)
        with torch.no_grad():
            features = self.model.encode_image(tensor)
            features = features / features.norm(dim=-1, keepdim=True)
            probs = (100.0 * features @ self.text_features.T).softmax(dim=-1)[0]
        score, index = probs.max(dim=0)
        return self.labels[index.item()], round(score.item(), 4)
