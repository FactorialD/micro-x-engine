# Manual keypad-only UI checklist

Run each case at **240×320**, then repeat at one smaller and one larger display size.
Do not use a pointer/touch input.

- [ ] The title/main menu appears before the level or renderer starts loading.
- [ ] `2`/`8` moves the highlight through Start, Settings, Про гру and Exit; selection wraps.
- [ ] `5` starts the game. A missing/corrupt level shows `LOAD ERROR`; `5` returns to the main menu.
- [ ] In gameplay, `*` opens the PDA and the simulation stops; `*`/`#` changes Inventory, Map and Quests tabs.
- [ ] The map and HUD remain within the screen and centered at every tested resolution.
- [ ] Inventory item boxes use their item cell widths; W1/W2, armor and all five artifact slots fit.
- [ ] Map transitions, player, objective and found stash markers remain visible without clipping.
- [ ] Dialogue portraits/text/numbered answers and both Trade/Loot columns fit without overlap.
- [ ] Quests show table-driven title, description, state and current objective.
- [ ] Repeat the layout pass for renderer resolution modes 0, 1 and 2 at each display size.
- [ ] The left/right game actions (or `4`/`6`) change each Settings value; applying a resolution recreates renderer buffers without a crash or stale frame.
- [ ] The right soft key opens Pause; `2`/`8` and `5` resume, open Settings, or return to the main menu.
- [ ] The left soft key backs out of Settings, PDA, Inventory, Map, Quests, Dialogue, Trade and Loot.
- [ ] Gameplay keys `1`–`9`, `0`, `*`, `#`, both soft keys and directional game-action codes work on the target handset.
- [ ] With Debug enabled, every overlay row (FPS and update/render p95; TRI submitted/clipped/drawn; TEX/mesh; heap used/total/peak; renderer used/budget/percent; location; NPC/MUT/ITEM/ANOM/CORPSE; visible rooms/dropped steps) is legible and does not overlap the minimap, HP/armor/stamina, ammo, or crosshair.
- [ ] Check all debug-overlay rows at 240×320 and at the smaller and larger test sizes, in renderer resolution modes 0, 1 and 2.
- [ ] Move between locations and change geometry; confirm the location, TEX and mesh counters update. Spawn/kill entities and confirm the separate NPC, MUT, ITEM, ANOM and CORPSE counters update.
- [ ] Health, armor, stamina, magazine/reserve, weapon, bleeding/radiation and interaction prompt remain legible.
- [ ] Hold a modal menu open for 30 seconds and confirm player position, AI and status timers do not advance.
