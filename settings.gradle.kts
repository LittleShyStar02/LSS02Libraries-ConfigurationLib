plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "LSS02ConfigurationLib"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // SimpleYAML is published only on JitPack: restrict this repository to its group so no
        // other dependency can be resolved from it (dependency confusion).
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeGroupAndSubgroups("com.github.Carleslc") }
        }
        // Paper API and its Minecraft specific transitive dependencies only.
        maven("https://repo.papermc.io/repository/maven-public/") {
            content {
                includeGroupAndSubgroups("io.papermc")
                includeGroup("com.mojang")
                includeGroup("net.md-5")
            }
        }
    }
}

include(
    "config-core",
    "config-bukkit"
)
