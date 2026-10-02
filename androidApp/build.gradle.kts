import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val appVersion: String = providers.gradleProperty("app.version").get()
val appVersionCode: Int = providers.gradleProperty("app.versionCode").get().toInt()

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

android {
    namespace = "io.rudione.chatone"
    compileSdk = 36
    ndkVersion = "29.0.14206865"
    defaultConfig {
        applicationId = "io.rudione.chatone"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    androidResources {
        localeFilters += setOf("en", "ru")
    }

    val keystoreFile = project.findProperty("signing.storeFile")?.toString()?.let { file(it) }
    val keystorePassword = project.findProperty("signing.storePassword")?.toString()
    val keyAlias = project.findProperty("signing.keyAlias")?.toString()
    val keyPassword = project.findProperty("signing.keyPassword")?.toString()

    val isSigningConfigured = keystoreFile?.exists() == true &&
            !keystorePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() &&
            !keyPassword.isNullOrBlank()

    signingConfigs {
        if (isSigningConfigured) {
            create("release") {
                storeFile = keystoreFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (isSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
        }
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
}
