#!/usr/bin/env python3
"""
Crop images from a PNG using Aseprite-exported JSON metadata (slices or frames).

Usage:
  python tools/extract_from_ase_json.py sheet.png meta.json out_dir

How to get meta.json (recommended):
  If you have Aseprite installed, run:
    aseprite -b input.aseprite --sheet sheet.png --data meta.json
  This will export the flattened sheet and a JSON with slices/frames.

This script does NOT require any aseprite-specific Python package; it parses the JSON file produced by Aseprite CLI.
"""
import json
import sys
from pathlib import Path
from PIL import Image


def extract_from_json(png_path, json_path, out_dir):
    png = Image.open(png_path).convert('RGBA')
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    slices = []
    # Aseprite exports slices under meta.slices as a list
    meta = data.get('meta') or {}
    if 'slices' in meta and isinstance(meta['slices'], list):
        for s in meta['slices']:
            name = s.get('name') or s.get('slice') or f"slice_{len(slices)}"
            # keys may contain multiple keys; take first
            keys = s.get('keys') or []
            if keys:
                b = keys[0].get('bounds') or keys[0].get('frame') or None
                if b:
                    x = b.get('x')
                    y = b.get('y')
                    w = b.get('w')
                    h = b.get('h')
                    if None not in (x, y, w, h):
                        slices.append((name, int(x), int(y), int(w), int(h)))
    # fallback: some exports put frames in top-level 'frames' dict (frame names -> frame data)
    if not slices and 'frames' in data:
        frames = data['frames']
        # frames may be dict or list
        if isinstance(frames, dict):
            for name, frameinfo in frames.items():
                f = frameinfo.get('frame') or frameinfo.get('frameRect') or frameinfo
                x = f.get('x')
                y = f.get('y')
                w = f.get('w')
                h = f.get('h')
                if None not in (x, y, w, h):
                    slices.append((name, int(x), int(y), int(w), int(h)))
        elif isinstance(frames, list):
            for i, frameinfo in enumerate(frames):
                fname = frameinfo.get('filename') or f'frame_{i}'
                f = frameinfo.get('frame') or frameinfo
                x = f.get('x')
                y = f.get('y')
                w = f.get('w')
                h = f.get('h')
                if None not in (x, y, w, h):
                    slices.append((fname, int(x), int(y), int(w), int(h)))

    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    saved = 0
    for name, x, y, w, h in slices:
        crop = png.crop((x, y, x + w, y + h))
        # sanitize name
        safe_name = ''.join(c if c.isalnum() or c in '.-_ ' else '_' for c in name).strip()
        if not safe_name:
            safe_name = f'slice_{saved}'
        out_path = out_dir / f"{safe_name}.png"
        # avoid overwriting by adding index if exists
        if out_path.exists():
            out_path = out_dir / f"{safe_name}_{saved}.png"
        crop.save(out_path)
        saved += 1

    return saved


if __name__ == '__main__':
    if len(sys.argv) < 4:
        print('Usage: extract_from_ase_json.py sheet.png meta.json out_dir')
        sys.exit(1)
    sheet = Path(sys.argv[1])
    meta = Path(sys.argv[2])
    out = Path(sys.argv[3])
    n = extract_from_json(sheet, meta, out)
    print(f"Saved {n} slices to {out}")
