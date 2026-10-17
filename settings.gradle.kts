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
           
            version("kotlin", "2.4.21")
            version("compose", "1.12.2")
            version("agp", "9.3.3")

           
            version("ktor", "3.6.0")

           
            version("koin", "4.2.2")
            version("koinCompose", "4.2.2")

           
            version("sqldelight", "2.4.1")

           
            version("kotlinxSerialization", "1.11.0")

           
            version("coroutines", "1.11.0")

           
            version("datetime", "0.8.0")

           
            version("napier", "2.7.1")

           
            version("settings", "1.3.0")

           
            version("androidxCore", "1.19.1")
            version("androidxActivity", "1.13.0")
            version("androidxLifecycle", "2.11.0")
            version("androidxSplashscreen", "1.2.0")

           
            version("coil", "3.6.3")

            version("media3", "1.11.1")

            version("work", "2.12.0")


            version("jna", "5.19.1")


            version("nav3", "1.1.2")
            version("material3", "1.9.0")
            version("atomicfu", "0.33.0")
            version("androidxBrowser", "1.10.0")
            version("paging", "3.5.3")


            plugin("kotlinMultiplatform", "org.jetbrains.kotlin.multiplatform").versionRef("kotlin")
            plugin("androidApplication", "com.android.application").versionRef("agp")
            plugin("androidKmpLibrary", "com.android.kotlin.multiplatform.library").versionRef("agp")
            plugin("composeMultiplatform", "org.jetbrains.compose").versionRef("compose")
            plugin("composeCompiler", "org.jetbrains.kotlin.plugin.compose").versionRef("kotlin")
            plugin("kotlinSerialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            plugin("sqldelight", "app.cash.sqldelight").versionRef("sqldelight")

           
            library("ktor-client-core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor-client-okhttp", "io.ktor", "ktor-client-okhttp").versionRef("ktor")
            library("ktor-client-darwin", "io.ktor", "ktor-client-darwin").versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-client-serialization", "io.ktor", "ktor-client-serialization").versionRef("ktor")
            library("ktor-client-logging", "io.ktor", "ktor-client-logging").versionRef("ktor")
            library("ktor-serialization-json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            library("ktor-client-websockets", "io.ktor", "ktor-client-websockets").versionRef("ktor")
            library("ktor-client-auth", "io.ktor", "ktor-client-auth").versionRef("ktor")
            library("ktor-client-mock", "io.ktor", "ktor-client-mock").versionRef("ktor")

           
            library("ktor-server-cio", "io.ktor", "ktor-server-cio").versionRef("ktor")
            library("ktor-server-html-builder", "io.ktor", "ktor-server-html-builder").versionRef("ktor")

           
            library("koin-core", "io.insert-koin", "koin-core").versionRef("koin")
            library("koin-compose", "io.insert-koin", "koin-compose").versionRef("koinCompose")
            library("koin-compose-viewmodel", "io.insert-koin", "koin-compose-viewmodel").versionRef("koinCompose")
            library("koin-android", "io.insert-koin", "koin-android").versionRef("koin")

           
            library("sqldelight-driver-android", "app.cash.sqldelight", "android-driver").versionRef("sqldelight")
            library("sqldelight-driver-native", "app.cash.sqldelight", "native-driver").versionRef("sqldelight")
            library("sqldelight-driver-sqlite", "app.cash.sqldelight", "sqlite-driver").versionRef("sqldelight")
            library("sqldelight-coroutines", "app.cash.sqldelight", "coroutines-extensions").versionRef("sqldelight")

           
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef("kotlinxSerialization")

           
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")
            library("kotlinx-coroutines-android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef("coroutines")
            library("kotlinx-coroutines-swing", "org.jetbrains.kotlinx", "kotlinx-coroutines-swing").versionRef("coroutines")
            library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("coroutines")

           
            library("slf4j-simple", "org.slf4j", "slf4j-simple").version("2.0.20")

           
            library("kotlinx-datetime", "org.jetbrains.kotlinx", "kotlinx-datetime").versionRef("datetime")

           
            library("napier", "io.github.aakira", "napier").versionRef("napier")

           
            library("multiplatform-settings", "com.russhwolf", "multiplatform-settings").versionRef("settings")
            library("multiplatform-settings-noarg", "com.russhwolf", "multiplatform-settings-no-arg").versionRef("settings")

           
            library("androidx-core-ktx", "androidx.core", "core-ktx").versionRef("androidxCore")
            library("androidx-activity-compose", "androidx.activity", "activity-compose").versionRef("androidxActivity")
            library("androidx-lifecycle-runtime-compose", "androidx.lifecycle", "lifecycle-runtime-compose").versionRef("androidxLifecycle")
            library("androidx-lifecycle-viewmodel-compose", "androidx.lifecycle", "lifecycle-viewmodel-compose").versionRef("androidxLifecycle")
            library("androidx-core-splashscreen", "androidx.core", "core-splashscreen").versionRef("androidxSplashscreen")

           
            library("coil-compose", "io.coil-kt.coil3", "coil-compose").versionRef("coil")
            library("coil-network-ktor", "io.coil-kt.coil3", "coil-network-ktor3").versionRef("coil")
            library("coil-gif", "io.coil-kt.coil3", "coil-gif").versionRef("coil")
            library("coil-svg", "io.coil-kt.coil3", "coil-svg").versionRef("coil")

            library("media3-exoplayer", "androidx.media3", "media3-exoplayer").versionRef("media3")
            library("media3-exoplayer-hls", "androidx.media3", "media3-exoplayer-hls").versionRef("media3")
            library("media3-datasource-okhttp", "androidx.media3", "media3-datasource-okhttp").versionRef("media3")
            library("media3-session", "androidx.media3", "media3-session").versionRef("media3")

            library("androidx-work-runtime", "androidx.work", "work-runtime").versionRef("work")

            library("androidx-paging-common", "androidx.paging", "paging-common").versionRef("paging")
            library("androidx-paging-compose", "androidx.paging", "paging-compose").versionRef("paging")
            library("androidx-paging-testing", "androidx.paging", "paging-testing").versionRef("paging")


            library("jna", "net.java.dev.jna", "jna").versionRef("jna")
            library("jna-platform", "net.java.dev.jna", "jna-platform").versionRef("jna")


            library("navigation3-ui", "org.jetbrains.androidx.navigation3", "navigation3-ui").versionRef("nav3")

            library("compose-runtime", "org.jetbrains.compose.runtime", "runtime").versionRef("compose")
            library("compose-foundation", "org.jetbrains.compose.foundation", "foundation").versionRef("compose")
            library("compose-ui", "org.jetbrains.compose.ui", "ui").versionRef("compose")
            library("compose-material3", "org.jetbrains.compose.material3", "material3").versionRef("material3")
            library("compose-components-resources", "org.jetbrains.compose.components", "components-resources").versionRef("compose")
            library("compose-components-uiToolingPreview", "org.jetbrains.compose.components", "components-ui-tooling-preview").versionRef("compose")
            library("kotlinx-atomicfu", "org.jetbrains.kotlinx", "atomicfu").versionRef("atomicfu")
            library("androidx-browser", "androidx.browser", "browser").versionRef("androidxBrowser")
        }
    }
}

include(":composeApp")
include(":androidApp")
include(":core:icons")
