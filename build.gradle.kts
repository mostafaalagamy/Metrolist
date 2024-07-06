plugins {
    alias(libs.plugins.hilt) apply (false)
    alias(libs.plugins.kotlin.ksp) apply (false)
}

buildscript {
    val isFullBuild by extra {
        gradle.startParameter.taskNames.none { task -> task.contains("foss", ignoreCase = true) }
    }

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.gradle)
        classpath(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    }
}

tasks.register<Delete>("Clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            if (project.findProperty("enableComposeCompilerReports") == "true") {
                arrayOf("reports", "metrics").forEach {
                    freeCompilerArgs = freeCompilerArgs +
                        listOf(
                            "-P",
                            "plugin:androidx.compose.compiler.plugins.kotlin:${it}Destination=${project.buildDir.absolutePath}/compose_metrics",
                        )
                }
            }
        }
    }
}
