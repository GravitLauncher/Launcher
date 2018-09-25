-libraryjars '<java.home>/lib/rt.jar'
-libraryjars '<java.home>/lib/jce.jar'
-libraryjars '<java.home>/lib/ext/jfxrt.jar'
-libraryjars '<java.home>/lib/ext/nashorn.jar'

-printmapping '../build/mapping.pro'
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute Source

-dontnote
-dontwarn
-dontshrink
-dontoptimize
-ignorewarnings
-target 8
-forceprocessing

-obfuscationdictionary 'dictionary.pro'
-classobfuscationdictionary 'dictionary.pro'
-overloadaggressively
-repackageclasses 'launcher'
-keep class ru.zaxar163.*
-keepattributes SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile
-adaptresourcefilecontents META-INF/MANIFEST.MF

-keeppackagenames com.eclipsesource.json.**,com.com.mojang.**

-keep class com.eclipsesource.json.**,com.com.mojang.** {
    <fields>;
    <methods>;
}

-keepclassmembers @LauncherAPI class ** {
    <fields>;
    <methods>;
}

-keepclassmembers class ** {
    @LauncherAPI
    <fields>;
    @LauncherAPI
    <methods>;
}

-keepclassmembers public class ** {
    public static void main(java.lang.String[]);
}

-keepclassmembers enum ** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}