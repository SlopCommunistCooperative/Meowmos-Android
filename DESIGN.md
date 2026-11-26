---
name: Meowmos Design System
description: Terminal Yellow Wayfinding built on Jetpack Compose Material 3 Expressive for shared family and group note-taking.
world: "wayfinding-cartography-signage-terminal-yellow-wayfinding"
colors:
  signal-yellow: "#f4cc00"
  signal-ink: "#181500"
  primary-light: "#5e4c00"
  primary-dark: "#f4cc00"
  plaster-surface: "#f5f5f0"
  card-surface-light: "#ffffff"
  surface-container-light: "#e8e8df"
  ink-light: "#141513"
  ink-muted-light: "#464840"
  outline-light: "#c9cac0"
  charcoal-surface: "#111210"
  card-surface-dark: "#181916"
  surface-container-dark: "#1f211c"
  ink-dark: "#eaeae2"
  ink-muted-dark: "#c2c4b8"
  outline-dark: "#3e4038"
  arrival-teal-light: "#005d52"
  arrival-teal-dark: "#7fd5c7"
  error-light: "#ba1a1a"
  error-dark: "#ffb4ab"
typography:
  display-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "34px"
    fontWeight: 700
    lineHeight: 1.18
    letterSpacing: "-0.5px"
  headline-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "0px"
  title-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "0px"
  title-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.375
    letterSpacing: "0.1px"
  body-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0.15px"
  body-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.43
    letterSpacing: "0.15px"
  label-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1.38
    letterSpacing: "0.15px"
  label-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "11px"
    fontWeight: 600
    lineHeight: 1.27
    letterSpacing: "0.45px"
rounded:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "20px"
  xl: "28px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
components:
  wayfinding-badge-signal:
    backgroundColor: "{colors.signal-yellow}"
    textColor: "{colors.signal-ink}"
    typography: "{typography.label-small}"
    rounded: "{rounded.xs}"
    padding: "3px 7px"
  wayfinding-badge-beacon:
    backgroundColor: "{colors.ink-light}"
    textColor: "{colors.plaster-surface}"
    typography: "{typography.label-small}"
    rounded: "{rounded.xs}"
    padding: "3px 7px"
  memo-card:
    backgroundColor: "{colors.card-surface-light}"
    textColor: "{colors.ink-light}"
    typography: "{typography.body-large}"
    rounded: "{rounded.md}"
    padding: "16px"
  fab-primary:
    backgroundColor: "{colors.signal-yellow}"
    textColor: "{colors.signal-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.lg}"
    padding: "16px"
  bottom-nav:
    backgroundColor: "{colors.surface-container-light}"
    textColor: "{colors.ink-light}"
    typography: "{typography.label-medium}"
    height: "64px"
---

# Design System: Meowmos

## Overview

Meowmos uses **Terminal Yellow Wayfinding** implemented on **Jetpack Compose Material 3 Expressive (`1.4.0`)**. The system is designed for Operate-mode clarity: families and small groups should know in a glance where a note belongs (which shared space, whether it is pinned, who wrote it, and whether it is synced) and reach every core destination by thumb.

The navigation drawer is retired in favor of an adaptive Material 3 navigation shell: a compact `ShortNavigationBar` with 4 destinations (`Memos`, `Explore`, `Resources`, `Me`; 3 for local accounts where `Explore` is hidden) on phone widths, transitioning seamlessly into a persistent `NavigationRail` at medium and expanded widths (`>= 600dp`) alongside the two-pane `MemoListDetailLayout` (`>= 840dp`). All tags live directly inside `SearchPage` for immediate discovery and filtering.

## Colors

Meowmos supports **Android 12+ Material You dynamic color** as a first-class option (`MeowmosTheme(dynamicColor = true)`). When dynamic color is disabled or unavailable, the static Terminal Yellow Wayfinding scheme applies:

- **Signal Yellow (`#F4CC00`) + Signal Ink (`#181500`)**: Reserved strictly for wayfinding beacons—the active navigation indicator pill, primary compose FAB, pinned note badge, and active posting-space selector. Contrast exceeds 11:1.
- **Tonal Plaster & Charcoal Surfaces**: Light mode pairs a warm plaster canvas (`#F5F5F0`) with crisp white cards (`#FFFFFF`) and `1dp` subtle borders (`#C9CAC0`). Dark mode flips to departure-hall charcoal (`#111210`) with elevated charcoal cards (`#181916`) and glowing signal beacons.
- **Extended Semantic Tokens (`MeowmosTheme.colors`)**: `wayfindingContainer`, `beaconContainer`, `cardSurface`, `pinnedBorder`, and a 5-step `heatmapEmpty` / `heatmapLevel1..4` ramp derived from the active `ColorScheme` so light, dark, and Material You dynamic themes stay harmonious.

Source of truth: `app/src/main/java/io/github/slopcommunistcooperative/meowmos/ui/theme/Color.kt`, `DesignTokens.kt`, and `Theme.kt`.

## Typography

Built on the Android system humanist sans-serif family (`FontFamily.SansSerif`, Roboto + system CJK) mapped cleanly to Material 3 roles in `Type.kt`:

