# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ----------------------------------------------------
# 🔍 1. Debugging & Stack Traces (Highly Recommended for Google Play)
# ----------------------------------------------------
# Preserve original line numbers and source file names in stack traces for crash reporting
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod,Annotation

# ----------------------------------------------------
# 📰 2. Jsoup HTML Parser Keep Rules
# ----------------------------------------------------
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ----------------------------------------------------
# 🤖 3. Google Generative AI SDK (Gemini) Keep Rules
# ----------------------------------------------------
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ----------------------------------------------------
# 🔄 4. Kotlin Coroutines & Serialization Fallbacks
# ----------------------------------------------------
-keepattributes *Annotation*,ElementPrecision
-dontwarn kotlinx.coroutines.**