# R8 for release builds: shrinking, and optimizing what isn't kept.
#
# Nothing is renamed: much of stream resolution is reached by reflection or
# by name (Rhino running YouTube's player JavaScript, NewPipe, InnerTubeX's
# Ktor client, protobuf, QuickJS's JNI callbacks, the PoToken WebView's
# JavaScript interface), and readable class names keep the stack traces in
# "Copy log" useful. Those packages and the app itself are kept whole below,
# which also leaves their code as written. Everything else (Compose, AndroidX,
# OkHttp, coroutines) is optimized, as BitChord does: Compose run unoptimized
# is markedly slower, the stutter on the first scrolls and the opening.
# Shrinking removes library code nothing calls, the bulk of it unused
# Material icons, which is where the size goes.
-dontobfuscate
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,Exceptions

# Keep core data models, serialization, playback services and reflection.
# Because -dontobfuscate is set above, no classes or methods are renamed.
# Allowing R8 to optimize com.aura.ui enables Compose compiler inlining and dead code pruning.
-keep class com.aura.data.** { *; }
-keep class com.aura.playback.** { *; }
-keep class com.aura.cast.** { *; }
-keep class com.aura.MainActivity { *; }
-keep class com.aura.AuraApp { *; }

# Stream resolution.
-keep class org.schabi.newpipe.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class com.metrolist.innertubex.** { *; }
-keep class io.ktor.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class com.dokar.quickjs.** { *; }
-keep class org.jsoup.** { *; }

# The PoToken WebView calls back into Kotlin by method name.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Optional pieces of these libraries that Android doesn't have.
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn javax.lang.model.**
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.re2j.**
-dontwarn jdk.dynalink.**
-dontwarn java.lang.management.**

# Strip verbose/debug Log calls in release builds while keeping i/w/e for diagnostics.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
