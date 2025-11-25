#!/usr/bin/env python3
import argparse
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image


def _merge_bboxes(bboxes, merge_distance):
    """Merge bboxes that are within merge_distance of each other.

    bboxes: list of (min_y, min_x, max_y, max_x)
    Returns merged list.
    """
    if not bboxes:
        return []

    # normalize to [y1,x1,y2,x2]
    boxes = [list(b) for b in bboxes]

    def should_merge(a, b):
        # expand a by merge_distance and check overlap with b
        return not (
            a[2] + merge_distance < b[0] - merge_distance
            or b[2] + merge_distance < a[0] - merge_distance
            or a[3] + merge_distance < b[1] - merge_distance
            or b[3] + merge_distance < a[1] - merge_distance
        )

    merged = True
    while merged:
        merged = False
        out = []
        used = [False] * len(boxes)
        for i, a in enumerate(boxes):
            if used[i]:
                continue
            cur = a[:]
            for j in range(i + 1, len(boxes)):
                if used[j]:
                    continue
                b = boxes[j]
                if should_merge(cur, b):
                    # merge
                    cur[0] = min(cur[0], b[0])
                    cur[1] = min(cur[1], b[1])
                    cur[2] = max(cur[2], b[2])
                    cur[3] = max(cur[3], b[3])
                    used[j] = True
                    merged = True
            out.append(cur)
            used[i] = True
        boxes = out

    # convert back to tuples
    return [tuple(b) for b in boxes]


def _merge_components_by_proximity(components, merge_distance):
    """Merge components only when their pixels are within merge_distance.

    components: list of dicts with keys: min_y,min_x,max_y,max_x,count,pixels (Nx2 numpy int)
    Returns list of merged bboxes [(min_y,min_x,max_y,max_x), ...]
    """
    n = len(components)
    if n == 0:
        return []

    used = [False] * n
    merged_boxes = []

    for i in range(n):
        if used[i]:
            continue
        # start new group
        group_idxs = [i]
        used[i] = True
        changed = True
        while changed:
            changed = False
            for j in range(n):
                if used[j]:
                    continue
                # check proximity to any member of group
                prox = False
                for gi in list(group_idxs):
                    a = components[gi]
                    b = components[j]
                    # quick bbox expand test
                    if (a['max_y'] + merge_distance < b['min_y'] - merge_distance or
                        b['max_y'] + merge_distance < a['min_y'] - merge_distance or
                        a['max_x'] + merge_distance < b['min_x'] - merge_distance or
                        b['max_x'] + merge_distance < a['min_x'] - merge_distance):
                        continue

                    # restrict to bbox overlap area expanded
                    ymin = max(a['min_y'] - merge_distance, b['min_y'] - merge_distance)
                    ymax = min(a['max_y'] + merge_distance, b['max_y'] + merge_distance)
                    xmin = max(a['min_x'] - merge_distance, b['min_x'] - merge_distance)
                    xmax = min(a['max_x'] + merge_distance, b['max_x'] + merge_distance)
                    if ymin > ymax or xmin > xmax:
                        continue

                    # Select pixels of a and b within that area to reduce cost
                    pa = a['pixels']
                    pb = b['pixels']
                    # mask
                    ma = (pa[:,0] >= ymin) & (pa[:,0] <= ymax) & (pa[:,1] >= xmin) & (pa[:,1] <= xmax)
                    mb = (pb[:,0] >= ymin) & (pb[:,0] <= ymax) & (pb[:,1] >= xmin) & (pb[:,1] <= xmax)
                    if not ma.any() or not mb.any():
                        # no pixels in intersection area
                        continue
                    sa = pa[ma]
                    sb = pb[mb]
                    # compute pairwise squared distances, but keep it bounded
                    # vectorized: for smaller array iterate over smaller
                    if sa.shape[0] > sb.shape[0]:
                        small, large = sb, sa
                    else:
                        small, large = sa, sb
                    # compute distances
                    # for each small pixel compute if any large pixel within merge_distance
                    md2 = merge_distance * merge_distance
                    for p in small:
                        dy = large[:,0] - p[0]
                        dx = large[:,1] - p[1]
                        if np.any(dy*dy + dx*dx <= md2):
                            prox = True
                            break
                    if prox:
                        break

                if prox:
                    group_idxs.append(j)
                    used[j] = True
                    changed = True

        # merge group into single bbox
        mins_y = min(components[k]['min_y'] for k in group_idxs)
        mins_x = min(components[k]['min_x'] for k in group_idxs)
        maxs_y = max(components[k]['max_y'] for k in group_idxs)
        maxs_x = max(components[k]['max_x'] for k in group_idxs)
        merged_boxes.append((mins_y, mins_x, maxs_y, maxs_x))

    return merged_boxes


