-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault

-keepnames class * extends java.lang.Throwable

-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }

-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**
-dontwarn javax.naming.**
