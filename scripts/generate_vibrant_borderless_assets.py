#!/usr/bin/env python3
"""
Generate ultra-premium, borderless AURA assets with full, radiant colors:
- Eliminates 100% of the squircle border rim.
- Restores 100% solid opacity across the ribbon body (no washed-out semi-transparency).
- Calibrates true chromatic vibrancy: Liquid Titanium Silver, Iridescent Cool Slate, Warm Champagne Bronze, and Pure Radiant White highlights.
- Centers the acoustic ribbon monogram at (512, 512).
- Exports pixel-perfect assets across all Android drawable densities, launcher mipmaps, and cross-platform targets.
"""

import os
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import gaussian_filter

def main():
    master_path = '/Users/spandandhage/.gemini/antigravity/brain/2d9f671c-7f81-4ea8-81c1-a0c3e9793a01/.user_uploaded/media_1793555902401_b0d5b921.png'
    print(f"Loading master artwork from {master_path}...")
    orig = Image.open(master_path).convert('RGB')
    arr = np.array(orig, dtype=np.float32)
    H, W = arr.shape[:2]

    # 1. Luminance computation
    lum = 0.299 * arr[..., 0] + 0.587 * arr[..., 1] + 0.114 * arr[..., 2]

    # 2. Polygon mask to isolate the acoustic ribbon wave completely within the dark moat
    # This guarantees ZERO border rim or squircle edge pixels are included.
    mask_img = Image.new('L', (W, H), 0)
    draw = ImageDraw.Draw(mask_img)
    poly = [
        (512, 280), (660, 335), (785, 475), (866, 585),
        (868, 642), (845, 678), (750, 715), (520, 715),
        (235, 715), (218, 640), (315, 475), (415, 355),
    ]
    draw.polygon(poly, fill=255)
    poly_mask = np.array(mask_img, dtype=np.float32) / 255.0
    poly_mask = gaussian_filter(poly_mask, sigma=5.0)

    # 3. Solid Opacity & Natural Edge Antialiasing
    # Noise floor in the void is ~4.5.
    # Solid 100% opacity starts at lum = 16.0, ensuring the entire 3D ribbon body is solid and punchy.
    t = np.clip((lum - 4.2) / (16.0 - 4.2), 0.0, 1.0)
    smooth_alpha = t * t * (3.0 - 2.0 * t) * poly_mask

    # 4. Color & Vibrancy Calibration (Make colors rich, bright, and radiant like before)
    # Normalized [0, 1]
    rgb_norm = arr / 255.0

    # Gentle filmic tone curve to lift deep frosted midtones on OLED black (gamma = 0.84)
    # Peak whites [255, 255, 255] remain uncompressed at 255.
    rgb_lift = np.power(rgb_norm, 0.84) * 255.0

    # Subtle vibrance boost (+12% saturation in chrominance) to bring out the titanium blue and bronze hues
    mean_val = np.mean(rgb_lift, axis=-1, keepdims=True)
    rgb_vibrant = mean_val + (rgb_lift - mean_val) * 1.14
    rgb_final = np.clip(rgb_vibrant, 0.0, 255.0)

    # Assemble 32-bit RGBA master ribbon
    ribbon_rgba = np.zeros((H, W, 4), dtype=np.float32)
    ribbon_rgba[..., :3] = rgb_final
    ribbon_rgba[..., 3] = smooth_alpha * 255.0

    # 5. Precision Geometric Centering
    # Compute center of visible mass/bounding box
    vis = ribbon_rgba[..., 3] > 15
    ys, xs = np.where(vis)
    ymin, ymax = ys.min(), ys.max()
    xmin, xmax = xs.min(), xs.max()
    cx = (xmin + xmax) / 2.0
    cy = (ymin + ymax) / 2.0
    dx = int(round(512.0 - cx))
    dy = int(round(512.0 - cy))
    print(f"Visible ribbon bounds: X=[{xmin}, {xmax}], Y=[{ymin}, {ymax}], Center=({cx:.1f}, {cy:.1f}), Shift=({dx}, {dy})")

    shifted = np.zeros_like(ribbon_rgba)
    src_y1 = max(0, -dy); src_y2 = min(H, H - dy)
    dst_y1 = max(0, dy);  dst_y2 = min(H, H + dy)
    src_x1 = max(0, -dx); src_x2 = min(W, W - dx)
    dst_x1 = max(0, dx);  dst_x2 = min(W, W + dx)
    shifted[dst_y1:dst_y2, dst_x1:dst_x2] = ribbon_rgba[src_y1:src_y2, src_x1:src_x2]

    master_ribbon = Image.fromarray(np.clip(shifted, 0, 255).astype(np.uint8), 'RGBA')
    master_ribbon.save('/tmp/master_borderless_ribbon.png')
    print("Master borderless ribbon saved to /tmp/master_borderless_ribbon.png")

    # 6. Save Splash Logo (720x720) for OpeningReveal.kt
    splash = master_ribbon.resize((720, 720), Image.Resampling.LANCZOS)
    splash.save('app/src/main/res/drawable-nodpi/splash_logo.png', optimize=True)
    print("Updated app/src/main/res/drawable-nodpi/splash_logo.png (720x720)")

    # 7. Adaptive Launcher Foregrounds (Transparent PNGs)
    densities = {
        'xxxhdpi': 432,
        'xxhdpi': 324,
        'xhdpi': 216,
        'hdpi': 162,
        'mdpi': 108
    }
    for density, size in densities.items():
        fg = master_ribbon.resize((size, size), Image.Resampling.LANCZOS)
        path = f'app/src/main/res/drawable-{density}/ic_launcher_foreground.png'
        fg.save(path, optimize=True)
        print(f"Updated {path} ({size}x{size})")

    # 8. Legacy Mipmap Icons (Composite on pure black)
    mipmap_sizes = {
        'xxxhdpi': 192,
        'xxhdpi': 144,
        'xhdpi': 96,
        'hdpi': 72,
        'mdpi': 48
    }
    for density, size in mipmap_sizes.items():
        # Square on pure black
        bg_sq = Image.new('RGBA', (size, size), (0, 0, 0, 255))
        fg = master_ribbon.resize((size, size), Image.Resampling.LANCZOS)
        bg_sq.paste(fg, (0, 0), fg)
        path_sq = f'app/src/main/res/mipmap-{density}/ic_launcher.png'
        bg_sq.save(path_sq, optimize=True)

        # Round on pure black
        bg_round = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        mask_circle = Image.new('L', (size, size), 0)
        draw_c = ImageDraw.Draw(mask_circle)
        draw_c.ellipse((0, 0, size, size), fill=255)
        bg_black = Image.new('RGBA', (size, size), (0, 0, 0, 255))
        bg_black.paste(fg, (0, 0), fg)
        bg_round.paste(bg_black, (0, 0), mask_circle)
        path_round = f'app/src/main/res/mipmap-{density}/ic_launcher_round.png'
        bg_round.save(path_round, optimize=True)
        print(f"Updated mipmap {density} icons ({size}x{size})")

    # 9. Cross-Platform & Documentation Assets
    master_ribbon.save('docs/logo.png', optimize=True)
    master_ribbon.save('desktop/packaging/icons/AppIcon.png', optimize=True)
    master_ribbon.save('ios/Aura/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png', optimize=True)
    
    icon_512 = master_ribbon.resize((512, 512), Image.Resampling.LANCZOS)
    icon_512.save('fastlane/metadata/android/en-US/images/icon.png', optimize=True)
    icon_512.save('site/icon-512.png', optimize=True)

    master_ribbon.resize((192, 192), Image.Resampling.LANCZOS).save('site/icon-192.png', optimize=True)
    master_ribbon.resize((180, 180), Image.Resampling.LANCZOS).save('site/apple-touch-icon.png', optimize=True)
    master_ribbon.resize((96, 96), Image.Resampling.LANCZOS).save('site/favicon-96x96.png', optimize=True)

    # 10. Artifact Directory Preview
    artifact_preview = '/Users/spandandhage/.gemini/antigravity/brain/2d9f671c-7f81-4ea8-81c1-a0c3e9793a01/squircle_transparent.png'
    master_ribbon.save(artifact_preview, optimize=True)
    print(f"Updated artifact preview {artifact_preview}")

    print("\nALL ASSETS SUCCESSFULLY REGENERATED WITH VIBRANT RADIANT COLORS & ZERO BORDER!")

if __name__ == '__main__':
    main()
