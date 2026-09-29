import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
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

    applyDefaultHierarchyTemplate()

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

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        val mobileMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.paging.common)
                implementation(libs.androidx.paging.compose)
            }
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)

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
            implementation(projects.core.icons)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.androidx.paging.testing)
            }
        }

        androidMain.dependencies {
            implementation(libs.coil.gif)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.driver.android)
            implementation(libs.koin.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.datasource.okhttp)
            implementation(libs.androidx.work.runtime)
            implementation("androidx.browser:browser:1.8.0")
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-android.pro")
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
        }
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    sourceSets.getByName("main").baselineProfiles.srcDir("src/androidMain/baselineProfiles")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "io.rudione.chatone.MainKt"

        jvmArgs(
            "-XX:+DisableAttachMechanism",
            "-XX:-UsePerfData",
            "-XX:-HeapDumpOnOutOfMemoryError",
            "-XX:-CreateCoredumpOnCrash",
            "-Djdk.attach.allowAttachSelf=false",
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication",
            "-XX:G1PeriodicGCInterval=60000",
            "-XX:MinHeapFreeRatio=10",
            "-XX:MaxHeapFreeRatio=30"
        )

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
                iconFile.set(project.file("packaging/icons/logochattone.icns"))
            }
            windows {
                iconFile.set(project.file("packaging/icons/logochattone.ico"))
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
            throw GradleException("Directory not found: $appDir")
        }
        appDir.listFiles()?.forEach { f ->
            println("   ├─ ${f.name} (${if (f.isDirectory) "dir" else "file"})")
        }
    }

    from(appDir)

    val osTag = when {
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "windows"
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "macos"
        else -> "linux"
    }

    archiveFileName.set(
        "Chatone-${compose.desktop.application.nativeDistributions.packageVersion}-$osTag-portable.zip"
    )
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    entryCompression = ZipEntryCompression.DEFLATED
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    doLast {
        val zipFile = archiveFile.get().asFile
        println("Portable ZIP created: ${zipFile.name}")
        println("Location: ${zipFile.absolutePath}")
        println("Size: ${zipFile.length() / 1024 / 1024} MB")
    }
}

abstract class KeepHostNatives : TransformAction<KeepHostNatives.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val hostOs: Property<String>
    }

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val host = parameters.hostOs.get()
        val keep = nativeFilter(input.name, host)
        if (keep == null || !input.isFile) {
            outputs.file(input)
            return
        }
        val output = outputs.file("${input.nameWithoutExtension}-$host.jar")
        ZipFile(input).use { zip ->
            ZipOutputStream(output.outputStream().buffered()).use { out ->
                zip.entries().asSequence().filter { keep(it.name) }.forEach { entry ->
                    out.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                    if (!entry.isDirectory) zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
    }

    private fun nativeFilter(jarName: String, host: String): ((String) -> Boolean)? = when {
        jarName.startsWith("sqlite-jdbc-") && jarName.endsWith(".jar") -> {
            val kept = when (host) {
                "macos" -> listOf("Mac/")
                "windows" -> listOf("Windows/")
                else -> listOf("Linux/", "Linux-Musl/")
            }.map { "org/sqlite/native/$it" }
            val filter: (String) -> Boolean = { name ->
                !name.startsWith("org/sqlite/native/") || name == "org/sqlite/native/" ||
                        kept.any { name.startsWith(it) }
            }
            filter
        }

        jarName.startsWith("jna-") && !jarName.startsWith("jna-platform") && jarName.endsWith(".jar") -> {
            val prefix = when (host) {
                "macos" -> "darwin"
                "windows" -> "win32"
                else -> "linux"
            }
            val platformDir = Regex("^com/sun/jna/([a-z0-9]+)-[a-z0-9-]+/")
            val filter: (String) -> Boolean = { name ->
                platformDir.find(name)?.groupValues?.get(1)?.let { it == prefix } ?: true
            }
            filter
        }

        else -> null
    }
}

val hostNativesOnly: Attribute<Boolean> =
    Attribute.of("io.rudione.chatone.host-natives-only", Boolean::class.javaObjectType)

dependencies {
    attributesSchema { attribute(hostNativesOnly) }
    artifactTypes.maybeCreate("jar").attributes.attribute(hostNativesOnly, false)
    registerTransform(KeepHostNatives::class) {
        from.attribute(hostNativesOnly, false)
            .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
        to.attribute(hostNativesOnly, true)
            .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
        parameters {
            hostOs.set(
                when {
                    org.gradle.internal.os.OperatingSystem.current().isWindows -> "windows"
                    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "macos"
                    else -> "linux"
                }
            )
        }
    }
}

configurations.matching { it.name == "desktopRuntimeClasspath" }.configureEach {
    attributes.attribute(hostNativesOnly, true)
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

        val process = ProcessBuilder(
            compiler,
            "/DAppVersion=$version",
            "/DSourceDir=${source.absolutePath}",
            "/DOutputDir=${output.absolutePath}",
            script.asFile.absolutePath
        ).redirectErrorStream(true).start()

        process.inputStream.bufferedReader().useLines { lines -> lines.forEach(::println) }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("Inno Setup compiler failed with exit code $exitCode")
        }

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
        val extensions = setOf("msi", "dmg", "deb", "zip")
        val artifacts = listOf(binariesDir, distributionsDir, installerDir)
            .asSequence()
            .mapNotNull { it.orNull?.asFile }
            .filter { it.exists() }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile }.toList() }
            .filterNot { it.invariantSeparatorsPath.contains("/main-release/app/") }
            .filter {
                it.extension.lowercase() in extensions || it.name.endsWith("-setup.exe", true)
            }
            .distinctBy { it.name }
            .sortedBy { it.name }
            .toList()

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

