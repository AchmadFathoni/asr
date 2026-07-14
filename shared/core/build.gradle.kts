@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val appVersionNameProvider = providers.gradleProperty("app.versionName")
val appVersionCodeProvider = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.map { it.trim() }

val generateAppVersion = tasks.register("generateAppVersion") {
    val outputDir = layout.buildDirectory.dir("generated/version/src/commonMain/kotlin/com/asr/core")
    inputs.property("versionName", appVersionNameProvider)
    inputs.property("versionCode", appVersionCodeProvider)
    outputs.dir(outputDir)

    doLast {
        val out = outputDir.get().asFile
        out.mkdirs()
        out.resolve("AppVersion.kt").writeText(
            """
            package com.asr.core

            object AppVersion {
                val build: String = "${appVersionCodeProvider.get()}"
                val release: String = "${appVersionNameProvider.get()}"
            }
            """.trimIndent(),
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()

    android {
        namespace = "com.asr.shared.core"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        withHostTest {}
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines)
        }
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/version/src/commonMain/kotlin"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines)
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                dependsOn(generateAppVersion)
            }
        }
    }
}
