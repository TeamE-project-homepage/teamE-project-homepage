"""Regenerates app/item_prompts.csv from docs/data/items.csv.

The Docker build only sees ai/, so the classifier reads this generated copy instead of the seed file.
Run it from anywhere after editing docs/data/items.csv, then commit both files:

    python ai/scripts/sync_item_prompts.py
"""
import csv
import io
import sys
from pathlib import Path

AI_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(AI_DIR))

from app.labels import CATEGORY_LABELS, ITEM_PROMPTS_PATH  # noqa: E402

SEED_ITEMS = AI_DIR.parent / "docs" / "data" / "items.csv"
COLUMNS = ("item_key", "label", "name", "clip_prompt")


def render(seed: Path = SEED_ITEMS) -> str:
    with seed.open(encoding="utf-8-sig", newline="") as file:
        rows = list(csv.DictReader(file))
    out = io.StringIO()
    writer = csv.writer(out, lineterminator="\n")
    writer.writerow(COLUMNS)
    for row in rows:
        writer.writerow((row["item_key"], CATEGORY_LABELS[row["category_code"]], row["name"], row["clip_prompt"]))
    return out.getvalue()


def main() -> None:
    content = render()
    if ITEM_PROMPTS_PATH.exists() and ITEM_PROMPTS_PATH.read_text(encoding="utf-8") == content:
        print(f"{ITEM_PROMPTS_PATH.name} is up to date")
        return
    ITEM_PROMPTS_PATH.write_text(content, encoding="utf-8", newline="")
    print(f"wrote {ITEM_PROMPTS_PATH} ({content.count(chr(10)) - 1} items)")


if __name__ == "__main__":
    main()
