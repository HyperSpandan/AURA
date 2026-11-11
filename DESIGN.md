---
name: AURA
description: Editorial Audio Bench design system for AURA's open-source lossless music portal and companion pages.
colors:
  grain-orange: "#FF5F26"
  grain-orange-hover: "#FF713E"
  soft-coral-orange: "#FF8759"
  orange-dim-wash: "rgba(255, 95, 38, 0.14)"
  ink-black: "#0A0A0A"
  studio-void: "#080808"
  charred-ink: "#131313"
  panel-charcoal: "#181818"
  row-hover: "#1F1F1F"
  sheet-charcoal: "#1B1B1E"
  hairline-rule: "#262626"
  rule-highlight: "#3A3A3A"
  theme-ground: "#261007"
  theme-surface: "#1B0C06"
  theme-border: "#481D0F"
  punched-white: "#FAFAFA"
  code-silver: "#EAEAEA"
  silver-ash: "#B8B8B8"
  lyric-dim: "#9E9E9E"
  on-punched-slate: "#404040"
  on-orange-ink: "#111111"
  coverflow-shadow: "rgba(0, 0, 0, 0.72)"
  syntax-sage: "#D4E8C1"
  syntax-amber: "#F5C37A"
typography:
  display:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "clamp(56px, 6.8vw, 92px)"
    fontWeight: 700
    lineHeight: 0.9
    letterSpacing: "-0.02em"
  display-docs:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "clamp(50px, 7.5vw, 88px)"
    fontWeight: 700
    lineHeight: 1.04
    letterSpacing: "-0.02em"
  display-privacy:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "clamp(42px, 6vw, 64px)"
    fontWeight: 700
    lineHeight: 1.08
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "clamp(40px, 4.8vw, 66px)"
    fontWeight: 700
    lineHeight: 0.94
    letterSpacing: "-0.02em"
  headline-docs:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "clamp(34px, 4.2vw, 52px)"
    fontWeight: 700
    lineHeight: 0.96
    letterSpacing: "-0.02em"
  title-lg:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "32px"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "0em"
  title-install:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "30px"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "0em"
  title:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "0.01em"
  title-deck:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "26px"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "0.01em"
  title-card:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "24px"
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: "0em"
  title-route:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "22px"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "0em"
  title-code:
    fontFamily: "JetBrains Mono, ui-monospace, Menlo, monospace"
    fontSize: "20px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.12em"
  title-cell:
    fontFamily: "Antonio, 'Arial Narrow', sans-serif"
    fontSize: "18px"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "0em"
  body-hero:
    fontFamily: "Archivo, system-ui, -apple-system, sans-serif"
    fontSize: "17.5px"
    fontWeight: 400
    lineHeight: 1.62
    letterSpacing: "0em"
  body-lede:
    fontFamily: "Archivo, system-ui, -apple-system, sans-serif"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: 1.65
    letterSpacing: "0em"
  body:
    fontFamily: "Archivo, system-ui, -apple-system, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: "0em"
  body-compact:
    fontFamily: "Archivo, system-ui, -apple-system, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: "0em"
  body-card:
    fontFamily: "Archivo, system-ui, -apple-system, sans-serif"
    fontSize: "14.5px"
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: "0em"
  body-sm:
    fontFamily: "Archivo, system-ui, -apple-system, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: "0em"
  body-steps:
    fontFamily: "Archivo, system-ui, -apple-system, sans-serif"
    fontSize: "13.5px"
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: "0em"
  label-md:
    fontFamily: "JetBrains Mono, ui-monospace, Menlo, monospace"
    fontSize: "13px"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "0.05em"
  label:
    fontFamily: "JetBrains Mono, ui-monospace, Menlo, monospace"
    fontSize: "12px"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "0.06em"
rounded:
  none: "0px"
  brand-icon: "6px"
  hardware-thumb: "8px"
  hardware-row: "10px"
  hardware-tile: "12px"
  hardware-cover: "16px"
  hardware-sheet: "24px"
  hardware-screen: "35px"
  hardware-shell: "44px"
  pill: "999px"
  circle: "50%"
spacing:
  xs: "8px"
  sm: "12px"
  md: "16px"
  lg: "24px"
  xl: "36px"
  hero: "clamp(48px, 6.5vw, 88px)"
  section: "clamp(56px, 7vw, 96px)"
  outro: "56px"
  gutter: "clamp(16px, 3vw, 44px)"
components:
  button-signal:
    backgroundColor: "{colors.grain-orange}"
    textColor: "{colors.ink-black}"
    typography: "{typography.label-md}"
    rounded: "{rounded.none}"
    padding: "16px 24px"
  button-signal-hover:
    backgroundColor: "{colors.grain-orange-hover}"
  button-tape:
    backgroundColor: "{colors.grain-orange}"
    textColor: "{colors.ink-black}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "10px 16px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.punched-white}"
    typography: "{typography.label-md}"
    rounded: "{rounded.none}"
    padding: "16px 22px"
  button-ink:
    backgroundColor: "{colors.ink-black}"
    textColor: "{colors.punched-white}"
    typography: "{typography.label-md}"
    rounded: "{rounded.none}"
    padding: "15px 22px"
  button-bench:
    backgroundColor: "{colors.panel-charcoal}"
    textColor: "{colors.punched-white}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "0 12px"
    height: "38px"
  button-bench-active:
    backgroundColor: "{colors.grain-orange}"
    textColor: "{colors.ink-black}"
  chip-filter:
    backgroundColor: "{colors.ink-black}"
    textColor: "{colors.silver-ash}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "6px 12px"
  chip-filter-active:
    backgroundColor: "{colors.grain-orange}"
    textColor: "{colors.ink-black}"
  card-bench:
    backgroundColor: "{colors.charred-ink}"
    textColor: "{colors.punched-white}"
    rounded: "{rounded.none}"
    padding: "24px"
  coverflow-item:
    backgroundColor: "{colors.charred-ink}"
    textColor: "{colors.punched-white}"
    rounded: "{rounded.none}"
    width: "clamp(200px, 22vw, 276px)"
  input-select:
    backgroundColor: "{colors.ink-black}"
    textColor: "{colors.punched-white}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "9px 12px"
  nav-site:
    backgroundColor: "{colors.ink-black}"
    textColor: "{colors.punched-white}"
    height: "76px"
    padding: "0 clamp(16px, 3vw, 44px)"
  phone-frame:
    backgroundColor: "{colors.ink-black}"
    textColor: "{colors.punched-white}"
    rounded: "{rounded.hardware-shell}"
    padding: "9px"
