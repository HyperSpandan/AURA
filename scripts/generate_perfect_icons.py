#!/usr/bin/env python3
"""
Generate ultra-premium AURA launcher icons and drawables with:
1. Exact Euclidean Distance Transform (EDT) 360-degree specular rim.
2. Reinforced rounded corner crest width and brilliance (eliminating corner thinning).
3. Calibrated safe keyline zone (66dp / 264px in xxxhdpi) ensuring zero clipping on Samsung One UI & Pixel launchers.
4. Chromatic dual-tone specular reflections: Champagne Bronze (Top/TR) and Cool Liquid Titanium (TL/BL/BR).
"""

import os
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt, label

def main():
    master_path = '/Users/spandandhage/.gemini/antigravity/brain/2d9f671c-7f81-4ea8-81c1-a0c3e9793a01/.user_uploaded/media_1793555902401_b0d5b921.png'
    print(f"Loading master image from {master_path}...")
    orig = Image.open(master_path).convert('RGBA')
    arr = np.array(orig, dtype=np.float32)
    H, W = arr.shape[:2]

    # 1. Precise Squircle Mask Extraction
    lum = 0.299 * arr[..., 0] + 0.587 * arr[..., 1] + 0.114 * arr[..., 2]
    bg = lum < 2.0
    labeled, _ = label(bg)
    bg_mask = (labeled == labeled[0, 0]) | (labeled == labeled[0, -1]) | (labeled == labeled[-1, 0]) | (labeled == labeled[-1, -1])
    sq_mask = ~bg_mask

    # 2. Euclidean Distance Transform from Squircle Boundary
    dist_in = distance_transform_edt(sq_mask)

    # 3. Angle-based Corner Reinforcement
    cx, cy = 508.5, 509.0
    Y, X = np.ogrid[:H, :W]
    theta = np.arctan2(Y - cy, X - cx) % (2 * np.pi)

    corner_thetas = np.array([0.25, 0.75, 1.25, 1.75]) * np.pi
    t_diffs = np.abs(theta[..., None] - corner_thetas)
    min_t_dist = np.min(np.minimum(t_diffs, 2 * np.pi - t_diffs), axis=-1)
    corner_weight = np.clip(1.0 - (min_t_dist / 0.55), 0.0, 1.0)
    corner_weight = 0.5 * (1.0 - np.cos(np.pi * corner_weight)) # Smooth cosine easing

    # Calibrate rim geometry:
    # Straight edges: crest 6px, bevel 20px
    # Rounded corners: crest 11px, bevel 32px (guaranteeing >= 4px bright rim on mobile displays)
    crest_start = 0.8
    crest_end = 6.0 + 5.0 * corner_weight
    falloff_end = 20.0 + 12.0 * corner_weight

    rim_intensity = np.zeros((H, W), dtype=np.float32)

    # Outer sub-pixel antialiasing
    m1 = (dist_in >= 0) & (dist_in < crest_start) & sq_mask
    rim_intensity[m1] = dist_in[m1] / crest_start

    # Specular crest (peak brilliance)
    m2 = (dist_in >= crest_start) & (dist_in <= crest_end) & sq_mask
    rim_intensity[m2] = 1.0

    # Smooth bevel falloff into dark glass volume
    m3 = (dist_in > crest_end) & (dist_in <= falloff_end) & sq_mask
    t3 = (dist_in[m3] - crest_end[m3]) / (falloff_end[m3] - crest_end[m3])
    rim_intensity[m3] = 0.5 * (1.0 + np.cos(np.pi * t3))

    # 4. Chromatic Lighting Distribution
    # Warm Champagne Bronze for top and top-right (around theta ~ 1.75pi to 0.1pi)
    warm_ang_diff = np.abs(theta - 1.85 * np.pi)
    warm_ang_diff = np.minimum(warm_ang_diff, 2 * np.pi - warm_ang_diff)
    warm_weight = np.clip(1.0 - (warm_ang_diff / (0.45 * np.pi)), 0.0, 1.0)
    warm_weight = 0.5 * (1.0 - np.cos(np.pi * warm_weight))

    rim_crest_rgb = np.zeros((H, W, 3), dtype=np.float32)
    rim_crest_rgb[..., 0] = 255.0
    rim_crest_rgb[..., 1] = 255.0 - warm_weight * 8.0
    rim_crest_rgb[..., 2] = 255.0 - warm_weight * 22.0

    rim_bevel_rgb = np.zeros((H, W, 3), dtype=np.float32)
    rim_bevel_rgb[..., 0] = (1.0 - warm_weight) * 225.0 + warm_weight * 250.0
    rim_bevel_rgb[..., 1] = (1.0 - warm_weight) * 235.0 + warm_weight * 228.0
    rim_bevel_rgb[..., 2] = (1.0 - warm_weight) * 248.0 + warm_weight * 200.0

    crest_blend = np.clip((dist_in - 3.0) / 12.0, 0.0, 1.0)
    rim_rgb = (1.0 - crest_blend[..., None]) * rim_crest_rgb + crest_blend[..., None] * rim_bevel_rgb

    # 5. Composite Specular Rim onto Master
    out_arr = arr.copy()
    alpha = np.clip(dist_in / 1.0, 0.0, 1.0) * 255.0
    out_arr[..., 3] = np.clip(alpha, 0, 255)

    for c in range(3):
        base_c = out_arr[..., c]
        rim_c = rim_intensity * rim_rgb[..., c]
        out_arr[..., c] = np.clip(np.maximum(base_c, rim_c), 0, 255)

    squircle_master = Image.fromarray(out_arr.astype(np.uint8), 'RGBA')

    # 6. Crop Exact Centered Square Squircle Tile
    ys, xs = np.where(out_arr[..., 3] > 10)
    ymin, ymax = ys.min(), ys.max()
    xmin, xmax = xs.min(), xs.max()
    cx_crop = (xmin + xmax) / 2.0
    cy_crop = (ymin + ymax) / 2.0
    box_size = max(xmax - xmin + 1, ymax - ymin + 1)
    half_sz = box_size / 2.0
    crop_box = (int(cx_crop - half_sz), int(cy_crop - half_sz), int(cx_crop + half_sz), int(cy_crop + half_sz))
    tile = squircle_master.crop(crop_box)
    print(f"Cropped centered squircle tile: {tile.size}")

    # 7. Generate Adaptive Icon Foregrounds
    # Keyline: 66dp (guarantees safe margins inside Samsung One UI 72dp squircle and Pixel 72dp circle)
    adaptive_densities = {
        'xxxhdpi': (432, 264),
        'xxhdpi':  (324, 198),
        'xhdpi':   (216, 132),
        'hdpi':    (162, 99),
        'mdpi':    (108, 66)
    }

    for d, (canvas_sz, keyline) in adaptive_densities.items():
        scaled_tile = tile.resize((keyline, keyline), Image.Resampling.LANCZOS)
        fg = Image.new('RGBA', (canvas_sz, canvas_sz), (0, 0, 0, 0))
        offset = (canvas_sz - keyline) // 2
        fg.paste(scaled_tile, (offset, offset), scaled_tile)
        
        out_dir = f'app/src/main/res/drawable-{d}'
        os.makedirs(out_dir, exist_ok=True)
        out_path = f'{out_dir}/ic_launcher_foreground.png'
        fg.save(out_path, optimize=True)
        print(f"Generated {out_path} ({canvas_sz}x{canvas_sz}, squircle={keyline}x{keyline})")

    # 8. Generate Legacy Mipmaps (Full-bleed squircle with comfortable edge margin)
    mipmap_densities = {
        'xxxhdpi': (192, 180),
        'xxhdpi':  (144, 135),
        'xhdpi':   (96, 90),
        'hdpi':    (72, 68),
        'mdpi':    (48, 45)
    }

    for d, (canvas_sz, tile_sz) in mipmap_densities.items():
        scaled_tile = tile.resize((tile_sz, tile_sz), Image.Resampling.LANCZOS)
        icon = Image.new('RGBA', (canvas_sz, canvas_sz), (0, 0, 0, 0))
        offset = (canvas_sz - tile_sz) // 2
        icon.paste(scaled_tile, (offset, offset), scaled_tile)
        
        out_dir = f'app/src/main/res/mipmap-{d}'
        os.makedirs(out_dir, exist_ok=True)
        icon.save(f'{out_dir}/ic_launcher.png', optimize=True)

        # Round icon (circle mask)
        round_mask = Image.new('L', (canvas_sz, canvas_sz), 0)
        draw = ImageDraw.Draw(round_mask)
        draw.ellipse([offset, offset, offset + tile_sz, offset + tile_sz], fill=255)
        round_icon = Image.new('RGBA', (canvas_sz, canvas_sz), (0, 0, 0, 0))
        round_icon.paste(icon, (0, 0), round_mask)
        round_icon.save(f'{out_dir}/ic_launcher_round.png', optimize=True)
        print(f"Generated {out_dir}/ic_launcher.png & ic_launcher_round.png")

    # 9. Splash Screen Logo (720x720, pre-scaled for 60fps GPU RenderNode reveal)
    splash = tile.resize((720, 720), Image.Resampling.LANCZOS)
    splash_path = 'app/src/main/res/drawable-nodpi/splash_logo.png'
    splash.save(splash_path, optimize=True)
    print(f"Generated {splash_path} (720x720)")

    # 10. Documentation and Store Assets
    store_icon = tile.resize((512, 512), Image.Resampling.LANCZOS)
    store_icon.save('docs/logo.png', optimize=True)
    store_icon.save('fastlane/metadata/android/en-US/images/icon.png', optimize=True)
    print("Generated docs/logo.png and fastlane/metadata/android/en-US/images/icon.png")

    print("\nAll icon assets successfully created and calibrated!")

if __name__ == '__main__':
    main()
