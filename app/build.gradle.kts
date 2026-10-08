@file:Suppress("SpellCheckingInspection", "NewerVersionAvailable", "AndroidGradlePluginVersion")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)

}

android {
    namespace = "com.example.mymagic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sproduction.mymagic"
        minSdk = 29
        targetSdk = 35
        versionCode = 37
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        noCompress.addAll(listOf("txt", "mdl", "conf", "fst", "dubm", "ie", "mat", "stats", "gmm", "tree", "mp4", "mp3"))
    }
    // ЖЕСТКАЯ ФИКСАЦИЯ КЛЮЧА ПОДПИСИ
    signingConfigs {
        create("release") {
            // Ключ лежит прямо в папке app, путь железно надежен
            storeFile = file("mymagickey.jks")
            storePassword = "Ramplstilskin" // Писать строго в кавычках
            keyAlias = "app_alias" // Твой алиас, всё чётко
            keyPassword = "Ramplstilskin" // Писать строго в кавычках
        }
    }

    buildTypes {
        release {
            // Привязываем зафиксированный ключ намертво к релизному билду
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


        compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            pickFirsts.add("lib/**/libjnidispatch.so")
        }
        jniLibs {
            useLegacyPackaging = false

        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


tasks.withType(com.android.build.gradle.internal.tasks.CheckAarMetadataTask::class.java).configureEach {
    enabled = false
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("com.startapp:inapp-sdk:5.1.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("com.samsung.developer:iap:6.5.2")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation("com.alphacephei:vosk-android:0.3.70") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    // Наш сетевой движок Ktor
    implementation("io.ktor:ktor-client-android:2.3.11")
// Плагин для работы с JSON/сериализацией
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")

}
