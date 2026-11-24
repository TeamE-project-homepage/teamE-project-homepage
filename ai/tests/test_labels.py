import sys
from pathlib import Path

import pytest

from app.labels import ITEM_PROMPTS_PATH, LABELS, best_label, load_items

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
import sync_item_prompts  # noqa: E402


def test_every_item_has_a_known_label_and_prompts() -> None:
    items = load_items()

    assert items
    assert {item.label for item in items} == set(LABELS)
    assert all(item.prompts and all(item.prompts) for item in items)
    assert len({item.item_key for item in items}) == len(items)


@pytest.mark.skipif(not sync_item_prompts.SEED_ITEMS.exists(),
                    reason="seed data is outside the docker build context")
def test_item_prompts_match_seed_items() -> None:
    assert ITEM_PROMPTS_PATH.read_text(encoding="utf-8") == sync_item_prompts.render(), (
        "app/item_prompts.csv is out of date with docs/data/items.csv; "
        "run `python ai/scripts/sync_item_prompts.py` and commit the result"
    )


def test_best_label_sums_item_probabilities_per_label() -> None:
    # A single "glass" item wins over each plastic item, but plastic wins in total.
    label, score = best_label(["plastic", "plastic", "glass"], [0.3, 0.3, 0.4])

    assert label == "plastic"
    assert score == pytest.approx(0.6)
