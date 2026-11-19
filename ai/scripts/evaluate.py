import argparse
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from PIL import Image

IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
THRESHOLDS = [round(0.2 + 0.05 * i, 2) for i in range(13)]


def collect(photos: Path) -> list[tuple[str, Path]]:
    samples = []
    for folder in sorted(p for p in photos.iterdir() if p.is_dir()):
        for path in sorted(folder.iterdir()):
            if path.suffix.lower() in IMAGE_SUFFIXES:
                samples.append((folder.name, path))
    return samples


def percent(part: int, total: int) -> str:
    return f"{100 * part / total:5.1f}%" if total else "    -"


def main() -> None:
    parser = argparse.ArgumentParser(description="Top-1 accuracy of the classifier on labeled photos")
    parser.add_argument("--photos", required=True, type=Path, help="folder with one sub-folder per expected label")
    args = parser.parse_args()

    samples = collect(args.photos)
    if not samples:
        sys.exit(f"no images found under {args.photos}")

    from app.classifier import Classifier

    classifier = Classifier()
    results = []
    for expected, path in samples:
        label, score = classifier.predict(Image.open(path))
        results.append((expected, label, score))

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

    print("\nthreshold  coverage  accuracy")
    for threshold in THRESHOLDS:
        kept = [(expected, label) for expected, label, score in results if score >= threshold]
        hit = sum(expected == label for expected, label in kept)
        print(f"  {threshold:.2f}     {percent(len(kept), len(results))}   {percent(hit, len(kept))}")


if __name__ == "__main__":
    main()