---

# Design System: AURA

## Overview

**Creative North Star: "Editorial Audio Bench"**

AURA's web presence pairs an uncluttered matte Ink Black (`#0A0A0A`) editorial layout with interactive studio instrumentation: hairline `1px` structural divisions (`#262626` / `#3A3A3A`), a split hero lit by a vertical `180deg` `color-mix` studio backlight bloom (`#080808` edges peaking at `22%` `--orange` behind the center band), a crisp transparent 5-cover 3D Cover Flow stage with dynamic cover-derived `--orange` accent and `--theme-ground` / `--theme-surface` / `--theme-border` theming (`750ms` Houdini `@property` crossfades, persisted via `sessionStorage` `"aura_theme"` across companion pages), a `#tone-toggle`-launched mobile player preview modal (`<dialog id="player-dialog">` replicating AURA's mobile `ControlStyle.BLOOM` player with 3-stop album-tinted gradients, masked word-synced lyrics, Queue drawer, and in-phone Song Menu sheet), real device contact-sheet proofs, a 15-band AutoEq workbench, BitChord FLAC signal routing, pixel-wipe karaoke lyrics, Nostr Listen Together invites, and a kinetic `#outro` `"A U R A"` closing band. Rather than wrapping music features in glossy glass cards or decorative telemetry chrome, the interface clears away superfluous navigation marks so condensed display headlines, primary signal actions, and real audio tools speak directly.

Typography and surfaces maintain stark dark-mode contrast (`color-scheme: dark`, `tabular-nums`, accent `::selection`, `caret-color`, custom scrollbars, and `2px` accent `:focus-visible` outlines with `3px` offset). Transitions and kinetic scroll entrances (`opacity: 1` at rest via WAAPI reveals) decelerate crisply along `cubic-bezier(0.16, 1, 0.3, 1)`, while `@media (prefers-reduced-motion: reduce)` softens color shifts to `240ms` and collapses motion and smooth scrolling to static precision.

The visual system explicitly excludes decorative SaaS ornament: no eyebrow labels above headings, no decorative `01 / 02` section indices, no rounded pill cards, no >1px accent border stripes, and no emojis as UI icons—relying exclusively on crisp uniform-stroke inline SVGs.

**Key Characteristics:**
- Split hero (`.hero-showcase`, `1.05fr / 0.95fr`) bathed in a vertical `180deg` `color-mix` studio backlight bloom (`#080808` → `14%` / `22%` / `10%` `--orange` → `#080808`), pairing staggered `EVERY SONG. ZERO ADS.` typography with a transparent interactive 5-item 3D `.coverflow-orbit` stage, 42-bar `.hero-wave` scrubber, live `.hero-deck` controls, and the `#tone-toggle`-launched `<dialog id="player-dialog">` AURA mobile player replica
- Minimal sticky navigation (`.site-nav`) with `#readline` horizontal scroll progress bar, clean logo + wordmark, centered uppercase mono links, GitHub star pill, and a single solid primary CTA
- Strictly rectilinear (`0px` radius) chassis, cards, buttons, inputs, and Cover Flow tiles—limiting rounded geometry solely to `6px` on `.brand img` and the concentric mobile hardware replica inside `<dialog id="player-dialog">` (`44px` `.phone-frame`, `35px` `.phone-screen`, `24px` `.pm-sheet-card` top corners, `16px` `.pm-cover-wrap`, `12px` `.pm-sheet-tile`, `10px` list/sheet rows, `8px` inner thumbs, `999px` pills/morph bar, and `50%` circular controls)
- Dynamic single-signal accent and deep cover-tinted surface system defaulting to Grain Orange (`#FF5F26`) and Ember Brown (`#261007` / `#1B0C06` / `#481D0F`) via `@property` tokens (`--orange*`, `--theme-ground`, `--theme-surface`, `--theme-border`) that crossfade per centered album and persist via `sessionStorage` `"aura_theme"`, paired with 3-stop `--pm-top` / `--pm-mid` / `--pm-bot` player washes (`168deg`) inside `<dialog id="player-dialog">` and reserving Punched White (`#FAFAFA`) exclusively for selected device proof frames and staged mats
- Uniform-stroke inline SVG iconography, custom dark browser chrome, always-visible WAAPI scroll reveals (`opacity: 1` at rest), and immediate reduced-motion static fallbacks

## Colors

A monochrome matte-ink workbench punctuated by a single signal accent family—defaulting to Grain Orange (`#FF5F26`)—and paired dark cover-tinted grounds/surfaces registered as Houdini `@property` color tokens (`--orange`, `--orange-soft`, `--orange-hover`, `--theme-ground`, `--theme-surface`, `--theme-border`) alongside `--orange-dim` so centered Cover Flow albums crossfade the hero studio backlight, accents, and chassis surfaces over `750ms` and persist across pages via `sessionStorage` (`aura_theme`). Inside `#player-dialog`, `.phone-screen` shifts smoothly (`420ms`) across track-specific 3-stop `--pm-top` / `--pm-mid` / `--pm-bot` (`168deg`) gradients set in `pmLoadTrack()`.

