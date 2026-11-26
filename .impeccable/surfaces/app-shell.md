# Surface Brief: Meowmos App Shell & Core Screens

<!-- impeccable:surface-brief 1 -->

## Mode

operate

## Direction contract

- **FORM:** `seed:153c8f71b89770ea` · Terminal Yellow Wayfinding (`wayfinding-cartography-signage-terminal-yellow-wayfinding`) on Jetpack Compose Material 3 Expressive (`1.5.0`).
- **THESIS:** Use a bottom bar for Memos, Explore, Resources, and Me. Hide Explore for local accounts. Use a navigation rail at widths of 600 dp or more. Show the note's space on its card.
- **OWN-WORLD:** The light palette uses a warm background (`#F5F5F0`), white cards (`#FFFFFF`), and charcoal text (`#141513`). The dark palette uses a charcoal background (`#111210`) and cards (`#181916`). Reserve signal yellow (`#F4CC00`) for selected tabs, the compose button, pinned and space badges, and focus highlights. Meowmos uses system colors unless the user turns off Material You colors. Both themes use Material Symbols Sharp at weight 400 and grade 0.
- **STORY:** Open directly into the live memo feed, compose with one tap, switch cleanly between personal notes, shared explore, attachments, and personal overview (`Me` with stats, heatmap, archive, and settings), and discover or filter every `#tag` inside Search.
- **FIRST VIEWPORT:** The Home title, search, and sync stay above the notes on the same background as the feed. The compose button sits above the bottom navigation on compact screens and moves to the navigation rail at widths of 600 dp or more. At 840 dp of available content width, show the list and note side by side.
- **RISKS & GUARDRAILS:** Signal yellow must never flood body text or large backgrounds; all text pairs keep ≥4.5:1 contrast; all interactive targets stay ≥48dp; preserve Spaces destination memory, autosave, comments preview/reply, and system predictive Back.
