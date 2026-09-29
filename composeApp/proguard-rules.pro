-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod

-keepattributes RuntimeVisibleAnnotations
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-dontwarn kotlinx.serialization.**

-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.websocket.** { *; }

-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keep class io.rudione.chatone.di.** { *; }

-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**
-keep class io.rudione.chatone.data.local.** { *; }
-keep class io.rudione.chatone.data.local.*Queries { *; }
-keep class io.rudione.chatone.data.local.*Database { *; }

-keep class coil3.** { *; }
-dontwarn coil3.**

-keep class io.github.aakira.napier.** { *; }
-dontwarn io.github.aakira.napier.**

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class org.jetbrains.compose.** { *; }
-dontwarn org.jetbrains.**
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

-keep class io.rudione.chatone.** { *; }

-keepattributes SourceFile, LineNumberTable
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
-dontwarn javax.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-dontwarn okhttp3.internal.platform.android.**
-dontwarn okhttp3.internal.platform.AndroidPlatform
-dontwarn okhttp3.internal.platform.Android10Platform
-dontwarn okhttp3.internal.platform.BouncyCastlePlatform
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn okhttp3.internal.platform.ConscryptPlatform$DisabledHostnameVerifier

-dontwarn android.**
-dontwarn com.android.**

-dontwarn org.bouncycastle.**

-dontwarn org.conscrypt.**

-dontwarn io.ktor.network.sockets.**
-dontwarn io.ktor.server.engine.**

-dontwarn org.fusesource.**

-dontwarn com.typesafe.**

-dontwarn org.slf4j.**

-ignorewarnings