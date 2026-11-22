# kotlinx.serialization: сохранить сгенерированные сериализаторы после R8
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class ru.tac.whosayswhat.data.**$$serializer { *; }
-keepclassmembers class ru.tac.whosayswhat.data.** {
    *** Companion;
}
-keepclasseswithmembers class ru.tac.whosayswhat.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
