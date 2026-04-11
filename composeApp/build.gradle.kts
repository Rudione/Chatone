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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

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

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.driver.native)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.driver.sqlite)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.slf4j.simple)
            implementation(libs.ktor.server.cio)
        }
    }
}

android {
    namespace = "io.rudione.chatone"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.rudione.chatone"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.8"
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
            packageVersion = "1.0.8"

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
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
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
    dependsOn("createReleaseDistributable")

    val appImageRoot = file("build/compose/binaries/main-release/app")
    from(appImageRoot)

    archiveFileName.set("Chatone-${compose.desktop.application.nativeDistributions.packageVersion}-portable.zip")
    destinationDirectory.set(file("build/distributions"))

    entryCompression = org.gradle.api.tasks.bundling.ZipEntryCompression.DEFLATED
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    doLast {
        println("Portable ZIP created: ${archiveFileName.get()}")
        println("Location: ${destinationDirectory.get()}")
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

tasks.register("publishRelease") {
    dependsOn("packageReleaseDistributionForCurrentOS", "createPortableZip")
    doLast {
        println("✅ Release packages ready in build/compose/binaries/")
        println("✅ Portable ZIP ready in build/distributions/")
        println("📤 Upload to GitHub:")
        println("gh release upload v${compose.desktop.application.nativeDistributions.packageVersion} build/compose/binaries/main-release/** build/distributions/*.zip")
    }
}