def extract_sprites(source, target, alpha_threshold, min_pixels, padding, merge_distance=4):
    """Extract sprites from source image using connected components and merge nearby boxes.

    merge_distance: distance in pixels; bboxes within this distance will be merged so a sprite
    spanning small transparent gaps isn't split.
    """
    img = Image.open(source).convert("RGBA")
    alpha = np.array(img)[:, :, 3]
    solid = alpha > alpha_threshold
    visited = np.zeros_like(solid, dtype=bool)

    height, width = solid.shape

    components = []  # list of dicts: {min_y,min_x,max_y,max_x,count,pixels}

    for y in range(height):
        for x in range(width):
            if not solid[y, x] or visited[y, x]:
                continue

            queue = deque([(y, x)])
            visited[y, x] = True
            min_y = max_y = y
            min_x = max_x = x
            pixels = []

            while queue:
                cy, cx = queue.popleft()
                pixels.append((cy, cx))
                min_y = min(min_y, cy)
                max_y = max(max_y, cy)
                min_x = min(min_x, cx)
                max_x = max(max_x, cx)

                for ny, nx in ((cy - 1, cx), (cy + 1, cx), (cy, cx - 1), (cy, cx + 1)):
                    if 0 <= ny < height and 0 <= nx < width and solid[ny, nx] and not visited[ny, nx]:
                        visited[ny, nx] = True
                        queue.append((ny, nx))

            components.append({
                'min_y': min_y,
                'min_x': min_x,
                'max_y': max_y,
                'max_x': max_x,
                'count': len(pixels),
                'pixels': np.array(pixels, dtype=np.int32),
            })

    # filter small components
    components = [c for c in components if c['count'] >= min_pixels]

    # merge components using pixel proximity test
    merged = _merge_components_by_proximity(components, merge_distance)

    saved = 0
    for bbox in merged:
        min_y, min_x, max_y, max_x = bbox
        top = max(min_y - padding, 0)
        bottom = min(max_y + padding + 1, height)
        left = max(min_x - padding, 0)
        right = min(max_x + padding + 1, width)

        crop = img.crop((left, top, right, bottom))
        crop.save(target / f"sprite_{saved:03d}.png")
        saved += 1

    return saved


