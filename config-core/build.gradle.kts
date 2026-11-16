import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.japicmp)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

apply(plugin = "com.vanniktech.maven.publish")

description = "Core layer over SimpleYAML's YamlFile: enum-based ConfigKey accessors, typed getters with defaults, and fail-fast validation."

dependencies {
    // The SimpleYAML range is part of the public contract: YamlFile is exposed by AbstractConfiguration.
    api(libs.simpleyaml)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).links("https://docs.oracle.com/en/java/javase/17/docs/api/")
}

// The baseline is the last published release. It is downloaded as a plain file because resolving
// io.github.littleshystar02:config-core as a dependency would be substituted by this very project.
val baselineVersion = libs.versions.baseline.get()
val baselineJar = layout.buildDirectory.file("baseline/config-core-$baselineVersion.jar")

val downloadApiBaseline = tasks.register("downloadApiBaseline") {
    description = "Downloads the last published config-core jar from Maven Central."
    val target = baselineJar
    val version = baselineVersion
    outputs.file(target)
    doLast {
        val file = target.get().asFile
        file.parentFile.mkdirs()
        URI("https://repo1.maven.org/maven2/io/github/littleshystar02/config-core/$version/config-core-$version.jar")
            .toURL().openStream().use { Files.copy(it, file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    }
}

val japicmpCheck = tasks.register<me.champeau.gradle.japicmp.JapicmpTask>("japicmpCheck") {
    description = "Fails on binary incompatible changes against the last published release."
    group = "verification"
    dependsOn(downloadApiBaseline)
    oldClasspath.from(baselineJar)
    newClasspath.from(tasks.jar)
    onlyBinaryIncompatibleModified.set(true)
    failOnModification.set(true)
    ignoreMissingClasses.set(true)
    txtOutputFile.set(layout.buildDirectory.file("reports/japicmp.txt"))
}

tasks.named("check") {
    dependsOn(japicmpCheck)
}
