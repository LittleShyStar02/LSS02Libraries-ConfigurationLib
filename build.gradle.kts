import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.vanniktech.publish) apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    // Stable module name for JPMS consumers: derived from the artifact, not from the jar file name.
    tasks.withType<Jar> {
        manifest {
            attributes(
                "Automatic-Module-Name" to "io.github.littleshystar02.lss02." + project.name.replace("-", ".")
            )
        }
    }

    afterEvaluate {
        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release.set(project.the<JavaPluginExtension>().toolchain.languageVersion.get().asInt())
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }

        tasks.withType<Javadoc> {
            (options as StandardJavadocDocletOptions).apply {
                encoding = "UTF-8"
                addStringOption("Xdoclint:all,-missing", "-quiet")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(tasks.withType<JacocoCoverageVerification>())
    }

    dependencies {
        "testImplementation"(platform(rootProject.libs.junit.bom))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = SourcesJar.Sources()))
            publishToMavenCentral(automaticRelease = false)
            signAllPublications()

            pom {
                name.set(project.name)
                // Lazy: modules set their description after applying the plugin.
                description.set(providers.provider {
                    requireNotNull(project.description?.takeIf { it.isNotBlank() }) {
                        "${project.path} must declare a description, Maven Central requires it in the POM"
                    }
                })
                inceptionYear.set("2026")
                url.set("https://github.com/LittleShyStar02/LSS02Libraries-ConfigurationLib")

                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }

                developers {
                    developer {
                        id.set("LittleShyStar02")
                        name.set("Vincenzo Cutolo")
                        url.set("https://github.com/LittleShyStar02")
                    }
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/LittleShyStar02/LSS02Libraries-ConfigurationLib/issues")
                }

                scm {
                    url.set("https://github.com/LittleShyStar02/LSS02Libraries-ConfigurationLib")
                    connection.set("scm:git:https://github.com/LittleShyStar02/LSS02Libraries-ConfigurationLib.git")
                    developerConnection.set("scm:git:git@github.com:LittleShyStar02/LSS02Libraries-ConfigurationLib.git")
                }
            }
        }
    }
}
