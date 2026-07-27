# ---------------------------------------------------------------------------
# libopenconnect JNI bridge
#
# jni.c resolves Java members by *name and signature* at runtime:
#   - fields on LibOpenConnect (libctx) and on the AuthForm / FormOpt /
#     FormChoice / IPInfo / VPNStats value classes,
#   - the callback methods (onProgress, onProcessAuthForm, onProtectSocket, …)
#     on whatever concrete subclass is instantiated.
# R8 has no way to see any of that, so all of it has to survive shrinking.
# ---------------------------------------------------------------------------
-keep class org.infradead.libopenconnect.** { *; }
-keep class * extends org.infradead.libopenconnect.LibOpenConnect { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin coroutines / serialization housekeeping
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose keeps its own rules via consumer files; nothing extra needed here.
