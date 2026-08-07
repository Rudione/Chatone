import java.security.MessageDigest
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

val appVersion: String = providers.gradleProperty("app.version").get()
val appVersionCode: Int = providers.gradleProperty("app.versionCode").get().toInt()

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/commonMain/kotlin")
    val appVersionValue = appVersion
    val appVersionCodeValue = appVersionCode
    outputs.dir(outputDir)
    inputs.property("appVersion", appVersionValue)
    inputs.property("appVersionCode", appVersionCodeValue)
    doLast {
        val pkgDir = outputDir.get().asFile.resolve("io/rudione/chatone/util")
        pkgDir.mkdirs()
        pkgDir.resolve("BuildConfig.kt").writeText(
            """
            package io.rudione.chatone.util

            object BuildConfig {
                const val VERSION: String = "$appVersionValue"
                const val VERSION_CODE: Int = $appVersionCodeValue
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        mainRun {
            mainClass.set("io.rudione.chatone.MainKt")
        }
    }

    // iOS targets temporarily disabled: navigation3-ui's iOS klibs (all versions, incl. 1.1.1)
    // are built with Kotlin/Native ABI 2.3.0, unreadable by our Kotlin 2.2.20 toolchain.
    // Re-enable once the project moves to Kotlin 2.3.x + Compose Multiplatform 1.11.x.
    // listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "ComposeApp"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        val desktopMain by getting

        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/source/buildConfig/commonMain/kotlin"))
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.auth)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.napier)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.noarg)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.coil.svg)
            implementation("org.jetbrains.kotlinx:atomicfu:0.27.0")
            implementation(compose.materialIconsExtended)
            implementation(libs.navigation3.ui)
        }

        androidMain.dependencies {
            implementation(libs.coil.gif)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.driver.android)
            implementation(libs.koin.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.android)
        }

        // iosMain.dependencies {
        //     implementation(libs.ktor.client.darwin)
        //     implementation(libs.sqldelight.driver.native)
        // }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.driver.sqlite)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.slf4j.simple)
            implementation(libs.ktor.server.cio)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
        all {
            languageSettings.enableLanguageFeature("BreakContinueInInlineLambdas")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateBuildConfig)
}

android {
    namespace = "io.rudione.chatone"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.rudione.chatone"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "io.rudione.chatone.MainKt"

        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "Chatone"
            packageVersion = appVersion

            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.sql",
                "java.security.jgss",
                "java.security.sasl",
                "jdk.crypto.ec",
                "jdk.crypto.cryptoki",
                "jdk.unsupported",
                "jdk.naming.dns",
                "jdk.net"
            )

            macOS {
                iconFile.set(project.file("src/desktopMain/resources/logochattone.icns"))
            }
            windows {
                iconFile.set(project.file("src/desktopMain/resources/logochattone.ico"))
                perUserInstall = true
                menuGroup = "Chatone"
                shortcut = true
            }
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
        }
    }
}

tasks.register<Zip>("createPortableZip") {
    dependsOn(":composeApp:createReleaseDistributable")

    val buildDir = layout.buildDirectory.asFile.get()
    val appDir = buildDir.resolve("compose/binaries/main-release/app")

    doFirst {
        println("🔍 createPortableZip: Checking $appDir")
        if (!appDir.exists()) {
            throw GradleException("❌ Directory not found: $appDir")
        }
        appDir.listFiles()?.forEach { f ->
            println("   ├─ ${f.name} (${if (f.isDirectory) "dir" else "file"})")
        }
    }

    val sourceDir = provider {
        val candidates = appDir.listFiles()?.filter { it.isDirectory && it.name.endsWith(".app") } ?: emptyList()
        when {
            candidates.isNotEmpty() -> {
                println("✅ Found .app bundle: ${candidates.first().name}")
                candidates.first()
            }
            appDir.listFiles()?.any { it.isDirectory } == true -> {
                val first = appDir.listFiles()!!.first { it.isDirectory }
                println("✅ Using first subdirectory: ${first.name}")
                first
            }
            else -> {
                println("⚠️ No subdirectories, zipping app/ contents directly")
                appDir
            }
        }
    }

    from(sourceDir)

    archiveFileName.set("Chatone-${compose.desktop.application.nativeDistributions.packageVersion}-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    entryCompression = ZipEntryCompression.DEFLATED
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    doLast {
        val zipFile = archiveFile.get().asFile
        println("✅ Portable ZIP created: ${zipFile.name}")
        println("📍 Location: ${zipFile.absolutePath}")
        println("📦 Size: ${zipFile.length() / 1024 / 1024} MB")
    }
}

sqldelight {
    databases {
        create("ChatoneDatabase") {
            packageName.set("io.rudione.chatone.data.local")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "chatone.composeapp.generated.resources"
    generateResClass = auto
}

tasks.register("packageWindowsSetup") {
    group = "distribution"
    description = "Builds a branded Inno Setup installer from the release app image"
    dependsOn(":composeApp:createReleaseDistributable")

    val appImageDir = layout.buildDirectory.dir("compose/binaries/main-release/app/Chatone")
    val outputDir = layout.buildDirectory.dir("installer")
    val script = layout.projectDirectory.file("installer/chatone.iss")
    val version = appVersion

    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }

    doLast {
        val source = appImageDir.get().asFile
        if (!source.exists()) {
            throw GradleException("App image not found: $source")
        }
        val output = outputDir.get().asFile.apply { mkdirs() }

        val compiler = sequenceOf(
            System.getenv("INNO_SETUP_ISCC"),
            "C:\\Program Files (x86)\\Inno Setup 6\\ISCC.exe",
            "C:\\Program Files\\Inno Setup 6\\ISCC.exe",
            "iscc"
        ).filterNotNull().firstOrNull { candidate ->
            candidate == "iscc" || File(candidate).exists()
        } ?: throw GradleException("Inno Setup (ISCC.exe) not found. Set INNO_SETUP_ISCC.")

        providers.exec {
            commandLine(
                compiler,
                "/DAppVersion=$version",
                "/DSourceDir=${source.absolutePath}",
                "/DOutputDir=${output.absolutePath}",
                script.asFile.absolutePath
            )
        }.standardOutput.asText.get().let(::println)

        println("Installer ready: ${output.resolve("Chatone-$version-setup.exe")}")
    }
}

tasks.register("generateReleaseChecksums") {
    group = "distribution"
    description = "Writes SHA-256 checksums for every produced release artifact"

    val binariesDir = layout.buildDirectory.dir("compose/binaries/main-release")
    val distributionsDir = layout.buildDirectory.dir("distributions")
    val installerDir = layout.buildDirectory.dir("installer")
    val outputFile = layout.buildDirectory.file("distributions/SHA256SUMS.txt")

    doLast {
        val extensions = setOf("msi", "exe", "dmg", "deb", "zip")
        val artifacts = listOf(binariesDir, distributionsDir, installerDir)
            .mapNotNull { it.orNull?.asFile }
            .filter { it.exists() }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile }.toList() }
            .filter { it.extension.lowercase() in extensions }
            .distinctBy { it.name }
            .sortedBy { it.name }

        if (artifacts.isEmpty()) {
            println("No release artifacts found; skipping checksums")
            return@doLast
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val lines = artifacts.map { file ->
            digest.reset()
            file.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val hex = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            "$hex  ${file.name}"
        }

        val target = outputFile.get().asFile
        target.parentFile.mkdirs()
        target.writeText(lines.joinToString("\n") + "\n")
        println("Checksums written to ${target.absolutePath}")
        lines.forEach(::println)
    }
}

tasks.register("publishRelease") {
    group = "distribution"
    dependsOn("packageReleaseDistributionForCurrentOS", "createPortableZip")
    if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        dependsOn("packageWindowsSetup")
    }
    finalizedBy("generateReleaseChecksums")
    doLast {
        println("Release packages ready in build/compose/binaries/")
        println("Portable ZIP ready in build/distributions/")
        println("Windows setup (if built) in build/installer/")
    }
}