- **Tabular numerals (`fontFeatureSettings = "tnum"`)** are enabled on `displaySmall`, `headline*`, and `label*` roles so timestamps, counts, and the 3-column metrics board on `MePage` align cleanly without jitter.
- **Reading comfort**: `bodyLarge` (`16sp / 24sp`) governs memo Markdown prose; `titleLarge` (`20sp` SemiBold) anchors top app bars; `labelSmall` (`11sp` SemiBold) drives compact wayfinding badges.

## Layout

- **Grid & Rhythm**: Strict `4dp / 8dp` spacing scale (`MeowmosSpacing`: `4, 8, 12, 16, 24dp`). Memo cards use `16dp` horizontal screen inset and `6dp` vertical margin (`12dp` gap between cards).
- **Adaptive Breakpoints**:
  - `< 600dp` (Compact): `ShortNavigationBar` at bottom; `ExtendedFloatingActionButton` on `MemosHomePage` collapses on scroll.
  - `>= 600dp` (Medium/Expanded): Persistent `NavigationRail` on start edge hosting the single compose `FloatingActionButton`; home page hides its duplicate FAB.
  - `>= 840dp` (Available content width): `MemoListDetailLayout` opens side-by-side list and detail panes, preserving selection across breakpoint transitions via `movableContentOf`.
- **Edge-to-Edge**: Transparent status and navigation bars; bottom bar consumes its own window insets so FABs and list footers never collide with system gesture bars.

## Elevation & Depth

Meowmos relies on **tonal surface layering + crisp `1dp` structural outlines** rather than heavy drop shadows:

- Page ground sits on `surface` (`#F5F5F0` light / `#111210` dark).
- Memo cards and settings groups sit on `MeowmosTheme.colors.cardSurface` (`#FFFFFF` light / `#181916` dark) with `1dp` `outlineVariant` border (`1.5dp` `pinnedBorder` when pinned).
- Navigation chrome (`ShortNavigationBar`, `NavigationRail`, scrolled `TopAppBar`) steps to `surfaceContainer` (`#E8E8DF` light / `#1F211C` dark) with a `1dp` divider.

## Shapes

Defined in `MeowmosShapes` (`Shape.kt`):

- `extraSmall` (`4dp`): Activity heatmap cells and `WayfindingBadge` tags.
- `small` (`8dp`): Tag filter chips, posting-space button, icon tiles.
- `medium` (`16dp`): Memo cards, settings group cards, media grid tiles.
- `large` (`20dp`): Extended FAB and dialogs.
- `extraLarge` (`28dp`): Full-sheet overlays.

## Components

### Navigation (`MemosPage.kt`, `MemosNavigation.kt`)
- **Compact**: Material 3 `ShortNavigationBar` with signal-yellow active pill (`wayfindingContainer`) and crisp icon/label contrast. Top-level tab switches use Material 3 fade-through motion (`180ms` / `140ms`).
- **Wide**: Material 3 `NavigationRail` with header FAB and vertical divider.

### Wayfinding Badges (`WayfindingBadge.kt`)
- Compact `4dp`-radius architectural pills with four semantic tones: `Signal` (pinned, active tag header, tag count), `Beacon` (space destination, Explore `@author`), `Muted`, and `Alert` (unsynced count).

### Memo Cards (`MemosCard.kt`, `ExploreMemoCard.kt`, `ArchevedMemoCard.kt`)
- `16dp` rounded surface cards with tabular timestamp + wayfinding metadata row, Markdown content, attachments, and inline comment preview/reply bar.

### Search & All Tags (`SearchPage.kt`)
- Material 3 `SearchBar` paired with a live-filtering `FlowRow` panel showing all account `#tags` as `AssistChip` controls with a signal count badge.

### Me Hub (`MePage.kt`, `Stats.kt`, `HeatmapStat.kt`)
- Houses the current account identity card, 3-column tabular metrics board, 5-step theme-adaptive activity heatmap with legend, and grouped links to `Archived` and `Settings`.

## Do's and Don'ts

### Do:
- **Do** reserve `MeowmosTheme.colors.wayfindingContainer` (Signal Yellow) for wayfinding signals: active navigation state, primary FAB, pinned markers, and active posting space.
- **Do** use `MeowmosTheme.colors` and `MaterialTheme.colorScheme` tokens everywhere so light, dark, and Android 12+ Material You dynamic color stay coherent.
- **Do** keep one primary FAB per screen (`NavigationRail` header on wide screens, bottom-end `ExtendedFloatingActionButton` on compact screens).
- **Do** maintain at least `48x48dp` interactive targets via `ActionIconButton` and standard Material 3 controls.

### Don't:
- **Don't** flood large card backgrounds or body copy with Signal Yellow.
- **Don't** hardcode hex colors inside UI composables (such as heatmap tiles); always read from `MeowmosTheme.colors` or `MaterialTheme.colorScheme`.
- **Don't** reintroduce a modal navigation drawer or split top-level navigation across two patterns.
- **Don't** bypass Spaces destination memory, autosave, or predictive Back in the editor or list-detail browser.
