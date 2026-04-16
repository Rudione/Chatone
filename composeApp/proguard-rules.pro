# ── Kotlin ──────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod

# ── Kotlinx Serialization ───────────────────────────────────
# Без этого @Serializable классы сломаются в рантайме
-keepattributes RuntimeVisibleAnnotations
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-dontwarn kotlinx.serialization.**

# ── Ktor ────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.websocket.** { *; }

# ── OkHttp / Okio ───────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Koin DI ─────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**
# Модули DI — нельзя удалять
-keep class io.rudione.chatone.di.** { *; }

# ── SQLDelight ──────────────────────────────────────────────
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**
-keep class io.rudione.chatone.data.local.** { *; }
# Сгенерированные запросы SQLDelight
-keep class io.rudione.chatone.data.local.*Queries { *; }
-keep class io.rudione.chatone.data.local.*Database { *; }

# ── Coil (загрузка изображений) ─────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**

# ── Napier (логгер) ─────────────────────────────────────────
-keep class io.github.aakira.napier.** { *; }
-dontwarn io.github.aakira.napier.**

# ── Compose / Jetbrains UI ──────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class org.jetbrains.compose.** { *; }
-dontwarn org.jetbrains.**
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ── Multiplatform Settings ──────────────────────────────────
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# ── SLF4J (используется десктопным логгером) ────────────────
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# ── Весь код приложения — не трогать ────────────────────────
# Если хочешь включить оптимизацию/обфускацию — убери эту строку
# и добавляй исключения точечно по ошибкам рантайма
-keep class io.rudione.chatone.** { *; }

# ── JVM internals / reflection ──────────────────────────────
-keepattributes SourceFile, LineNumberTable
#-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
-dontwarn javax.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── OkHttp platform adapters (не используются на десктопе) ──
# OkHttp содержит адаптеры для Android, BouncyCastle и Conscrypt.
# На десктопном JVM их нет — это нормально, они не нужны.
-dontwarn okhttp3.internal.platform.android.**
-dontwarn okhttp3.internal.platform.AndroidPlatform
-dontwarn okhttp3.internal.platform.Android10Platform
-dontwarn okhttp3.internal.platform.BouncyCastlePlatform
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn okhttp3.internal.platform.ConscryptPlatform$DisabledHostnameVerifier

# ── Android классы (OkHttp тащит их как опциональные зависимости) ──
-dontwarn android.**
-dontwarn com.android.**

# ── BouncyCastle (опциональный TLS провайдер, не используется) ──
-dontwarn org.bouncycastle.**

# ── Conscrypt (опциональный TLS провайдер, не используется) ──
-dontwarn org.conscrypt.**

# ── Ktor network internals ──────────────────────────────────
-dontwarn io.ktor.network.sockets.**
-dontwarn io.ktor.server.engine.**

# ── jansi / fusesource (цветной терминал, опционально) ──────
-dontwarn org.fusesource.**

# ── Typesafe config (Ktor server тащит его) ─────────────────
-dontwarn com.typesafe.**

# ── SLF4J динамическая загрузка ──────────────────────────────
-dontwarn org.slf4j.**

# Глобальный фолбэк — для любых других unresolved warning
# (не влияет на реальные ошибки, только на предупреждения о
# классах которые намеренно отсутствуют в рантайме)
-ignorewarnings