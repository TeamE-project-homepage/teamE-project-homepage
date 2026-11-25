import argparse
import csv
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from PIL import Image

from app.labels import CATEGORY_LABELS, LABELS

IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
THRESHOLDS = [round(0.2 + 0.05 * i, 2) for i in range(13)]


def collect(photos: Path) -> list[tuple[str, Path]]:
    samples = []
    for folder in sorted(p for p in photos.iterdir() if p.is_dir()):
        for path in sorted(folder.iterdir()):
            if path.suffix.lower() in IMAGE_SUFFIXES:
                samples.append((folder.name, path))
    return samples


def load_item_labels(items: Path) -> dict[str, str]:
    with items.open(encoding="utf-8-sig", newline="") as file:
        return {row["item_key"]: CATEGORY_LABELS[row["category_code"]] for row in csv.DictReader(file)}


def percent(part: int, total: int) -> str:
    return f"{100 * part / total:5.1f}%" if total else "    -"


def main() -> None:
    parser = argparse.ArgumentParser(description="Top-1 accuracy of the classifier on labeled photos")
    parser.add_argument("--photos", required=True, type=Path,
                        help="folder with one sub-folder per expected label or per item key")
    parser.add_argument("--items", type=Path,
                        help="items.csv used to map item-key folders to labels through their category")
    args = parser.parse_args()

    samples = collect(args.photos)
    if not samples:
        sys.exit(f"no images found under {args.photos}")

    item_labels = load_item_labels(args.items) if args.items else {}
    labels = set(LABELS)
    unknown = sorted({folder for folder, _ in samples if folder not in item_labels and folder not in labels})
    if unknown:
        sys.exit(f"folders are neither labels nor known item keys: {', '.join(unknown)}")

    from app.classifier import Classifier

    classifier = Classifier()
    results = []
    per_folder = defaultdict(list)
    for folder, path in samples:
        expected = item_labels.get(folder, folder)
        label, score = classifier.predict(Image.open(path))
        results.append((expected, label, score))
        per_folder[folder].append((expected, label, score))

    correct = sum(expected == label for expected, label, _ in results)
    print(f"model: {classifier.name}")
    print(f"overall: {correct}/{len(results)} ({percent(correct, len(results))})")

    per_label = defaultdict(lambda: [0, 0])
    for expected, label, _ in results:
        per_label[expected][1] += 1
        per_label[expected][0] += expected == label
    print("\nper label")
    for name in sorted(per_label):
        hit, total = per_label[name]
        print(f"  {name:<14}{hit:>4}/{total:<4}{percent(hit, total)}")

    if item_labels:
        print("\nper item (expected label, hits, predictions)")
        for folder in sorted(per_folder):
            rows = per_folder[folder]
            hit = sum(expected == label for expected, label, _ in rows)
            guesses = ", ".join(f"{label} {score:.2f}" for _, label, score in rows)
            print(f"  {folder:<24}{rows[0][0]:<9}{hit}/{len(rows)}  {guesses}")

    print("\nthreshold  coverage  accuracy")
    for threshold in THRESHOLDS:
        kept = [(expected, label) for expected, label, score in results if score >= threshold]
        hit = sum(expected == label for expected, label in kept)
        print(f"  {threshold:.2f}     {percent(len(kept), len(results))}   {percent(hit, len(kept))}")


if __name__ == "__main__":
    main()