### Primary
- **Grain Orange** (`#FF5F26`, `--orange` default): Primary CTA fills (`.btn-signal`, `.tape-btn`, `addons/` `.btn-primary`, `together/` `#open`), split `<mark>` headline emphasis, `#readline` progress bar, filled 15-band AutoEq meters, `.deck-swatch`, active Cover Flow center border, active checkboxes, and active tab/button fills (`6.52:1` contrast with Ink Black).
- **Grain Orange Hover** (`#FF713E`, `--orange-hover` default): Brightened hover state on solid Grain Orange buttons (`.btn-signal`, `.tape-btn`, `addons/` `.btn-primary`, `together/` `#open`).
- **Soft Coral Orange** (`#FF8759`, `--orange-soft` default): High-legibility accent text and stroke companion on dark grounds (`8.35:1` on Ink Black, `7.84:1` on Charred Ink) used for inline links, route icons, code tokens, room codes, version strips, `#deck-hex` / `#outro-hex` readouts, and dark-background hover text.
- **Orange Dim Wash** (`rgba(255, 95, 38, 0.14)`, `--orange-dim` default): Tinted highlight wash for the active tuned radio row (`.station-row[data-tuned="true"]`; subpage callouts and ghost hovers use `0.08`–`0.12` alpha).
- **Cover-Derived Dynamic Palettes (`@property --orange*`, `--theme-ground`, `--theme-surface`, `--theme-border`):** Selecting any centered `.coverflow-item` smoothly interpolates root accent and surface tokens over `750ms cubic-bezier(0.16, 1, 0.3, 1)` (`650ms` on `.eq-fill` meters, `240ms` under reduced motion). `.coverflow-stage` renders with a completely transparent background (`--hero-glow` removed for crisp cover edges) above `.hero-showcase`'s vertical studio backlight gradient (`linear-gradient(180deg, #080808 0%, color-mix(in oklab, var(--orange) 14%, #0A0A0A) 38%, color-mix(in oklab, var(--orange) 22%, #0D0D0D) 58%, color-mix(in oklab, var(--orange) 10%, #0A0A0A) 78%, #080808 100%)`), while `--theme-surface` / `--theme-border` color elevated hardware chassis (`.screen-stage`, `.eq-chassis`, primary `.install-col:first-child`, and `addons/` `.route`) and `{ color, soft, hover, dim, ground, surface, border }` persists to `sessionStorage` (`aura_theme`) across landing, `addons/`, `together/`, and `privacy/`:
  - `Future Nostalgia`: accent `#EC65AA` / `#FF92C8` / `#F57EB9` (`0.16` dim), ground `#28111D`, surface `#1E0D16`, border `#4A1E35`; mobile player `168deg` wash `--pm-top` `#6E234B` -> `--pm-mid` `#3E182E` -> `--pm-bot` `#1B0D16`
  - `After Hours` *(default)*: accent `#FF5F26` / `#FF8759` / `#FF713E` (`0.14` dim), ground `#261007`, surface `#1B0C06`, border `#481D0F`; mobile player `168deg` wash `--pm-top` `#5E251B` -> `--pm-mid` `#3A211B` -> `--pm-bot` `#1A1211` (`#1B1312` CSS fallback)
  - `AM`: accent `#A8BCD6` / `#C9D7EA` / `#BCCCE2` (`0.16` dim), ground `#121720`, surface `#0E1219`, border `#283344`; mobile player `168deg` wash `--pm-top` `#25313E` -> `--pm-mid` `#182029` -> `--pm-bot` `#0E1217`
  - `Harry's House`: accent `#4DB5F0` / `#82CEFA` / `#6DC3F4` (`0.16` dim), ground `#0B1B26`, surface `#08141D`, border `#1A384E`; mobile player `168deg` wash `--pm-top` `#1C4F6E` -> `--pm-mid` `#133245` -> `--pm-bot` `#0B1922`
  - `American IV`: accent `#DE913B` / `#F3B66F` / `#E7A252` (`0.16` dim), ground `#24180A`, surface `#191107`, border `#442E15`; mobile player `168deg` wash `--pm-top` `#64291B` -> `--pm-mid` `#442C24` -> `--pm-bot` `#1A1311`
  - Ambient `color-mix(in oklab, var(--orange) ...)` mixes drive `.hero-showcase` (`180deg` `14%` / `22%` / `10%` studio backlight), `.eq-spec` (`22%`), and `.lyrics-bench` (`12%`).

### Neutral
- **Ink Black** (`#0A0A0A`): Primary page canvas (`html`, `body`), `.phone-frame` hardware shell (`border: 1px solid rgba(255, 255, 255, 0.14)`), recessed sub-cells (`.ver-strip`, `.eq-track`, `.chip-btn`, `.btn-ink`), and foreground ink inside Grain Orange fills.
- **Studio Void** (`#080808`): Deepest cutout/edge stop used on `.phone-island` and `.hero-showcase` top/bottom backlight edges.
- **Charred Ink** (`#131313`): Primary modular card and chassis surface (`.coverflow-item`, `.lyrics-bench`, `.radio-bench`, secondary `.install-col`, `.screen-cell`, `.endpoint`, `.code`, `together/` card, `privacy/` header) and `.pm-dock-circle[aria-pressed="true"]` foreground ink inside `#player-dialog`.
- **Theme Ground** (`#261007`, `--theme-ground` default): Houdini `@property`-registered cover-tinted ground token synced across album palettes and `sessionStorage` (`aura_theme`), paired with `.hero-showcase`'s `180deg` `color-mix` studio backlight bloom between `#080808` edges.
- **Theme Surface** (`#1B0C06`, `--theme-surface` default): Houdini `@property`-registered deep tinted hardware chassis fill for `.screen-stage`, `.eq-chassis`, primary `.install-col:first-child`, and `addons/` `.route`.
- **Theme Border** (`#481D0F`, `--theme-border` default): Houdini `@property`-registered `1px` perimeter rule pairing with `--theme-surface` on elevated hardware chassis and `addons/` `.route`.
- **Panel Charcoal** (`#181818`): Secondary dark control surface for `.bench-btn` deck controls (`addons/` uses `#1A1A1A` on `.copy` buttons and `.rule:hover`).
- **Row Hover** (`#1F1F1F`): Subtle surface step on hovered radio station rows (`.station-row:hover`).
- **Sheet Charcoal** (`#1B1B1E`): Elevated frosted bottom-sheet surface on `.pm-sheet-card` (`border-top: 1px solid rgba(255, 255, 255, 0.12)`) inside `<dialog id="player-dialog">`.
- **Hairline Rule** (`#262626`): Primary `1px` structural dividers across `.site-nav`, `.hero-showcase`, `.hero-deck`, `.section-band`, `.band-head`, `#outro`, and `.spec-table`.
- **Rule Highlight** (`#3A3A3A`): Emphasized `1px` borders on outer cards, `.btn-ink`, `.btn-outline`, `.bench-btn`, `.coverflow-item`, form controls, and scrollbar thumbs.
- **Punched White** (`#FAFAFA`): Primary display/body text on dark backgrounds (`18.97:1` on Ink Black) and matte-white cutout frames reserved exclusively for active device proof windows (`.screen-cell[aria-pressed="true"]`, `.stage-punched`).
- **Code Silver** (`#EAEAEA`): Crisp light neutral foreground inside `addons/` `<pre>` code blocks.
- **Silver Ash** (`#B8B8B8`): Secondary body paragraphs, navigation links, `.hero-specs`, spec keys, table headers, and footer notes (`9.98:1` on Ink Black, `9.37:1` on Charred Ink).
- **Lyric Dim** (`#9E9E9E`): Inactive synced lyric stanza lines on Charred Ink before advancing to Punched White (`6.94:1` on `#131313`).
- **On-Punched Slate** (`#404040`): Secondary cell label inside active Punched White device proof tiles (`9.93:1` on `#FAFAFA`).
- **On-Orange Ink** (`#111111`): Secondary body copy inside `addons/` `.cta-band` Grain Orange field (`6.22:1` on `#FF5F26`).
- **Cover Flow Shadow** (`rgba(0, 0, 0, 0.72)`): Deep shadow scrim beneath the centered 3D Cover Flow tile (`[data-pos="0"]`).
- **Syntax Sage & Amber** (`#D4E8C1`, `#F5C37A`): Syntax-highlighting tokens for JSON string values and primitive literals inside `addons/` code examples only.

