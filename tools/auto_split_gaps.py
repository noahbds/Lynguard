#!/usr/bin/env python3
import sys
from pathlib import Path
from PIL import Image
import numpy as np

def split_by_gaps(source, target, alpha_threshold=10, gap_width=8, padding=2):
    img = Image.open(source).convert('RGBA')
    arr = np.array(img)
    alpha = arr[:, :, 3]
    h, w = alpha.shape

    col_has = (alpha > alpha_threshold).any(axis=0)
    # find x segments where col_has is True
    xs = []
    in_seg = False
    seg_start = 0
    for x in range(w):
        if col_has[x]:
            if not in_seg:
                in_seg = True
                seg_start = x
        else:
            if in_seg:
                # check gap length ahead
                # if next gap >= gap_width, end segment
                # to do that, look ahead gap size
                gap = 0
                j = x
                while j < w and not col_has[j] and gap < gap_width:
                    gap += 1
                    j += 1
                if gap >= gap_width:
                    xs.append((seg_start, x-1))
                    in_seg = False
    if in_seg:
        xs.append((seg_start, w-1))

    saved = 0
    target = Path(target)
    target.mkdir(parents=True, exist_ok=True)

    for xi, (x0, x1) in enumerate(xs):
        sub_alpha = alpha[:, x0:x1+1]
        row_has = (sub_alpha > alpha_threshold).any(axis=1)
        in_r = False
        rstart = 0
        rows = []
        for y in range(h):
            if row_has[y]:
                if not in_r:
                    in_r = True
                    rstart = y
            else:
                if in_r:
                    # check gap in rows
                    gap = 0
                    j = y
                    while j < h and not row_has[j] and gap < gap_width:
                        gap += 1
                        j += 1
                    if gap >= gap_width:
                        rows.append((rstart, y-1))
                        in_r = False
        if in_r:
            rows.append((rstart, h-1))

        for yi, (y0, y1) in enumerate(rows):
            left = max(0, x0 - padding)
            right = min(w, x1 + 1 + padding)
            top = max(0, y0 - padding)
            bottom = min(h, y1 + 1 + padding)
            crop = img.crop((left, top, right, bottom))
            out = target / f"auto_{xi:02d}_{yi:02d}.png"
            crop.save(out)
            saved += 1
    return saved

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print('Usage: auto_split_gaps.py SOURCE.png TARGET_DIR [alpha_threshold] [gap_width] [padding]')
        sys.exit(1)
    src = Path(sys.argv[1])
    tgt = Path(sys.argv[2])
    alpha = int(sys.argv[3]) if len(sys.argv) > 3 else 10
    gap = int(sys.argv[4]) if len(sys.argv) > 4 else 8
    pad = int(sys.argv[5]) if len(sys.argv) > 5 else 2
    n = split_by_gaps(src, tgt, alpha_threshold=alpha, gap_width=gap, padding=pad)
    print(f"Saved {n} images to {tgt}")
