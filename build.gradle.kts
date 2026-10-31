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

// Dev-only mod holding the TFC Registry API workaround (see copyDevMods). Its own source set, so it never reaches the release jar.
val devfix: SourceSet = sourceSets.create("devfix") {
    compileClasspath += sourceSets.main.get().compileClasspath
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
        register("cinteractions_tfc_devfix") {
            sourceSet(devfix)
        }
    }
}

val devMods by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}
// Hot or Not and its mandatory TFC Registry API, copied into run/mods for the dev runs.
val devRunMods by configurations.creating {
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

    devRunMods("maven.modrinth:${prop("deps.hon_project")}:${prop("deps.hon_version_id")}")
    devRunMods("maven.modrinth:${prop("deps.tfcregistry_project")}:${prop("deps.tfcregistry_version_id")}")
}

// TFC Registry API (Hot or Not's dependency) opens the jar of every loaded mod as a zip filesystem, which throws
// ProviderNotFoundException for NeoForge/Minecraft/this project in a Gradle dev run. The devfix mod (src/devfix) makes
// it skip non-jar mods, and the two jars load from run/mods like in a real instance.
// `-PwithoutHotOrNot` removes them to exercise the "without Hot or Not" path.
val copyDevMods by tasks.registering {
    val modsDir = file("run/mods")
    val without = hasProperty("withoutHotOrNot")
    inputs.files(devRunMods)
    doLast {
        modsDir.mkdirs()
        modsDir.listFiles { f -> f.name.startsWith("devmod-") }?.forEach { it.delete() }
        if (!without) devRunMods.files.forEach { it.copyTo(File(modsDir, "devmod-${it.name}"), overwrite = true) }
    }
}
tasks.matching { it.name == "runClient" || it.name == "runServer" }.configureEach { dependsOn(copyDevMods) }

// `./gradlew stageRealMods` collects every jar a normal NeoForge 1.21.1 launcher instance needs into build/real-mods.
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
