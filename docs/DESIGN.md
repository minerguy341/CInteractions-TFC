# Design decisions (anvil scene)

Target: Minecraft 1.21.1, NeoForge, TFC 4.2.12, Cinematic Interactions 0.18.12+ addon API. TFC Hot or Not (HoN) 1.2 is **optional**.

## Tongs
- Source: HoN. Per-metal items `tfchotornot:tongs/<metal>` (flat `item/handheld_rod` models, textures in `assets/tfchotornot/textures/item/tongs/` and `tong_part/`). BSD-2-Clause, keep attribution.
- Build a 3D model by extruding those textures. Jaws clamp to the held item's size (ingot, double ingot, sheet, double sheet, rod, other via bounds fallback).
- Shown when tongs are in **either hand**. If the player approaches with tongs and an ingot, the ingot is held in the tongs.
- **Without HoN: no tongs are rendered.** Every HoN reference sits behind `ModList.isLoaded("tfchotornot")`. HoN is `compileOnly`; default dev runs exercise the "without" path. To test "with", put the HoN jar and `tfcregistryapi` in `run/mods`.

## Ingot / workpiece
- Use TFC's own ingot models (generated at runtime in code) rather than hand-made ones.

## Heat
- The player must be able to read the workpiece's heat visually: TFC heat color scale + glow/shimmer.
- A numeric/gauge readout is **configurable** (client config).

## Hammer
- Look follows the hammer's material (TFC per-metal hammer models). Swing/action is chosen by **hovering over different portions of the ingot**.

## Work plan UI
- A floating panel is the placeholder while other scene elements are built.
- Final presentation (work bar, goal, the 3 rule requirements): options being decided, see the chat/PR notes.

## Open
- Zone-to-action mapping and how hit strength (light/medium/hard) is chosen.
- Final plan UI option.