### Named Rules
**The Binary Signal Rule.** The interface uses one active chromatic signal family at a time—defaulting to Grain Orange (`#FF5F26` / `#FF8759` / `#FF713E`) paired with low-luminance cover-tinted surfaces (`--theme-ground`, `--theme-surface`, `--theme-border`)—and dynamically crossfading via Houdini `@property` tokens to match centered Cover Flow album art (`Future Nostalgia`, `After Hours`, `AM`, `Harry's House`, `American IV`) with `sessionStorage` `"aura_theme"` persistence across companion pages; album artwork never introduces competing multi-accent chrome.

**The Punched Paper Proof Rule.** Punched White (`#FAFAFA`) surfaces appear strictly as active contact-sheet frames around real device screenshots (`.screen-cell[aria-pressed="true"]`, `.stage-punched`), never as generic background cards.

**The Orange-Ink Inversion Rule.** Text and borders layered directly over Grain Orange (or active cover-derived `--orange`) fills always switch to Ink Black (`#0A0A0A` or `#111111`, `≥6.22:1` contrast) and never use white text on the signal fill.

## Typography

**Display Font:** `Antonio` (with `"Arial Narrow", sans-serif` fallback)
**Body Font:** `Archivo` (with `system-ui, -apple-system, sans-serif` fallback)
**Label/Mono Font:** `JetBrains Mono` (with `ui-monospace, Menlo, monospace` fallback)

**Character:** Condensed all-caps `Antonio` headlines provide strong editorial density (`0.9–1.08` line-height), balanced by clean `Archivo` reading copy and crisp `JetBrains Mono` buttons, code blocks, and tabular telemetry.

### Hierarchy
- **Display** (`700`, `clamp(56px, 6.8vw, 92px)` landing / `50px` mobile; `clamp(50px, 7.5vw, 88px)` Add-ons; `clamp(42px, 6vw, 64px)` Privacy, line-height `0.9–1.08`, `-0.02em` letter-spacing, uppercase): Hero `<h1>` statements across portal pages.
- **Headline** (`700`, `clamp(40px, 4.8vw, 66px)` landing / `40px` Together; `clamp(34px, 4.2vw, 52px)` Add-ons, line-height `0.94–0.96`, `-0.02em` letter-spacing, uppercase): Section band titles (`<h2>`) with optional Grain Orange `<mark>` emphasis.
- **Title** (`700`, `18px–32px`, line-height `1–1.15`, `0–0.01em` letter-spacing, uppercase): Workbench headings (`.stage-info h3` / `.cta-band h3` `32px`, `.install-col h3` `30px`, `.eq-top h3` / `.lyrics-head h3` / `.radio-filter h3` / `privacy/` `h2` `28px`, `.deck-meta h2` `26px`, `addons/` `h3` / footer title `24px`, `.route strong` / `.rule strong` `22px`, `.screen-cell-foot strong` `18px`), synced lyric stanzas (`clamp(24px, 2.5vw, 32px)`, sentence case), `together/` room `#code` (`20px` bold mono, `0.12em` tracking), plus mobile modal headings inside `#player-dialog` (`.pm-track-meta h3` `20px` / `18px` `<540px`, `.pm-lyric-line` `18px`, `.pm-mini-copy strong` `16px`, `.pm-queue-heading` `15px`).
- **Body** (`400`, `13.5px–17.5px`, line-height `1.55–1.72`, max-width `48ch–68ch`; `privacy/` `p, li` `max-width: 58ch`, `text-wrap: pretty`): `.hero-lede` (`17.5px`, `15.5px` mobile), `addons/` `.lede` (`17px`), base copy (`16px`–`16.5px`), `.stage-info p` (`15px`), `.install-col p` / `.rule p` (`14.5px`), footer prose (`14px`) and modal metadata/sheet rows (`.pm-track-meta p`, `.pm-queue-copy strong`, `.pm-sheet-head-meta strong`, `.pm-sheet-row` `14px`), and `.steps-list` (`13.5px`).
- **Label** (`500–700`, `12px–13px`, line-height `1`, `0.04em–0.08em` letter-spacing, uppercase on navigation and CTAs): Primary hero/install buttons, `#pm-album` / `.pm-mini-copy span`, and `.chain-step` / `<pre>` text (`13px`), `.hero-specs` / `.station-table` / `.changelog-details` / `.pm-queue-copy span` (`12.5px`), and nav links, `.tape-btn`, `.bench-btn`, `.chip-btn`, `.spec-row`, `.ver-strip`, `.pm-from span`, `.pm-times`, `.pm-codec-tag`, `.pm-queue-dur`, `.pm-sheet-tile`, `.pm-repeat-badge` (`12px`).

### Named Rules
**The Condensed Uppercase Display Rule.** All `Antonio` display headings and panel titles set uppercase with compressed leading (`0.9–1.08`) and tight negative tracking (`-0.02em` on display/headline tiers) so statements anchor crisply above `1px` rules.

**The Tabular Telemetry Rule.** `body` enforces `font-variant-numeric: tabular-nums` globally so bitrate specs, sample rates, star badges, EQ frequencies, modal timestamps (`.pm-times`), and APK file sizes never shift horizontally during updates.

## Layout

The portal arranges content within a centered `1440px` editorial grid (`1280px` on `addons/`, `780px` reading column on `privacy/`, `460px` centered invite card on `together/`), using `1px solid #262626` section rules to partition bands cleanly.

