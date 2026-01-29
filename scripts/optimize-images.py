#!/usr/bin/env python3
"""
Multithreaded PNG optimizer using oxipng and fzf

features:
- walk the repository and find PNG files, excluding directories named `build`
- present files via `fzf` (supports multiple selection with `-m`)
- run `oxipng -o 4 --strip safe --alpha {file}` on each selected file in parallel using threads

usage examples:
  python scripts/optimize-images.py                     # interactive via fzf
  python scripts/optimize-images.py --all               # optimize all found PNGs without fzf
  python scripts/optimize-images.py -j 8 --dry-run      # show commands without running

requirements:
- fzf
- oxipng
"""

from __future__ import annotations

import argparse
import concurrent.futures
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List, Tuple


def find_png_files(root: Path) -> List[Tuple[Path, int]]:
    """Recursively find .png files excluding dirs: build, .git, __pycache__, node_modules."""
    pngs: List[Tuple[Path, int]] = []
    for dirpath, dirs, files in os.walk(root):
        # Exclude common build dirs
        dirs[:] = [
            d for d in dirs if d not in ("build", ".git", "__pycache__", "node_modules")
        ]
        for f in files:
            if (
                (f.lower().endswith(".png"))
                or (f.lower().endswith(".jpg"))
                or (f.lower().endswith(".jpeg"))
            ):
                p = Path(dirpath) / f
                pngs.append((p, p.stat().st_size))
    return sorted(pngs, key=lambda x: x[1], reverse=True)


def run_fzf(items: Iterable[str]) -> List[str]:
    """Run fzf with multi-select enabled and return selected lines.

    Raises RuntimeError if fzf is not available.
    """
    if shutil.which("fzf") is None:
        raise RuntimeError("fzf is not installed or not found in PATH")

    preview_cmd = r"""bash -c '
  line="$1"
  relpath=$(echo "$line" | sed "s/.* | //" )
  echo "=== $relpath ==="
  stat -c "Size: %s bytes | Modified: %y" "$relpath" 2>/dev/null || echo "Size: unknown"
  echo "--- Potential savings ---"
  oxipng --pretend "$relpath" 2>/dev/null | head -6 || echo "oxipng preview unavailable"
' -- {}"""
    proc = subprocess.run(
        [
            "fzf",
            "-m",
            "--ansi",
            "--prompt=Select PNGs (largest first)> ",
            "--preview",
            preview_cmd,
            "--preview-window",
            "right:60%:noborder:wrap",
        ],
        input="\n".join(items),
        text=True,
        capture_output=True,
    )
    if proc.returncode not in (0, 1):
        # 1 means no selection (user pressed ESC or similar)
        raise RuntimeError(f"fzf exited with code {proc.returncode}: {proc.stderr}")
    selected = [line for line in proc.stdout.splitlines() if line.strip()]
    return selected


def optimize_file(path: str, dry_run: bool = False) -> tuple[str, int, str]:
    cmd = ["oxipng", "-o", "4", "--strip", "safe", "--alpha", path]
    if dry_run:
        return (path, 0, "DRY-RUN: " + " ".join(cmd))

    try:
        proc = subprocess.run(cmd, text=True, capture_output=True)
        out = (proc.stdout or "") + (proc.stderr or "")
        return (path, proc.returncode, out)
    except FileNotFoundError:
        return (path, 127, "oxipng not found in PATH")


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Optimize PNGs with oxipng using fzf")
    parser.add_argument(
        "--all",
        action="store_true",
        help="Optimize all PNGs found (don't run fzf)",
    )
    parser.add_argument(
        "-j",
        "--jobs",
        type=int,
        default=os.cpu_count() or 4,
        help="Number of parallel jobs (default: number of CPUs)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Don't actually run oxipng, just show commands",
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=Path.cwd(),
        help="Root directory to search (default: current working dir)",
    )

    args = parser.parse_args(argv)

    root = args.root.resolve()
    if not root.exists():
        print(f"Root path {root} does not exist", file=sys.stderr)
        return 2

    print(
        f"Searching for PNG files under {root} (excluding directories named 'build')..."
    )
    pngs = find_png_files(root)
    if not pngs:
        print("No PNG files found.")
        return 0

    file_displays = [
        f"{size:,} bytes | {p.relative_to(root).as_posix()}" for p, size in pngs
    ]

    selected_displays: List[str]
    if args.all:
        selected_displays = file_displays
    else:
        try:
            selected_displays = run_fzf(file_displays)
        except RuntimeError as e:
            print(f"Error: {e}", file=sys.stderr)
            print("Fallback: use --all to process everything without fzf.")
            return 3

    if not selected_displays:
        print("No files selected; exiting.")
        return 0

    selected_relpaths = [line.rsplit(" | ", 1)[-1] for line in selected_displays]
    full_paths = [str(root / rel) for rel in selected_relpaths]

    rel_to_size = {p.relative_to(root).as_posix(): size for p, size in pngs}
    before_total = sum(rel_to_size[rel] for rel in selected_relpaths)

    jobs = max(1, min(args.jobs, len(full_paths)))
    print(
        f"Optimizing {len(full_paths)} file(s) ({before_total:,} bytes) with {jobs} worker(s)..."
    )

    failures: List[tuple[str, int, str]] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=jobs) as exe:
        futures = [exe.submit(optimize_file, fp, args.dry_run) for fp in full_paths]
        for fut in concurrent.futures.as_completed(futures):
            path, rc, out = fut.result()
            if args.dry_run:
                print(out)
                continue
            if rc == 0:
                print(f"{path} — success")
                if out.strip():
                    print(out)
            elif rc == 127:
                print(f"{path} — oxipng not found", file=sys.stderr)
                failures.append((path, rc, out))
            else:
                print(f"{path} — failed (exit {rc})", file=sys.stderr)
                print(out, file=sys.stderr)
                failures.append((path, rc, out))

    if args.dry_run:
        print("Dry run complete")
    elif failures:
        print(f"Done with {len(failures)} failure(s).", file=sys.stderr)
        return 1
    else:
        after_total = sum(os.path.getsize(fp) for fp in full_paths)
        savings_bytes = before_total - after_total
        pct = (savings_bytes / before_total * 100) if before_total > 0 else 0
        print(f"\nTotal: {before_total:,} → {after_total:,} bytes")
        print(f"Saved: {savings_bytes:,} bytes ({pct:.1f}%)")

    print("All done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
