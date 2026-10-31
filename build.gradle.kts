plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.148"
    idea
}

fun prop(name: String) = property(name).toString()
val modId = prop("mod.id")

version = prop("mod.version")
group = prop("mod.group")
base.archivesName = "CInteractions-TFC-NeoForge-${prop("deps.minecraft")}"
java.toolchain.languageVersion = JavaLanguageVersion.of(prop("mod.java").toInt())

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

neoForge {
    version = prop("deps.neoforge")

    parchment {
        mappingsVersion = prop("deps.parchment")
        minecraftVersion = prop("deps.parchment_minecraft")
    }

    runs {
        register("client") {
            client()
            gameDirectory = file("run")
        }
        register("server") {
            server()
            gameDirectory = file("run")
            programArgument("--nogui")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val devMods by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    // Cinematic Interactions ships the addon API inside its own jar; compile against it, never bundle it.
    compileOnly(files("libs/${prop("deps.cinematic_jar")}"))
    // Needed in the dev runs so the host mod actually loads.
    runtimeOnly(files("libs/${prop("deps.cinematic_jar")}"))

    // TFC is not published to its own Maven; Modrinth's Maven serves the release jars (project ID : version ID).
    compileOnly("maven.modrinth:${prop("deps.tfc_project")}:${prop("deps.tfc_version_id")}")
    runtimeOnly("maven.modrinth:${prop("deps.tfc_project")}:${prop("deps.tfc_version_id")}")

    // TFC's mandatory dependency; dev runs fail to load without it.
    runtimeOnly("maven.modrinth:${prop("deps.patchouli_project")}:${prop("deps.patchouli_version_id")}")

    // Optional for players, so the addon never links against it and works without it (tongs are found by registry id).
    compileOnly("maven.modrinth:${prop("deps.hon_project")}:${prop("deps.hon_version_id")}")

    // Jars for a real launcher instance (see stageRealMods): everything the addon needs at runtime, Hot or Not included.
    devMods("maven.modrinth:${prop("deps.tfc_project")}:${prop("deps.tfc_version_id")}")
    devMods("maven.modrinth:${prop("deps.patchouli_project")}:${prop("deps.patchouli_version_id")}")
    devMods("maven.modrinth:${prop("deps.hon_project")}:${prop("deps.hon_version_id")}")
    devMods("maven.modrinth:${prop("deps.tfcregistry_project")}:${prop("deps.tfcregistry_version_id")}")
}

// Hot or Not cannot load in a Gradle dev run: its dependency TFC Registry API opens the jar of every loaded mod as a
// zip filesystem, which fails for NeoForge/Minecraft/this mod in dev (ProviderNotFoundException). So the "with Hot or
// Not" path is tested in a normal launcher: `./gradlew stageRealMods` collects every jar a NeoForge 1.21.1 instance needs
// into build/real-mods (drop them into the instance's mods folder). `runClient` stays the "without Hot or Not" path.
val stageRealMods by tasks.registering(Copy::class) {
    dependsOn(tasks.jar)
    from(tasks.jar)
    from(devMods)
    from(file("libs/${prop("deps.cinematic_jar")}"))
    into(layout.buildDirectory.dir("real-mods"))
}

tasks.processResources {
    val values = mapOf(
        "mod_id" to modId,
        "mod_name" to prop("mod.name"),
        "mod_version" to prop("mod.version"),
        "cinematic_range" to prop("deps.cinematic_range"),
        "tfc_range" to prop("deps.tfc_range"),
        "hon_range" to prop("deps.hon_range"),
        "minecraft_version" to prop("deps.minecraft"),
    )
    inputs.properties(values)
    filesMatching("META-INF/neoforge.mods.toml") { expand(values) }
}
