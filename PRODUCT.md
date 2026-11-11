# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Pure modern HTML/CSS/JS (zero external dependencies, hardware-accelerated CSS and Canvas, deployed directly to GitHub Pages).

## Users

Audiophiles, daily music streamers, and power listeners who want an ad-free, high-fidelity YouTube Music experience with album-art aesthetics, word-synced lyrics, lossless audio upgrades, and zero subscription barriers.

## Product Purpose

AURA exists to deliver a distraction-free, ad-free, and account-optional music listening client with studio-grade audio controls, rich tactile animations, and an unmistakable visual identity built around fluid glass, refractive ambient light, and 3D Cover Flow. Success means users can immediately discover, stream, customize, and enjoy music without ads, tracking, or mandatory logins across mobile, desktop, TV, and web.

## Positioning

The only open-source music player combining the entire YouTube Music catalogue with BitChord lossless FLAC audio resolvers, an interactive 3D Cover Flow carousel, fluid morphing capsule dock, decentralized Nostr Listen Together rooms, and bit-perfect USB DAC output—completely free under GPL-3.0 with zero ads or subscriptions.

## Operating Context

- Mobile and TV listening via the Android application (`arm64-v8a` and `universal` APKs).
- Desktop listening across Windows, macOS, and Linux.
- Web player companion with browser extension integration.
- Public web portal on GitHub Pages (`https://hyperspandan.github.io/AURA/`) acting as the primary installation, discovery, documentation, and live preview hub.

## Capabilities and Constraints

- **Streaming Engine:** YouTube Music catalogue integration with dual-stream failover and offline downloads.
- **Audio Fidelity:** 15-band EQ, AutoEq correction for 8,000+ headphones, EBU R128 loudness normalization, and BitChord lossless FLAC upgrade pipeline.
- **Visual Features:** 3D Cover Flow carousel, organic letterform-bloom synced lyrics, fluid morphing capsule dock, and real-time cover-art ambient aura extraction.
- **Synchronized Listening:** End-to-end encrypted rooms powered by Nostr relays with real-time drift correction and push-to-talk voice notes.
- **Deployment Constraints:** Must deploy seamlessly to GitHub Pages via `.github/workflows/pages.yml`, preserving release API integration (`releases.json`), iOS SideStore/AltStore source (`ios/source.json`), download tracking (`download-tracking.js`), and social crawler verification (`scripts/check-social-preview.py`).

## Brand Commitments

- **Name:** AURA (always capitalized).
- **Identity:** Obsidian canvas, ambient refractive light glows, and liquid glass capsules (mirroring the refractive ribbon wave logo and in-app `refracted_aura` shader).
- **Independence:** 100% original project identity under HyperSpandan; zero references or visual cloning of legacy forks.
- **License:** GNU General Public License v3.0 (GPL-3.0).

## Evidence on Hand

- Native Android source code and verified test suite in `app/` and `core/`.
- Official high-resolution AURA wave logo and favicon suite in `site/` and `docs/`.
- High-res application screenshots in `docs/screenshots/` (Player, Synced Lyrics, 3D Cover Flow Explore, Radio, Listen Together, Podcasts).
- Verified production README banner in `docs/banner.jpg`.
- Released builds and GitHub Actions CI pipelines at `github.com/HyperSpandan/AURA`.

## Product Principles

1. **Music-First Craft:** The interface recedes and elevates the album artwork and sound; interactions feel organic, tactile, and responsive.
2. **True Freedom & Privacy:** No mandatory accounts, zero ad networks, and no trackers. All local library data stays on the user's device.
3. **Audiophile Integrity:** Audio quality is never compromised; fast initial playback is immediately backed by bit-perfect and lossless pipelines where available.
4. **Transparent Open Source:** Code, release binaries, checksums, and architecture are open and verifiable by the community.

## Accessibility & Inclusion

- Respect for `prefers-reduced-motion` with instant, clean static fallbacks for all live canvas and scroll-driven animations.
- Accessible color contrast across dark obsidian surfaces with high-visibility typography and clear interactive focus rings.
- Semantic HTML landmarks, ARIA labels on all live controls, and full keyboard navigability.
