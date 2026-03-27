rootProject.name = "Chatone"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            // Kotlin & Compose
            version("kotlin", "2.1.10")
            version("compose", "1.7.3")
            version("agp", "8.7.3")

            // Networking
            version("ktor", "3.1.1")

            // DI
            version("koin", "4.0.0")
            version("koinCompose", "4.0.0")

            // Database
            version("sqldelight", "2.0.2")

            // Serialization
            version("kotlinxSerialization", "1.7.3")

            // Coroutines
            version("coroutines", "1.9.0")

            // DateTime
            version("datetime", "0.6.1")

            // Logging
            version("napier", "2.7.1")

            // Settings
            version("settings", "1.2.0")

            // Android
            version("androidxCore", "1.15.0")
            version("androidxActivity", "1.9.3")
            version("androidxLifecycle", "2.8.7")

            // Coil
            version("coil", "3.1.0")

            // Plugins
            plugin("kotlinMultiplatform", "org.jetbrains.kotlin.multiplatform").versionRef("kotlin")
            plugin("androidApplication", "com.android.application").versionRef("agp")
            plugin("composeMultiplatform", "org.jetbrains.compose").versionRef("compose")
            plugin("composeCompiler", "org.jetbrains.kotlin.plugin.compose").versionRef("kotlin")
            plugin("kotlinSerialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            plugin("sqldelight", "app.cash.sqldelight").versionRef("sqldelight")
            plugin("ksp", "com.google.devtools.ksp").version("2.1.10-1.0.31")

            // Ktor Client
            library("ktor-client-core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor-client-okhttp", "io.ktor", "ktor-client-okhttp").versionRef("ktor")
            library("ktor-client-darwin", "io.ktor", "ktor-client-darwin").versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-client-serialization", "io.ktor", "ktor-client-serialization").versionRef("ktor")
            library("ktor-client-logging", "io.ktor", "ktor-client-logging").versionRef("ktor")
            library("ktor-serialization-json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            library("ktor-client-websockets", "io.ktor", "ktor-client-websockets").versionRef("ktor")
            library("ktor-client-auth", "io.ktor", "ktor-client-auth").versionRef("ktor")

            // Ktor Server (for desktop OAuth callback)
            library("ktor-server-cio", "io.ktor", "ktor-server-cio").versionRef("ktor")
            library("ktor-server-html-builder", "io.ktor", "ktor-server-html-builder").versionRef("ktor")

            // Koin
            library("koin-core", "io.insert-koin", "koin-core").versionRef("koin")
            library("koin-compose", "io.insert-koin", "koin-compose").versionRef("koinCompose")
            library("koin-compose-viewmodel", "io.insert-koin", "koin-compose-viewmodel").versionRef("koinCompose")
            library("koin-android", "io.insert-koin", "koin-android").versionRef("koin")

            // SQLDelight
            library("sqldelight-driver-android", "app.cash.sqldelight", "android-driver").versionRef("sqldelight")
            library("sqldelight-driver-native", "app.cash.sqldelight", "native-driver").versionRef("sqldelight")
            library("sqldelight-driver-sqlite", "app.cash.sqldelight", "sqlite-driver").versionRef("sqldelight")
            library("sqldelight-coroutines", "app.cash.sqldelight", "coroutines-extensions").versionRef("sqldelight")

            // Serialization
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef("kotlinxSerialization")

            // Coroutines
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")
            library("kotlinx-coroutines-android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef("coroutines")
            library("kotlinx-coroutines-swing", "org.jetbrains.kotlinx", "kotlinx-coroutines-swing").versionRef("coroutines")

            // SLF4J
            library("slf4j-simple", "org.slf4j", "slf4j-simple").version("2.0.16")

            // DateTime
            library("kotlinx-datetime", "org.jetbrains.kotlinx", "kotlinx-datetime").versionRef("datetime")

            // Logging
            library("napier", "io.github.aakira", "napier").versionRef("napier")

            // Settings
            library("multiplatform-settings", "com.russhwolf", "multiplatform-settings").versionRef("settings")
            library("multiplatform-settings-noarg", "com.russhwolf", "multiplatform-settings-no-arg").versionRef("settings")

            // Android
            library("androidx-core-ktx", "androidx.core", "core-ktx").versionRef("androidxCore")
            library("androidx-activity-compose", "androidx.activity", "activity-compose").versionRef("androidxActivity")
            library("androidx-lifecycle-runtime-compose", "androidx.lifecycle", "lifecycle-runtime-compose").versionRef("androidxLifecycle")
            library("androidx-lifecycle-viewmodel-compose", "androidx.lifecycle", "lifecycle-viewmodel-compose").versionRef("androidxLifecycle")

            // Coil for image loading
            library("coil-compose", "io.coil-kt.coil3", "coil-compose").versionRef("coil")
            library("coil-network-ktor", "io.coil-kt.coil3", "coil-network-ktor3").versionRef("coil")
            library("coil-gif", "io.coil-kt.coil3", "coil-gif").versionRef("coil")
        }
    }
}

include(":composeApp")
