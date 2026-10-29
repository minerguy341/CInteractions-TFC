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

dependencies {
    // Cinematic Interactions ships the addon API inside its own jar; compile against it, never bundle it.
    compileOnly(files("libs/${prop("deps.cinematic_jar")}"))
    // Needed in the dev runs so the host mod actually loads.
    runtimeOnly(files("libs/${prop("deps.cinematic_jar")}"))

    // TFC is not published to its own Maven; Modrinth's Maven serves the release jars (project ID : version ID).
    compileOnly("maven.modrinth:${prop("deps.tfc_project")}:${prop("deps.tfc_version_id")}")
    runtimeOnly("maven.modrinth:${prop("deps.tfc_project")}:${prop("deps.tfc_version_id")}")

    // Optional: compile-only so the addon loads without it. Guard every use behind ModList.isLoaded("tfchotornot").
    // It is not on the dev runtime classpath, so the default runs exercise the "without" path;
    // to test "with", drop its jar (and its tfcregistryapi dependency) into run/mods.
    compileOnly("maven.modrinth:${prop("deps.hon_project")}:${prop("deps.hon_version_id")}")
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
