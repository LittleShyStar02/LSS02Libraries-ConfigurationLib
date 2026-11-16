java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

apply(plugin = "com.vanniktech.maven.publish")

description = "Bukkit/Paper adapter for LSS02ConfigurationLib: resource extraction backed by JavaPlugin#saveResource with automatic fallback."

dependencies {
    api(project(":config-core"))
    compileOnly(libs.paper.api)

    testImplementation(libs.paper.api)
    testImplementation(libs.mockito.core)
}
