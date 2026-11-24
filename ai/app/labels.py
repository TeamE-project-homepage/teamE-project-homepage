import csv
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

ITEM_PROMPTS_PATH = Path(__file__).with_name("item_prompts.csv")

# Category labels returned by /predict, one per category in docs/data/categories.csv.
LABELS: tuple[str, ...] = (
    "plastic",
    "vinyl",
    "paper",
    "glass",
    "can",
    "appliance",
    "furniture",
    "clothing",
    "food",
    "large_waste",
)


@dataclass(frozen=True)
class ItemPrompt:
    item_key: str
    label: str
    prompts: tuple[str, ...]


def load_items(path: Path = ITEM_PROMPTS_PATH) -> list[ItemPrompt]:
    """Reads item_prompts.csv (a copy of docs/data/items.csv) into one prompt set per item.

    Each item is described by its clip_prompt plus two sentences built from its name; the
    classifier averages their text embeddings.
    """
    with path.open(encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file))
    items = []
    for row in rows:
        name = row["name"].lower()
        prompts = (row["clip_prompt"], f"a photo of {name}", f"a photo of {name} as household waste")
        items.append(ItemPrompt(row["item_key"], row["label"], prompts))
    return items


def best_label(item_labels: list[str], probs: list[float]) -> tuple[str, float]:
    """Sums item probabilities per category label and returns the top label with its total."""
    totals: dict[str, float] = defaultdict(float)
    for label, prob in zip(item_labels, probs):
        totals[label] += prob
    label = max(totals, key=totals.__getitem__)
    return label, totals[label]
