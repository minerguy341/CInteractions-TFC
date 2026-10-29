# CInteractions-TFC

A [Cinematic Interactions](https://github.com/dcvke/CinematicInteractions) addon that gives TerraFirmaCraft workstations first-person scenes.

Target: Minecraft 1.21.1, NeoForge 21.1.x, TerraFirmaCraft 4.2.x, Cinematic Interactions 0.18.12+ (addon API preview).

## Setup

1. Download the Cinematic Interactions NeoForge 1.21.1 jar from its [releases](https://github.com/dcvke/CinematicInteractions/releases) and put it in `libs/` (the name must match `deps.cinematic_jar` in `gradle.properties`). It is git-ignored and never bundled.
2. `./gradlew build`, or `./gradlew runClient`.

## Status

Scaffold only. Next: wire the TFC dependency, then register a scene for the anvil (see `docs/addon-api.md` in the host repo).
