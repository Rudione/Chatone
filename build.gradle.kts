buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("com.android.tools:r8:8.11.18")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
    alias(libs.plugins.composeCompiler).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.sqldelight).apply(false)
    alias(libs.plugins.ksp).apply(false)
}
