# ====================================================================
# АТОМНАЯ ЗАЩИТА JNA И ИГНОРИРОВАНИЕ ВАРНИНГА
# ====================================================================
# Силой удерживаем абсолютно весь пакет JNA (все классы, поля и методы)
-keep class com.sun.jna.** { *; }

# Принудительно глушим варнинг "Overly broad keep rule" для JNA, чтобы билд не стопорился
-dontwarn com.sun.jna.**

# Защита основных классов Vosk
-keep class org.vosk.Model
-keep class org.vosk.Recognizer
-keep class org.vosk.android.SpeechService
-dontwarn org.vosk.**

# Запрещаем переименовывать нативные методы во всем проекте
-keepclasseswithmembernames class * {
    native <methods>;
}

# Отключаем предупреждения для AdMob
-dontwarn com.google.android.gms.**

# Сохраняем метаданные для Jetpack Compose
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Удерживаем оптимизатор R8 от паники из-за отсутствия логгера SLF4J в Ktor
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Защищаем сетевой движок Ktor от сжатия и обфускации
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ====================================================================
# ЖЕЛЕЗНАЯ ЗАЩИТА SAMSUNG IAP SDK (БЕЗ ДУБЛИКАТОВ)
# ====================================================================
# Удерживаем абсолютно все классы, интерфейсы, поля и методы Самсунга во всех вариациях пакетов
-keep class com.samsung.android.sdk.iap.** { *; }
-keep class com.samsung.android.iap.** { *; }
-dontwarn com.samsung.android.sdk.iap.**
-dontwarn com.samsung.android.iap.**