- **Hero Showcase (`.hero-showcase`):** `minmax(0, 1.05fr) minmax(0, 0.95fr)` split (`gap: clamp(36px, 5vw, 72px)`, `padding: clamp(48px, 6.5vw, 88px) clamp(16px, 3vw, 44px)`) placing open display copy opposite the `3D Cover Flow` stage and `.hero-deck` bar.
- **Section Bands (`.section-band`):** Padded `clamp(56px, 7vw, 96px)` vertically and `clamp(16px, 3vw, 44px)` horizontally, opening with a `.band-head` grid (`1.15fr / 0.85fr`, `24px 48px` gap).
- **Kinetic Outro Band (`#outro` / `.outro-band`):** Centered `1440px` closing section padded `56px` vertically (`clamp(16px, 3vw, 44px)` horizontally) above a `1px solid #262626` bottom rule, framing the spaced `.outro-word` (`A U R A`, `gap: clamp(16px, 4.5vw, 64px)`) and `.outro-meta` hex telemetry (`12.5px` mono).
- **Module Grids:**
  - `.contact-layout`: `1.15fr / 0.85fr` split pairing a `3`-column `.proof-grid` with a sticky `.screen-stage` (`top: 96px` at `>=1121px`, `minmax(180px, 240px) minmax(0, 1fr)`).
  - `.audio-bench`: `1.05fr / 0.95fr` split pairing the `15`-band `.eq-bars` chassis (`8px` gap, `190px` tall) with `.signal-bench`.
  - `.social-grid` & `.install-grid`: `0.92fr / 1.08fr` lyrics/radio split and `3`-column `.install-grid` (`16px` gap).
  - `addons/` Documentation Grid: `220px` sticky `.toc` sidebar beside the protocol guide (`gap: clamp(28px, 5vw, 64px)`).
- **Responsive Adaptation:**
  - `1120px`: Collapses `.hero-showcase`, `.band-head`, `.contact-layout`, `.audio-bench`, `.social-grid`, and `.install-grid` to `1` column.
  - `900px` (`addons/`): Stacks hero and guide layout to `1` column; turns `.toc` into a horizontal wrap bar pinned below the top nav.
  - `820px`: Switches `.site-nav-inner` to `68px` flex bar hiding `.nav-links`, converts `.proof-grid` to `2` columns, and stacks `.screen-stage` and `.site-footer`.
  - `600px` (`addons/`): Collapses `.rules` `2`-column matrix to `1` column and compacts `<pre>` padding (`14px`, `12px` font).
  - `540px`: Tightens `.hero-showcase` padding (`18px 16px 28px`, `gap: 20px`), sets `.hero-title` to `50px`, reduces `.coverflow-stage` height to `198px` and `.coverflow-item` width to `162px` (hiding outer `±2` depth tiles), stacks `.source-switch` and `.chain-step`, hides `.eq-hz` labels and `.star-pill`, sets `.eq-bars` height to `140px`, and inside `#player-dialog` sets `.phone-frame` width to `min(334px, calc(100vw - 20px))`, `.phone-screen` padding to `10px 16px 12px` (`gap: 8px`), `.pm-track-meta h3` to `18px`, `.pm-bloom-btn` to `50px` (`30px` SVG), and `.pm-play-btn` to `58px` (`38px` SVG).
  - `max-height: 740px`: Compacts `#player-dialog` (`.phone-screen` `10px 18px 10px`, `gap: 7px`; `.pm-cover-wrap` `min(100%, clamp(160px, 30vh, 240px))`; `.pm-lyrics-pane, .pm-queue-pane` `clamp(200px, 35vh, 280px)`) so full playback controls stay visible on short viewports.

## Elevation & Depth

Surfaces are flat by default. Depth is conveyed through dark luminance stepping (`#0A0A0A` canvas → `#131313` bench chassis or `var(--theme-surface)` elevated hardware chassis → `#181818` secondary controls) and `1px` borders (`#262626` structural rule, `#3A3A3A` interactive perimeter, `var(--theme-border)` themed chassis perimeter, `var(--orange)` active accents). Sticky headers (`.site-nav`, `addons/` `.nav`) use `rgba(10, 10, 10, 0.94)`—deepening to `rgba(10, 10, 10, 0.97)` on `.site-nav.is-scrolled`—with `backdrop-filter: blur(10px)` solely so scrolled sections remain crisp beneath the `1px` bottom rule, while `.phone-dialog::backdrop` (`rgba(6, 6, 6, 0.84)`, `backdrop-filter: blur(8px)`) dims and blurs the page behind `<dialog id="player-dialog">`.

### Shadow Vocabulary
- **Center Cover Flow Tile** (`box-shadow: 0 24px 48px rgba(0, 0, 0, 0.72)`): Applied to the active front Cover Flow tile (`.coverflow-item[data-pos="0"]`) to separate it in 3D space above recessed flankers (`±1`, `±2`).
- **Mobile Player Frame** (`box-shadow: 0 32px 72px rgba(0, 0, 0, 0.9)`): Elevated hardware shadow beneath `.phone-frame` (`9px` padding, `1px solid rgba(255, 255, 255, 0.14)`) inside `<dialog id="player-dialog">` above `.phone-dialog::backdrop`.
- **Mobile Player Album Sleeve** (`box-shadow: 0 20px 44px rgba(0, 0, 0, 0.48)`): Depth lift under the borderless `16px`-rounded `.pm-cover-wrap` artwork inside `.phone-screen`.
- **Mobile List Header Mini Cover** (`box-shadow: 0 8px 18px rgba(0, 0, 0, 0.35)`): Subtle lift under the clickable `52px × 52px` `.pm-mini-cover-btn` inside Lyrics and Queue views.
- **Mobile Scrubber Thumb** (`box-shadow: 0 2px 6px rgba(0, 0, 0, 0.45)`): Micro depth under the `12px` circular `.pm-range` thumb inside `#player-dialog`.

### Named Rules
**The Hairline Matrix Rule.** Resting panels, cards, grids, and tables never cast drop shadows; separation comes strictly from `1px` hairline borders (`#262626` / `#3A3A3A` / `var(--theme-border)`) and a single tonal step between Ink Black (`#0A0A0A`) and Charred Ink (`#131313`) or cover-tinted `--theme-surface` (`#1B0C06` default).

**The Earned Lift Rule.** Box shadow elevation appears solely on the active centered Cover Flow hero tile (`.coverflow-item[data-pos="0"]`) and the concentric mobile hardware replica inside `<dialog id="player-dialog">` (`.phone-frame`, `.pm-cover-wrap`, `.pm-mini-cover-btn`, and `.pm-range` slider thumbs); all resting workbench cards, tables, and page controls rely strictly on border color or `1px–2px` transform shifts.

