import csv
from pathlib import Path

import pytest

from app.labels import LABELS, best_label, load_items

SEED_ITEMS = Path(__file__).resolve().parents[2] / "docs" / "data" / "items.csv"
CATEGORY_LABELS = dict(zip(("PL", "VN", "PA", "GL", "ME", "AP", "FU", "CL", "FO", "BK"), LABELS))


def test_every_item_has_a_known_label_and_prompts() -> None:
    items = load_items()

    assert items
    assert {item.label for item in items} == set(LABELS)
    assert all(item.prompts and all(item.prompts) for item in items)
    assert len({item.item_key for item in items}) == len(items)


@pytest.mark.skipif(not SEED_ITEMS.exists(), reason="seed data is outside the docker build context")
def test_item_prompts_match_seed_items() -> None:
    with SEED_ITEMS.open(encoding="utf-8-sig", newline="") as file:
        seed = [(row["item_key"], CATEGORY_LABELS[row["category_code"]], row["name"], row["clip_prompt"])
                for row in csv.DictReader(file)]
    with (Path(__file__).resolve().parents[1] / "app" / "item_prompts.csv").open(encoding="utf-8") as file:
        copied = [(row["item_key"], row["label"], row["name"], row["clip_prompt"]) for row in csv.DictReader(file)]

    assert copied == seed


def test_best_label_sums_item_probabilities_per_label() -> None:
    # A single "glass" item wins over each plastic item, but plastic wins in total.
    label, score = best_label(["plastic", "plastic", "glass"], [0.3, 0.3, 0.4])

    assert label == "plastic"
    assert score == pytest.approx(0.6)
