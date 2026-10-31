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

## Implementation status (v0.1 anvil scene)
Code: `AnvilScene` (client), `AnvilStations` (registers every `tfc:rock/anvil/*` and `tfc:metal/anvil/*` block, one scene id each), `AnvilConfig` (client config), `Tongs`, `HeatLook`.
- Every blow is TFC's own `ScreenButtonPacket(ForgeStep.ordinal())`; PLAN_ID (8) opens TFC's plan screen on right-click. Server menu keeps authority. Step deltas: hit light/medium/hard -3/-6/-9, draw -15, punch +2, bend +7, upset +13, shrink +16.
- Hover zones on the workpiece (u along anvil, v towards player): left end upset, right end shrink, centre dot punch, front half draw, back half bend, middle band hit.
- Hit strength: mouse wheel (done). `hitControl = HOLD_CHARGE` exists in the config but is not implemented; it currently behaves like the wheel.
- Config (`cinteractions_tfc-client.toml`): `hitControl`, `heatGauge` (default off), `planPanel` (default on).
- Heat: translucent glow quad over the ingot coloured from `Heat.getHeat(temp)`; optional text gauge. Work needs `HeatCapability.view(stack).canWork()` and a hammer in the hammer slot, otherwise the click is denied.
- Tongs: found by registry id (`tfchotornot` namespace, path contains `tongs`) in either hand, no Hot or Not classes touched. Without Hot or Not nothing is drawn. Drawn as the extruded item model for now.
- Not verified in game: nothing has been run. Geometry constants (`TOP`, zone sizes, tongs/hammer orientation) will need tuning.

## Findings from first in-game test
- Symptom: the hands pushed out and the player stepped away right after the intro. Cause: CI opens a station's menu server-side via `BlockState.getMenuProvider` with no extra data (`MenuActions.open`). TFC's anvil block has no menu provider (the block entity opens the menu itself with the block position as open-screen data), so CI closed the session. Fix: `MenuActionsMixin` opens it the TFC way (`player.openMenu(anvilBlockEntity, pos)`) for TFC anvils only. Proper long-term fix is an upstream CI API hook for menu opening; candidate PR to the host repo.
- TFC requires Patchouli (>= 1.21.1-92), so dev runs add it as `runtimeOnly`.

- Hot or Not cannot load in a Gradle dev run. Its dependency TFC Registry API (`tfcregistryapi.util.Helpers.discoverRegistryEnums`) calls `FileSystems.newFileSystem` on the file path of every loaded mod, which throws `ProviderNotFoundException` for NeoForge/Minecraft/this mod's dev paths (it crashed from both the Gradle classpath and `run/mods`). So `runClient` is the "without Hot or Not" path; for the "with" path run `./gradlew stageRealMods` and copy `build/real-mods/*.jar` into a NeoForge 1.21.1 instance's `mods` folder in a normal launcher (the set: this addon, CI, TFC, Patchouli, Hot or Not, TFC Registry API).
- First in-game result after the mixin: the exit animation no longer plays immediately.

## Open
- Jaw clamping (tongs model per item type) and holding the ingot visibly in the tongs.
- Hammer swing timing against the server result; impact sparks/sounds.
- Hybrid plan UI: etched ruler, tong-shaft pips (panel is the placeholder).
- HOLD_CHARGE hit control.
- Sides the scene is usable from (currently all four) and the exact anvil top height.