## Shapes

Every editorial container, workbench card, button, tab, input, table, `<details>` disclosure, and Cover Flow tile maintains sharp rectilinear geometry (`0px` corner radius). Rounded corners are scoped exclusively to `6px` on the `28px × 28px` AURA app icon thumbnail (`.brand img`) and the concentric mobile hardware replica inside `<dialog id="player-dialog">`: `44px` outer `.phone-frame` shell, `35px` inner `.phone-screen` viewport (`44px - 9px`), `24px 24px 0 0` `.pm-sheet-card` bottom sheet top corners, `16px` borderless `.pm-cover-wrap` artwork, `12px` `.pm-sheet-tile` Song Menu tiles, `10px` `.pm-mini-cover-btn` / `.pm-queue-item` / `.pm-sheet-row` list controls, `8px` `.pm-queue-item img` / `.pm-sheet-head img` thumbnails, `999px` `.phone-island` (`88px × 22px`) / `.pm-grab-btn::before` (`40px × 4px`) / `.pm-range` (`4px` height) / `.pm-codec-tag` / `.pm-morph-pill` (`44px` tall, `max-width: 176px`) / `.phone-homebar` (`108px × 4px`) / `.pm-sheet-handle` (`36px × 4px`) / `.pm-sheet-close` (`32px`) pills, and `50%` circular controls (`.phone-island::after` `7px`, `.pm-close-x` `32px`, `.pm-glass-circle` `40px`, `.pm-range` thumbs `12px`, `.pm-bloom-btn` `58px` / `50px` mobile, `.pm-play-btn` `68px` / `58px` mobile, and `.pm-dock-circle` `44px`) confined strictly within `.phone-frame`.

Depth and motion choreography:
- **3D Cover Flow Perspective & Tilt:** `.coverflow-stage` uses `perspective: 1100px`, `touch-action: pan-y`, and a transparent background (`--hero-glow` removed for crisp album edges) wrapping `.coverflow-orbit` (`preserve-3d`) above `.hero-showcase`'s `180deg` `color-mix` vertical backlight bloom, combining eased (`0.08`) pointer tilt (`±4deg` X / `±5.5deg` Y), scroll float (`-24px`), pointer swipe (`>36px`), and `900ms` center artwork hover zoom (`scale(1.03)`).
- **Motion Curve & Entrances:** Transitions follow `cubic-bezier(0.16, 1, 0.3, 1)` (`180ms` controls, `280ms` EQ meters / `#readline`, `360ms` Cover Flow tiles, `650ms` `.eq-fill` accent color, `750ms` `@property` `--orange*`, `--theme-*`, and `.hero-showcase` gradient shifts). Hero elements play once (`line-up` `950ms` at `40ms + i*90ms`, `fade-up` `850ms` at `140/220/290ms`, `stage-in` `950ms` at `100ms`), below-fold modules stay visible (`opacity: 1`) at rest and trigger WAAPI high-water reveals (`0.28 -> 1`, `translate3d(0, 22px, 0) -> 0`, `580ms`, `55ms` stagger when `scrollY > 12`), and `@media (prefers-reduced-motion: reduce)` limits root color shifts to `240ms`, collapses animations/transitions to `0.01ms`, and fixes hero nodes to static `opacity: 1; transform: none`. Inside `<dialog id="player-dialog">`, `phonePop` enters over `240ms cubic-bezier(0.22, 1, 0.36, 1)` (`translateY(18px) scale(0.96)` -> `0, 1`), `.phone-screen` transitions background over `420ms var(--ease-snap)`, `.pm-cover-wrap` scales `0.94 -> 1` when `.is-playing` over `340ms cubic-bezier(0.22, 1, 0.36, 1)`, `.pm-mini-cover-btn` hovers over `160ms var(--ease-snap)`, `.pm-sheet` fades in over `180ms ease both` (`fadeInSheet`), and reduced-motion progress advances via `250ms` `stillTimer` intervals.

## Components

Every component pairs flat hairline structure or bold signal fills with high-contrast monospace labels and sharp focus rings (`2px solid var(--orange)`, `outline-offset: 3px`).

### Buttons
- **Shape:** Sharp rectilinear edges (`0px` radius).
- **Primary Signal & Tape:** `.btn-signal` (`16px 24px` padding, `13px` bold uppercase mono) and `.tape-btn` (`10px 16px`, `12px` bold uppercase mono) fill solid `var(--orange)` (`#FF5F26` default) with Ink Black (`#0A0A0A`) ink and `1px solid var(--orange)` border (`addons/` `.btn-primary` uses `48px` height `0 22px`; `together/` `#open` fills full width `15px 18px`). Hover brightens to `var(--orange-hover)` (`#FF713E`) with `-1px` vertical lift.
- **Outline & Ink Secondary:** `.btn-outline` (`transparent`, `1px solid #3A3A3A`, `#FAFAFA` ink, `16px 22px`) shifts border to `var(--orange)` and text to `var(--orange-soft)` on hover. `.btn-ink` (`#0A0A0A` fill, `#FAFAFA` text, `15px 22px`, `1px solid #3A3A3A` border) lifts to `#1E1E1E` with `var(--orange)` border on hover (`addons/` `.cta-band .btn` pairs solid `#0A0A0A` over the signal CTA band).
- **Bench Controls & Mini Links:** `.bench-btn` (`#181818`, `38px` height, `0 12px` padding, `1px solid #3A3A3A`, `14px` stroke SVG icon) flips to `var(--orange)` fill and Ink Black text on `[aria-pressed="true"]`. Compact `.mini-link` (`#0A0A0A`, `8px 12px`) handles APK/desktop/SideStore asset links.

### Chips & Segmented Controls
- **Style:** Sharp `0px` corners, Ink Black (`#0A0A0A`) fill, `1px solid #3A3A3A` border, `6px 12px` padding, `12px` `JetBrains Mono` in Silver Ash (`#B8B8B8`) (`.chip-btn`, `addons/` `.fields li`).
- **State:** Active `[aria-selected="true"]` chips flip to `var(--orange)` fill, `var(--orange)` border, and bold Ink Black text. `.source-switch` renders a `3`-column hairline-divided segmented switch for audio output paths.