def split_grid(source, target, tile_width, tile_height, cols, rows, margin, spacing, trim):
    """Split image into a regular grid of tiles.

    Either provide tile_width/tile_height or cols/rows. Margin and spacing are in pixels.
    If trim is True, each tile will have transparent borders trimmed to the non-transparent bbox.
    Returns number of saved tiles.
    """
    img = Image.open(source).convert("RGBA")
    width, height = img.size

    # Determine cols/rows and tile size
    if cols is None and tile_width is None:
        raise ValueError("Either --cols or --tile-width must be provided for grid mode")
    if rows is None and tile_height is None:
        raise ValueError("Either --rows or --tile-height must be provided for grid mode")

    if cols is None:
        # compute cols from tile_width
        cols = (width - 2 * margin + spacing) // (tile_width + spacing)
    else:
        cols = int(cols)

    if rows is None:
        rows = (height - 2 * margin + spacing) // (tile_height + spacing)
    else:
        rows = int(rows)

    if tile_width is None:
        tile_width = (width - 2 * margin - spacing * (cols - 1)) // cols
    if tile_height is None:
        tile_height = (height - 2 * margin - spacing * (rows - 1)) // rows

    saved = 0
    for r in range(rows):
        for c in range(cols):
            left = margin + c * (tile_width + spacing)
            top = margin + r * (tile_height + spacing)
            right = left + tile_width
            bottom = top + tile_height

            # clamp to image
            left = max(0, left)
            top = max(0, top)
            right = min(width, right)
            bottom = min(height, bottom)

            if left >= right or top >= bottom:
                continue

            crop = img.crop((left, top, right, bottom))

            if trim:
                # find bbox of non-transparent pixels
                alpha = np.array(crop)[:, :, 3]
                ys, xs = np.where(alpha > 0)
                if ys.size == 0 or xs.size == 0:
                    # fully transparent, keep original
                    trimmed = crop
                else:
                    min_y, max_y = int(ys.min()), int(ys.max())
                    min_x, max_x = int(xs.min()), int(xs.max())
                    trimmed = crop.crop((min_x, min_y, max_x + 1, max_y + 1))
                out_img = trimmed
            else:
                out_img = crop

            out_name = f"tile_r{r:03d}_c{c:03d}.png"
            out_img.save(target / out_name)
            saved += 1

    return saved


def main():
    parser = argparse.ArgumentParser(description="Split a spritesheet into individual PNG sprites.")
    parser.add_argument("source", type=Path, help="Path to the spritesheet PNG.")
    parser.add_argument("target", type=Path, help="Directory for the extracted sprites.")
    parser.add_argument("--mode", choices=("blob", "grid"), default="grid", help="Extraction mode: 'blob' finds connected components, 'grid' splits into tiles.")
    # Blob mode options (original behavior)
    parser.add_argument("--alpha-threshold", type=int, default=0, help="Alpha cutoff (0-255) used in blob mode.")
    parser.add_argument("--min-pixels", type=int, default=25, help="Ignore blobs below this pixel count (blob mode).")
    parser.add_argument("--padding", type=int, default=0, help="Extra pixels of transparent padding around each crop (blob mode).")
    parser.add_argument("--merge-distance", type=int, default=4, help="Merge nearby blobs within this pixel distance so a sprite isn't cut in pieces (blob mode).")
    # Grid mode options
    parser.add_argument("--tile-width", type=int, default=None, help="Tile width in pixels for grid mode.")
    parser.add_argument("--tile-height", type=int, default=None, help="Tile height in pixels for grid mode.")
    parser.add_argument("--cols", type=int, default=None, help="Number of columns in grid mode. If omitted, computed from tile width.")
    parser.add_argument("--rows", type=int, default=None, help="Number of rows in grid mode. If omitted, computed from tile height.")
    parser.add_argument("--margin", type=int, default=0, help="Outer margin in pixels (grid mode).")
    parser.add_argument("--spacing", type=int, default=0, help="Spacing between tiles in pixels (grid mode).")
    parser.add_argument("--trim", action="store_true", help="Trim transparent borders from each tile (grid mode).")
    args = parser.parse_args()

    args.target.mkdir(parents=True, exist_ok=True)
    if args.mode == "blob":
        count = extract_sprites(
            source=args.source,
            target=args.target,
            alpha_threshold=args.alpha_threshold,
            min_pixels=args.min_pixels,
            padding=args.padding,
            merge_distance=args.merge_distance,
        )
    else:
        count = split_grid(
            source=args.source,
            target=args.target,
            tile_width=args.tile_width,
            tile_height=args.tile_height,
            cols=args.cols,
            rows=args.rows,
            margin=args.margin,
            spacing=args.spacing,
            trim=args.trim,
        )

    print(f"Saved {count} images to {args.target} (mode={args.mode})")


if __name__ == "__main__":
    main()