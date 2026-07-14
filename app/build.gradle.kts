plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "dev.beltramitech.ofertazap"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.beltramitech.ofertazap"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.1"
    }

    buildTypes {
        debug {
            manifestPlaceholders["adMobApplicationId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            manifestPlaceholders["adMobApplicationId"] = "ca-app-pub-9920228067759661~4226417553"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    val fragmentVersion = "1.8.9"
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    constraints {
        implementation("androidx.fragment:fragment:$fragmentVersion") {
            because("Google Play flags older transitive Fragment SDK versions.")
        }
    }

    debugImplementation("androidx.compose.ui:ui-tooling")
}
