import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)

    //alias(libs.plugins.kotlin.parcelize)
    //alias(libs.plugins.androidx.navigation.safeargs.kotlin-gradle-plugin)
    //id("androidx.navigation.safeargs.kotlin") version "2.8.9" // Use the correct version
}

android {
    namespace = "com.djangofiles.djangofiles"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.djangofiles.djangofiles"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["firebaseAnalyticsDeactivated"] = false // enabled
        manifestPlaceholders["firebaseCrashlyticsEnabled"] = true // enabled
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["firebaseAnalyticsDeactivated"] = true // disabled
            manifestPlaceholders["firebaseCrashlyticsEnabled"] = false // disabled

            // Debugging Only
            val localProperties = Properties().apply {
                val localPropertiesFile = rootProject.file("local.properties")
                if (localPropertiesFile.exists()) {
                    localPropertiesFile.inputStream().use { load(it) }
                }
            }
            val discordWebhook = localProperties.getProperty("DISCORD_WEBHOOK", "")
            buildConfigField("String", "DISCORD_WEBHOOK", "\"$discordWebhook\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        //dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    //implementation(libs.firebase.messaging)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.converter.moshi)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    implementation(libs.glide)
    implementation(libs.okhttp3.integration)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)
    //noinspection KaptUsageInsteadOfKsp
    //kapt(libs.glide.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
