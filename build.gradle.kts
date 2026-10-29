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

    // TODO: TerraFirmaCraft 4.2.x for NeoForge 1.21.1 (add its Maven repo/coordinates once confirmed).
}

tasks.processResources {
    val values = mapOf(
        "mod_id" to modId,
        "mod_name" to prop("mod.name"),
        "mod_version" to prop("mod.version"),
        "cinematic_range" to prop("deps.cinematic_range"),
        "minecraft_version" to prop("deps.minecraft"),
    )
    inputs.properties(values)
    filesMatching("META-INF/neoforge.mods.toml") { expand(values) }
}
