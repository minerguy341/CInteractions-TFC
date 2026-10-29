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

### Chosen: hybrid (v1)
- Etched ruler along the anvil face edge with a slider (current work) and chalk tick (goal).
- Three rule pips on the tong shaft (third-last, second-last, last); they light as each rule is met.
- Hover tags naming the action and its effect (for example "Bend +7").
- Floating panel stays as a toggleable fallback (and is the only display without HoN, since the pips need tongs).

### Future config styles (not built yet)
Selectable via client config later:
1. Scribed anvil face: ruler plus three rule glyphs chalked into the face, no floating UI.
2. Smith's tally slate: in-world slate beside the anvil with the ruler graph and rule icons.
3. Workpiece as gauge: ingot lengthens/shortens, translucent ghost of the goal, three ember pips on the anvil edge.
4. Minimal hover-driven: only hover tags plus a three-step chain shown mid-sequence.

## TFC forging actions (verified from `tfc` 4.2.12 lang keys)
`tfc.enum.forgestep.*`: hit, hit_light, hit_medium, hit_hard, draw, punch, bend, upset, shrink. Plain `hit` is only a rule value (matches any strength); the player performs light/medium/hard as three separate actions. So six action families: hit (three strengths), draw, punch, bend, upset, shrink. Rule ordering lives in `ForgeRule` / `ForgeRule$Order`; steps in `ForgeSteps`.

## Open
- Zone-to-action mapping and how the hit strength is chosen (the three strengths are separate actions in TFC).