tasks.register("signReleaseArtifacts") {
    group = "distribution"
    description = "Signs every produced release artifact so the auto-updater can verify it"

    val binariesDir = layout.buildDirectory.dir("compose/binaries/main-release")
    val distributionsDir = layout.buildDirectory.dir("distributions")
    val installerDir = layout.buildDirectory.dir("installer")
    val trustedKeysFile = layout.projectDirectory.file("src/desktopMain/resources/update-signing-keys.txt")
    val version = appVersion

    doLast {
        val encodedKey = System.getenv("CHATONE_UPDATE_SIGNING_KEY").orEmpty().trim()
        if (encodedKey.isEmpty()) {
            throw GradleException("CHATONE_UPDATE_SIGNING_KEY is not set, refusing to publish unsigned updates")
        }
        val keyFactory = KeyFactory.getInstance("Ed25519")
        val decoder = Base64.getDecoder()
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(decoder.decode(encodedKey)))
        val trustedKeys = trustedKeysFile.asFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { keyFactory.generatePublic(X509EncodedKeySpec(decoder.decode(it))) }
        if (trustedKeys.isEmpty()) {
            throw GradleException("No trusted update keys in ${trustedKeysFile.asFile}")
        }

        val extensions = setOf("msi", "dmg", "deb", "zip")
        val artifacts = listOf(binariesDir, distributionsDir, installerDir)
            .asSequence()
            .mapNotNull { it.orNull?.asFile }
            .filter { it.exists() }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile }.toList() }
            .filterNot { it.invariantSeparatorsPath.contains("/main-release/app/") }
            .filter {
                it.extension.lowercase() in extensions || it.name.endsWith("-setup.exe", true)
            }
            .distinctBy { it.name }
            .sortedBy { it.name }
            .toList()
        if (artifacts.isEmpty()) {
            throw GradleException("No release artifacts found to sign")
        }

        val digest = MessageDigest.getInstance("SHA-256")
        artifacts.forEach { file ->
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
            val message = "chatone-update-v1\n$version\n${file.name}\n$hex\n".toByteArray(Charsets.UTF_8)
            val signature = Signature.getInstance("Ed25519").apply {
                initSign(privateKey)
                update(message)
            }.sign()
            val trusted = trustedKeys.any { key ->
                Signature.getInstance("Ed25519").apply {
                    initVerify(key)
                    update(message)
                }.verify(signature)
            }
            if (!trusted) {
                throw GradleException(
                    "CHATONE_UPDATE_SIGNING_KEY does not match any key in ${trustedKeysFile.asFile.name}"
                )
            }
            File(file.parentFile, "${file.name}.sig")
                .writeText(Base64.getEncoder().encodeToString(signature) + "\n")
            println("Signed ${file.name}")
        }
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