### Cards / Containers
- **Corner Style:** Sharp `0px` across all panels and chassis containers.
- **Background:** Charred Ink (`#131313`) on Ink Black (`#0A0A0A`), stepping to cover-tinted `var(--theme-surface)` (`#1B0C06` default) on elevated hardware chassis (`.screen-stage`, `.eq-chassis`, primary `.install-col:first-child`, and `addons/` `.route`), while nested sub-blocks (`.chain-step`, `.nostr-room`, `.ver-strip`, `.endpoint header`) step down to `#0E0E0E`, `#101010`, or `#0A0A0A`.
- **Shadow Strategy:** Flat by default; zero shadows at rest.
- **Border:** `1px solid var(--theme-border)` (`#481D0F` default) on elevated hardware chassis (`.screen-stage`, `.eq-chassis`, `.install-col:first-child`, `addons/` `.route`), `1px solid #3A3A3A` on neutral standalone panels (`.lyrics-bench`, `.radio-bench`, secondary `.install-col`, `.endpoint`, `.code`, `together/` `main`), and `1px solid #262626` on hairline grids (`.rules`).
- **Internal Padding:** `16px–28px` (`24px` on workbench cards, `28px` on `.install-col` and `together/` body, `20px` on `.screen-stage` and `.chain-box`).

### Inputs / Fields & Interactive Controls
- **Style:** `.headphone-select` uses Ink Black (`#0A0A0A`), `1px solid #3A3A3A`, `9px 12px` padding, `12px` mono, `0px` radius. Vertical 15-band `.eq-track` sliders use `#0B0B0B` wells with `1px solid #262626` borders, `ns-resize` cursor, keyboard `role="slider"` support, and `var(--orange)` level fills. `addons/` `.checklist` uses `20px × 20px` rectilinear checkboxes flipping to `var(--orange)` with Ink Black checkmarks when `:checked`.
- **Focus:** `2px solid var(--orange)` outline with `3px` outline-offset.

### Navigation
- **Sticky Site Nav & `#readline`:** `.site-nav` (`76px` desktop / `68px` `<820px`) renders `rgba(10, 10, 10, 0.94)` (`0.97` when `.is-scrolled`) with `backdrop-filter: blur(10px)` and a `1px solid #262626` bottom border on landing (`1px solid var(--orange)` on `addons/` `.nav` and static `780px` Charred Ink `privacy/` `.rail-top`). A `2px` bottom `#readline` indicator (`background: var(--orange)`) scales horizontally `0..1` along scroll progress (`280ms` opacity fade). Landing displays `28px` SVG icon (`6px` radius) + `AURA` (`AURA ADD-ONS` on `addons/`, `← AURA` on `privacy/`), centered `12px` uppercase mono links with `1px` accent underline, `.star-pill`, and `.tape-btn`.
- **Sticky Sidebar TOC (`addons/`):** `220px` sidebar with `1px solid #262626` left border; active `[aria-current="true"]` fills solid `var(--orange)` with bold Ink Black text.

### Signature Components
- **3D Cover Flow Stage & Minimalist Deck (`.hero-visual`):** Five `1:1` square `.coverflow-item` artwork tiles sit inside `.coverflow-orbit` (`perspective: 1100px`, `[data-pos="-2..2"]`, pointer/scroll 3D tilt + `>36px` swipe). Centered `[data-pos="0"]` carries `1px solid var(--orange)` border, `0 24px 48px rgba(0, 0, 0, 0.72)` shadow, dynamically shifts root `@property` accent and surface tokens (`--orange*`, `--theme-ground`, `--theme-surface`, `--theme-border`) across the 5 album palettes (persisting `aura_theme`), and drives `.hero-deck`: `26px` title, `.deck-swatch` (`9px × 9px`) + `#deck-hex` readout, clickable/keyboard `role="slider"` `42`-bar `.hero-wave` progress strip (`6800ms` auto-advance, `9000ms` manual / `5000ms` scrub hold), and `<` / `Preview` / `>` `.bench-btn` controls where `#tone-toggle` (`aria-haspopup="dialog"`, `aria-controls="player-dialog"`) opens the AURA mobile player preview modal.
- **Mobile Player Preview Modal (`<dialog id="player-dialog">` / `.phone-dialog`):** Replicates AURA's mobile app (`ControlStyle.BLOOM`, `01-player.jpg`, `02-lyrics.jpg`, `song_menu_full.png`): a `44px`-radius `.phone-frame` shell (`9px` padding, `#0A0A0A`, `1px solid rgba(255, 255, 255, 0.14)`, `0 32px 72px rgba(0, 0, 0, 0.9)`) over a `rgba(6, 6, 6, 0.84)` `blur(8px)` backdrop encloses an edge-to-edge `35px`-radius `.phone-screen` (`12px 20px 12px` padding, `10px` gap, `168deg` album-derived `--pm-top` -> `--pm-mid` -> `--pm-bot` gradient set by `pmLoadTrack`). Top chrome pairs an `88px × 22px` `.phone-island` (`999px`, `7px` lens dot), clickable `40px × 4px` `.pm-grab-btn` (`#pm-grab-close`), centered `PLAYING FROM` + `#pm-album`, and `32px` `#pm-close-x` (`50%`). Cover view renders borderless `16px`-rounded `#pm-cover` (`.pm-cover-wrap` `0 20px 44px rgba(0, 0, 0, 0.48)`, `scale(0.94 -> 1)` on `.is-playing`), `.pm-track-row` (`20px` `#pm-title`, `14px` `#pm-artist`), and two `40px` `50%` translucent `.pm-glass-circle` buttons (`#pm-fav` Heart and `#pm-more` `•••`). `[data-view="lyrics"]` and `[data-view="queue"]` hide `.pm-cover-wrap` and `.pm-track-row` via CSS and surface `.pm-list-header` (clickable `52×52` `10px`-rounded `.pm-mini-cover-btn` `#pm-lyrics-back`/`#pm-q-back`, `.pm-mini-copy`, Heart `#pm-lyrics-fav`/`#pm-q-fav`, More `#pm-lyrics-more`/`#pm-q-more`) above edge-masked `18px` word-synced lyrics (`.pm-lyric-line` / `#FFFFFF` `.pm-word-lit`) or `10px` `.pm-queue-item` rows (`42×42` `8px` thumbs). Below sit `#pm-seek` (`4px` `999px` track with `#FFFFFF` fill + `12px` `50%` thumb), timestamps + clickable `999px` `#pm-codec` (`FLAC` sample rate <-> `Opus · 251kbps`), bare solid-white `ControlStyle.BLOOM` transport controls (`58px` `#pm-prev` `◀◀`, bare `68px` `#pm-play` `▶`/`❚❚`, `58px` `#pm-next` `▶▶`), and `.pm-bottom-dock` (`44px` `50%` `.pm-dock-circle#pm-btn-lyrics`, `999px` `.pm-morph-pill` split by `1px` `.pm-pill-div` into `#pm-shuffle` & `#pm-repeat` with `#pm-repeat-one`, and `44px` `50%` `.pm-dock-circle#pm-btn-queue`). Tapping More (`•••`) opens the frosted dark Song Menu bottom sheet (`.pm-sheet#pm-sheet`, `#1B1B1E` `.pm-sheet-card`, `24px` top radius) with 4 `12px`-rounded `.pm-sheet-tile` buttons (`#pm-sh-like`, `#pm-sh-qual`, `#pm-sh-lyrics`, `#pm-sh-queue`) and 4 `10px`-rounded `.pm-sheet-row` items (`#pm-sh-shuffle`, `#pm-sh-repeat`, `#pm-sh-cover`, `#pm-sh-done`), framed above a `108px × 4px` `.phone-homebar`. Track selection syncs Cover Flow (`setCoverFlow(idx, true)`), `@property` palettes, `sessionStorage` `"aura_theme"`, and `.hero-wave`; `Escape` dismisses `#pm-sheet` first when open, or closes `#player-dialog` alongside `#pm-grab-close`, `#pm-close-x`, and backdrop clicks, restoring focus to `#tone-toggle`.
- **Contact-Sheet Screen Inspector:** `3×2` `.proof-grid` of real device captures (`9/15.5` portrait crop; `#131313` default → `#FAFAFA` when `aria-pressed="true"`) paired with sticky `.screen-stage` (`top: 96px` at `>=1121px`, `--theme-surface` / `--theme-border`, `.is-swapping` `0.35` opacity / `scale(0.985)` swap) rendering the `9/16.5` capture inside a `10px` `.stage-punched` mat with `-18px` scroll parallax beside a hairline `.spec-table`.
- **Audio Workbench, Synced Lyrics & Kinetic Outro:** `.eq-chassis` (`--theme-surface` / `--theme-border`) pairs interactive `15`-band AutoEq tracks with an `.eq-spec` reactive `22%` `color-mix` spectrum backdrop, while `.signal-bench` cascades `.chain-step.is-bump` pulses (`55ms` stagger, `280ms`, `translateX(4px)`). `.lyrics-bench` renders a radial `12%` `color-mix` glow and pixel-wipe `.k-word::after` lines (`clip-path: inset(...)` with `is-em`, `is-held`, `style-bounce` KuGou, `1.15x` NetEase pacing, and `0.62` `is-past`). `#outro` / `.outro-band` closes the page with kinetic `A U R A` `.outro-word` letter motion (`clamp(56px, 6.8vw, 92px)`) and `#outro-hex`.
- **Protocol Endpoints & Code Blocks (`addons/`):** `.endpoint` and `.code` blocks in Charred Ink (`#131313`, `1px solid #3A3A3A`) feature `#0E0E0E` method headers (`GET` badge in `var(--orange)` / `#0A0A0A`), corner `.copy` toggles, and JSON syntax tokens (`var(--orange-soft)` keys, `#D4E8C1` strings, `#F5C37A` primitives).

