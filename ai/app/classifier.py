import open_clip
import torch
from PIL import Image

from app.labels import best_label, load_items

MODEL_NAME = "ViT-B-32"
PRETRAINED = "laion2b_s34b_b79k"


class Classifier:
    """Zero-shot classifier that scores every item prompt set, then sums the scores per category."""

    def __init__(self) -> None:
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model, _, self.preprocess = open_clip.create_model_and_transforms(
            MODEL_NAME, pretrained=PRETRAINED, device=self.device
        )
        self.model.eval()
        tokenizer = open_clip.get_tokenizer(MODEL_NAME)
        items = load_items()
        self.item_labels = [item.label for item in items]
        with torch.no_grad():
            features = []
            for item in items:
                text = self.model.encode_text(tokenizer(list(item.prompts)).to(self.device))
                text = text / text.norm(dim=-1, keepdim=True)
                mean = text.mean(dim=0)
                features.append(mean / mean.norm())
            self.text_features = torch.stack(features)
            self.logit_scale = self.model.logit_scale.exp().item()

    @property
    def name(self) -> str:
        return f"{MODEL_NAME}/{PRETRAINED}"

    def predict(self, image: Image.Image) -> tuple[str, float]:
        tensor = self.preprocess(image.convert("RGB")).unsqueeze(0).to(self.device)
        with torch.no_grad():
            features = self.model.encode_image(tensor)
            features = features / features.norm(dim=-1, keepdim=True)
            probs = (self.logit_scale * features @ self.text_features.T).softmax(dim=-1)[0]
        label, score = best_label(self.item_labels, probs.tolist())
        return label, round(score, 4)
