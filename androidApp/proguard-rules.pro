-dontobfuscate

# JNA resolves its Java API from native code and accesses Structure subclasses reflectively.
-keep class com.sun.jna.** { *; }
-keep class * extends com.sun.jna.Structure { *; }
-keep class * implements com.sun.jna.Callback { *; }

# Unused optional HtmlCompressor integrations
-dontwarn com.yahoo.platform.yui.compressor.CssCompressor
-dontwarn com.yahoo.platform.yui.compressor.JavaScriptCompressor
-dontwarn org.mozilla.javascript.ErrorReporter

# TODO these should not be in common code
-dontwarn java.awt.**