## Do's and Don'ts

Concrete system guardrails verified across the shipped portal codebase.

### Do:
- **Do** use Ink Black (`#0A0A0A`), Charred Ink (`#131313`), cover-derived `--theme-ground` / `--theme-surface` / `--theme-border` (`#261007` / `#1B0C06` / `#481D0F` default), Grain Orange (`#FF5F26` default / active `--orange`), Soft Coral Orange (`#FF8759` / `--orange-soft`), and Punched White (`#FAFAFA`) consistently across portal surfaces.
- **Do** invert foreground text and borders to Ink Black (`#0A0A0A` or `#111111`, `≥6.22:1` contrast) whenever placing elements over Grain Orange / active `--orange` fills.
- **Do** enforce rectilinear `0px` corners across all editorial containers, workbench cards, buttons, inputs, tables, and Cover Flow tiles—limiting rounded geometry strictly to `6px` on `.brand img` and the concentric mobile hardware preview replica inside `<dialog id="player-dialog">` (`44px` / `35px` / `24px` / `16px` / `12px` / `10px` / `8px` / `999px` / `50%`).
- **Do** partition modules with `1px` hairline rules (`#262626` / `#3A3A3A` / `var(--theme-border)`) and `#0A0A0A` ↔ `#131313` / `--theme-surface` stepping instead of resting card shadows.
- **Do** render uniform-stroke inline `<svg>` icons (`stroke-width: 1.8–2`), global `tabular-nums`, `2px` accent `:focus-visible` rings, always-visible WAAPI scroll reveals (`opacity: 1` at rest), and `prefers-reduced-motion` resets (`0.01ms` motion collapse, `240ms` theme shift).

### Don't:
- **Don't** place white text on Grain Orange / active `--orange` fills, and don't paint generic content cards Punched White (`#FAFAFA`)—white surfaces belong strictly to active device proof frames and staged screenshot mats.
- **Don't** introduce multi-hue SaaS rainbow/mesh gradients (limit gradients strictly to `.hero-showcase`'s cover-derived `180deg` `color-mix` vertical backlight bloom, `.phone-screen`'s `168deg` 3-stop album wash inside `#player-dialog`, and `.lyrics-bench`'s subtle `12%` radial glow, keeping `.coverflow-stage` transparent), glassmorphism blur cards (only sticky top nav bars and `.phone-dialog::backdrop` blur underlying content), hard block drop shadows on resting containers, circular `50%` buttons outside `.phone-frame`, or >1px decorative left/right border stripes.
- **Don't** add eyebrow category tags above display headings, decorative `01 / 02` section numbers, or emoji characters as UI icons.
- **Don't** reintroduce legacy green (`#c8ee9b` / `#d1edac`), old red (`#E5483A`), or excluded fonts (`Fraunces`, `Playfair Display`, `Cormorant`, `Lora`, `Crimson`, `Newsreader`, `Syne`, `Space Grotesk`, `Space Mono`, `IBM Plex`, `Inter`, `DM Sans`, `DM Serif`, `Outfit`, `Plus Jakarta Sans`, `Instrument Sans`).
