# Surface Brief: Meowmos App Shell & Core Screens

<!-- impeccable:surface-brief 1 -->

## Mode

operate

## Direction contract

- **FORM:** `seed:153c8f71b89770ea` · Terminal Yellow Wayfinding (`wayfinding-cartography-signage-terminal-yellow-wayfinding`) on Jetpack Compose Material 3 Expressive (`1.4.0`).
- **THESIS:** Replace the legacy navigation drawer with a thumb-reachable 4-tab bottom bar (`Memos`, `Explore`, `Resources`, `Me`; 3 tabs for local accounts) and an expanded-width `NavigationRail`, using high-contrast wayfinding badges and disciplined tonal surfaces so family/group members immediately know which space a note belongs to and where every action lives. Refuses drawer-hidden utilities and gray-on-gray card monotony.
- **OWN-WORLD:** Warm plaster light ground (`#F5F5F0`) with crisp white memo cards (`#FFFFFF`), charcoal ink (`#141513`), and Schiphol signal yellow (`#F4CC00`) reserved strictly for wayfinding signals (active nav indicator, FAB, pinned/space badges, focus highlights); dark mode flips to departure-hall charcoal (`#111210` / `#181916`) with illuminated yellow beacons. Android 12+ Material You dynamic color remains first-class and overrides static tokens cleanly.
- **STORY:** Open directly into the live memo feed, compose with one tap, switch cleanly between personal notes, shared explore, attachments, and personal overview (`Me` with stats, heatmap, archive, and settings), and discover or filter every `#tag` inside Search.
- **FIRST VIEWPORT:** Compact: clean TopAppBar (`Memos` + sync status badge + Search), tonal memo cards with tabular-numeral metadata bar and wayfinding space/pin chips, Extended FAB above a compact Material 3 `ShortNavigationBar`. Expanded (`>= 840dp`): persistent `NavigationRail` with header FAB beside the adaptive two-pane list-detail browser.
- **RISKS & GUARDRAILS:** Signal yellow must never flood body text or large backgrounds; all text pairs keep ≥4.5:1 contrast; all interactive targets stay ≥48dp; preserve Spaces destination memory, autosave, comments preview/reply, and system predictive Back.
